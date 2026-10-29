package com.Johnny.wcx.features.items.moments

import android.app.Activity
import android.content.ContentValues
import android.view.View
import android.view.ViewGroup
import com.tencent.mm.plugin.sns.storage.ADInfo
import com.tencent.mm.plugin.sns.ui.SnsUserUI
import com.tencent.mm.plugin.sns.ui.improve.ImproveSnsTimelineUI
import com.tencent.mm.view.recyclerview.WxRecyclerView
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.toClassOrNull
import com.Johnny.wcx.features.api.core.WeDatabaseListenerApi
import com.Johnny.wcx.features.api.ui.WeMomentsApi
import com.Johnny.wcx.features.core.Feature
import com.Johnny.wcx.features.core.SwitchFeature
import com.Johnny.wcx.ui.utils.findViewWhich
import com.Johnny.wcx.ui.utils.rootView
import com.Johnny.wcx.utils.WeLogger
import com.Johnny.wcx.utils.reflection.ClassLoaders
import java.util.Collections
import java.util.WeakHashMap

@Feature(name = "拦截朋友圈广告", categories = ["朋友圈"], description = "拦截朋友圈广告")
object RemoveMomentsAds : SwitchFeature(),
    WeDatabaseListenerApi.IInsertListener,
    WeDatabaseListenerApi.IInsertFilter {

    private const val TAG = "RemoveMomentsAds"
    private const val TBL_SNS_INFO = "SnsInfo"

    // 已挂载的根视图集合，避免重复挂载
    private val attachedRoots: MutableSet<ViewGroup> = Collections.newSetFromMap(WeakHashMap())

    override fun onEnable() {
        WeLogger.i(TAG, "========== 拦截朋友圈广告: 已开启 ==========")

        // ── 第①层：数据源层拦截（阻断广告数据创建与入库） ──────────────────
        // Hook 1: ADInfo 构造函数 — 阻断广告数据对象创建
        try {
            ADInfo::class.reflekt()
                .firstConstructor { parameters(String::class) }
                .hookBefore {
                    try {
                        WeLogger.i(TAG, "数据源层拦截: 阻断 ADInfo 构造")
                        if (method is java.lang.reflect.Method) {
                            val returnType = (method as java.lang.reflect.Method).returnType
                            if (returnType == Void.TYPE) {
                                result = null
                            }
                        }
                    } catch (e: Throwable) {
                        WeLogger.e(TAG, "ADInfo 构造 Hook 异常", e)
                    }
                }
            WeLogger.d(TAG, "Hook ① ADInfo 构造 注册成功")
        } catch (e: Throwable) {
            WeLogger.e(TAG, "Hook ① ADInfo 构造 注册失败", e)
        }

        // Hook 2: SnsInfo 数据库插入过滤器与监听 — 彻底阻断广告入库
        try {
            WeDatabaseListenerApi.addListener(this)
            WeLogger.d(TAG, "Hook ② SnsInfo 数据库监听与过滤器 注册成功")
        } catch (e: Throwable) {
            WeLogger.e(TAG, "Hook ② SnsInfo 数据库监听与过滤器 注册失败", e)
        }

        // ── 第②层：View 渲染层兜底过滤 ────────────────────────────────────
        // Hook 3: 朋友圈时间线页面生命周期 — 挂载 RecyclerView/ListView 监听
        // 同时支持新版 ImproveSnsTimelineUI、旧版/传统 SnsTimeLineUI 以及用户主页 SnsUserUI
        try {
            val timelineClassNames = listOf(
                "com.tencent.mm.plugin.sns.ui.improve.ImproveSnsTimelineUI",
                "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI",
                "com.tencent.mm.plugin.sns.ui.SnsUserUI"
            )
            for (className in timelineClassNames) {
                val clazz = runCatching { ClassLoaders.HOST.loadClass(className) }.getOrNull()
                    ?: className.toClassOrNull()
                if (clazz == null) {
                    WeLogger.d(TAG, "页面类未找到，跳过: $className")
                    continue
                }

                val onCreateMethod = clazz.reflekt().firstMethodOrNull { name = "onCreate" }
                    ?: clazz.reflekt().firstMethodOrNull { name = "onCreate"; superclass() }
                onCreateMethod?.hookAfter {
                    val activity = thisObject as? Activity ?: return@hookAfter
                    if (clazz.isInstance(activity)) {
                        scheduleViewLayerAttach(activity)
                    }
                }

                val onResumeMethod = clazz.reflekt().firstMethodOrNull { name = "onResume" }
                    ?: clazz.reflekt().firstMethodOrNull { name = "onResume"; superclass() }
                onResumeMethod?.hookAfter {
                    val activity = thisObject as? Activity ?: return@hookAfter
                    if (clazz.isInstance(activity)) {
                        scheduleViewLayerAttach(activity)
                    }
                }
                WeLogger.d(TAG, "Hook ③ 生命周期挂载成功: ${clazz.name}")
            }
        } catch (e: Throwable) {
            WeLogger.e(TAG, "Hook ③ 时间线页面生命周期 注册失败", e)
        }

        WeLogger.i(TAG, "========== 拦截朋友圈广告: 全部 Hook 注册完成 ==========")
    }

    override fun onDisable() {
        WeLogger.i(TAG, "拦截朋友圈广告: 已关闭，恢复原生广告展示")
        try {
            WeDatabaseListenerApi.removeListener(this)
        } catch (e: Throwable) { WeLogger.w(TAG, "hook 回调异常已忽略: " + e.message) }
        attachedRoots.clear()
    }

    // =========================================================================
    // 数据源层：SnsInfo 数据库插入过滤器 (IInsertFilter)
    // =========================================================================
    override fun allowInsert(table: String, values: ContentValues): Boolean {
        try {
            // 广告专属表直接拦截
            if (table.equals("AdSnsInfo", ignoreCase = true) ||
                table.equals("AdSnsInfoStorage", ignoreCase = true) ||
                table.startsWith("AdSns", ignoreCase = true)
            ) {
                WeLogger.i(TAG, "数据源拦截: 阻止广告专属表入库, table=$table")
                return false
            }

            // 朋友圈主表: 校验广告特征字段
            if (table.equals(TBL_SNS_INFO, ignoreCase = true)) {
                // 1. adxml 字段（WCDB 核心广告标识）
                val adxml = values.getAsString("adxml") ?: values.getAsString("field_adxml")
                if (!adxml.isNullOrBlank()) {
                    val snsId = values.getAsLong("snsId") ?: values.getAsLong("field_snsId") ?: 0L
                    WeLogger.i(TAG, "数据源拦截: 阻止含 adxml 广告入库, table=$table, snsId=$snsId")
                    return false
                }

                // 2. adInfo 字段
                val adInfo = values.getAsString("adInfo") ?: values.getAsString("field_adInfo")
                if (!adInfo.isNullOrBlank()) {
                    val snsId = values.getAsLong("snsId") ?: values.getAsLong("field_snsId") ?: 0L
                    WeLogger.i(TAG, "数据源拦截: 阻止含 adInfo 广告入库, table=$table, snsId=$snsId")
                    return false
                }

                // 3. atAdInfo 字段
                val atAdInfo = values.getAsString("atAdInfo") ?: values.getAsString("field_atAdInfo")
                if (!atAdInfo.isNullOrBlank()) {
                    val snsId = values.getAsLong("snsId") ?: values.getAsLong("field_snsId") ?: 0L
                    WeLogger.i(TAG, "数据源拦截: 阻止含 atAdInfo 广告入库, table=$table, snsId=$snsId")
                    return false
                }

                // 4. sourceType 标志位检查（WeChat 广告 sourceType 包含 0x20 / 32）
                val sourceType = values.getAsInteger("sourceType") ?: values.getAsInteger("field_sourceType")
                if (sourceType != null && (sourceType and 0x20) != 0) {
                    val snsId = values.getAsLong("snsId") ?: values.getAsLong("field_snsId") ?: 0L
                    WeLogger.i(TAG, "数据源拦截: 阻止含 AD sourceType($sourceType) 广告入库, table=$table, snsId=$snsId")
                    return false
                }
            }
        } catch (e: Throwable) {
            WeLogger.e(TAG, "allowInsert 广告过滤异常", e)
        }
        return true
    }

    // =========================================================================
    // 数据源层：SnsInfo 数据库插入监听 (IInsertListener)
    // =========================================================================
    override fun onInsert(table: String, values: ContentValues) {
        try {
            if (!table.equals(TBL_SNS_INFO, ignoreCase = true)) return
            val snsId = values.getAsLong("snsId") ?: values.getAsLong("field_snsId") ?: return
            if (snsId == 0L) return

            // 通过 SnsId 获取 SnsInfo 对象，二次确认是否为广告
            val snsInfo = WeMomentsApi.getSnsInfoBySnsId(snsId) ?: return
            if (WeMomentsApi.isAd(snsInfo)) {
                WeLogger.w(TAG, "数据源层: 警告，广告 snsId=$snsId 仍被插入数据库")
            }
        } catch (e: Throwable) {
            WeLogger.e(TAG, "数据源层 SnsInfo 监听异常", e)
        }
    }

    // =========================================================================
    // View 渲染层兜底过滤
    // =========================================================================
    private fun scheduleViewLayerAttach(activity: Activity) {
        val root = activity.rootView
        intArrayOf(0, 200, 800, 2_000).forEach { delayMs ->
            root.postDelayed({
                runCatching { attachToTimelineList(root) }
                    .onFailure { WeLogger.w(TAG, "View层挂载失败, delay=${delayMs}ms", it) }
            }, delayMs.toLong())
        }
    }

    private fun attachToTimelineList(root: ViewGroup) {
        // 兼容 WxRecyclerView、RecyclerView 以及传统 ListView
        val list = root.findViewWhich<ViewGroup> {
            it is WxRecyclerView ||
            it is android.widget.ListView ||
            it.javaClass.name.endsWith("RecyclerView") ||
            it.javaClass.name.endsWith("ListView")
        } ?: return

        synchronized(attachedRoots) {
            if (!attachedRoots.add(root)) return
        }
        WeLogger.d(TAG, "View层: 挂载时间线列表监听 (${list.javaClass.name})")

        // 监听布局与滚动变化，过滤可见广告项
        list.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            filterVisibleAds(list)
        }
        list.viewTreeObserver.addOnGlobalLayoutListener {
            filterVisibleAds(list)
        }
        list.viewTreeObserver.addOnScrollChangedListener {
            filterVisibleAds(list)
        }
        // 首次立即过滤
        list.post { filterVisibleAds(list) }
    }

    /**
     * 遍历当前可见的列表项，识别广告 View 并彻底隐藏。
     * 作为数据源层拦截的兜底防护。
     */
    private fun filterVisibleAds(list: ViewGroup) {
        try {
            var blockedCount = 0
            var checkedCount = 0
            for (i in 0 until list.childCount) {
                val itemView = list.getChildAt(i) ?: continue
                checkedCount++
                try {
                    val snsInfo = locateSnsInfo(itemView)
                    if (snsInfo != null && WeMomentsApi.isAd(snsInfo)) {
                        hideAdItemView(itemView, snsInfo, i)
                        blockedCount++
                    }
                } catch (e: Throwable) {
                    WeLogger.d(TAG, "View层: 检查 item[$i] 异常，跳过: ${e.message}")
                }
            }
            if (checkedCount > 0 && blockedCount > 0) {
                WeLogger.d(TAG, "View层: 检查 $checkedCount 个可见项, 拦截隐藏 $blockedCount 个广告")
            }
        } catch (e: Throwable) {
            WeLogger.e(TAG, "View层过滤异常", e)
        }
    }

    /**
     * 彻底隐藏广告 View：设为 GONE、宽高置零、清除边距，确保不占用屏幕空间与点击事件
     */
    private fun hideAdItemView(itemView: View, snsInfo: Any?, index: Int) {
        if (itemView.visibility != View.GONE) {
            itemView.visibility = View.GONE
        }
        val lp = itemView.layoutParams
        if (lp != null && (lp.height != 0 || lp.width != 0)) {
            lp.height = 0
            lp.width = 0
            itemView.layoutParams = lp
        }
        itemView.setPadding(0, 0, 0, 0)
        WeLogger.i(TAG, "View层兜底: 彻底隐藏广告 View, index=$index, snsId=${snsInfo?.let { getSnsId(it) } ?: 0L}")
    }

    /**
     * 从 itemView 中定位 SnsInfo 对象。
     * 支持 Improve 模式与传统 ViewHolder / tag 模式。
     */
    private fun locateSnsInfo(itemView: View): Any? {
        try {
            // 方式1: 直接从 itemView 提取 ImproveSnsInfo
            val improveSnsInfo = extractImproveSnsInfo(itemView)
            if (improveSnsInfo != null) return improveSnsInfo

            // 方式2: 通过 InteractionLayout 定位
            val interactionView = itemView.findViewWhich<View> {
                WeMomentsApi.classImproveInteractionLayout.clazz.isInstance(it)
            }
            if (interactionView != null) {
                val fromInteraction = extractImproveSnsInfo(interactionView)
                    ?: WeMomentsApi.fieldInteractionSnsInfo.field.get(interactionView)
                if (fromInteraction != null) return fromInteraction
            }

            // 方式3: 检查 itemView.tag (传统 SnsTimeLineUI / ViewHolder 场景)
            val tag = itemView.tag
            if (tag != null) {
                if (WeMomentsApi.isAd(tag)) return tag
                // tag 可能是 ViewHolder，持有 snsInfo 字段
                val holderSnsInfo = tag.reflekt().firstFieldOrNull {
                    superclass()
                    name = "snsInfo"
                }?.get() ?: tag.reflekt().firstFieldOrNull {
                    superclass()
                    type { it.name.endsWith(".SnsInfo") }
                }?.get() ?: tag.reflekt().firstMethodOrNull {
                    parameters()
                    superclass()
                    name = "getSnsInfo"
                }?.invoke()
                if (holderSnsInfo != null) return holderSnsInfo
            }

            return null
        } catch (e: Throwable) {
            return null
        }
    }

    private fun extractImproveSnsInfo(receiver: Any): Any? {
        try {
            if (WeMomentsApi.classImproveSnsInfo.clazz.isInstance(receiver)) return receiver

            receiver.reflekt()
                .firstMethodOrNull {
                    parameters()
                    superclass()
                    returnType { WeMomentsApi.classImproveSnsInfo.clazz.isAssignableFrom(it) }
                }?.invoke()?.let { return it }

            receiver.reflekt().firstMethodOrNull {
                name = "getImproveListItem"
                parameters()
                superclass()
            }?.invoke()?.let { listItem ->
                listItem.reflekt()
                    .firstMethodOrNull {
                        parameters()
                        superclass()
                        returnType { WeMomentsApi.classImproveSnsInfo.clazz.isAssignableFrom(it) }
                    }?.invoke()?.let { return it }
                listItem.reflekt()
                    .firstFieldOrNull {
                        superclass()
                        type { WeMomentsApi.classImproveSnsInfo.clazz.isAssignableFrom(it) }
                    }?.get()?.let { return it }
            }

            return receiver.reflekt()
                .firstFieldOrNull {
                    superclass()
                    type { WeMomentsApi.classImproveSnsInfo.clazz.isAssignableFrom(it) }
                }?.get()
        } catch (e: Throwable) {
            return null
        }
    }

    private fun getSnsId(snsInfo: Any): Long {
        return try {
            (snsInfo.reflekt().firstFieldOrNull { name = "field_snsId"; superclass() }?.get() as? Number)?.toLong() ?: 0L
        } catch (_: Throwable) { 0L }
    }
}
