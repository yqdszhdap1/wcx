# WCX · 微信功能增强与逆向分析工具模块 💠

[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](#-适配版本)
[![Framework](https://img.shields.io/badge/Framework-LSPosed%20%7C%20Xposed-orange.svg)](#-适配版本)
[![Target](https://img.shields.io/badge/Target-WeChat%208.0.77~8.0.79-brightgreen.svg)](#-适配版本)
[![Language](https://img.shields.io/badge/Language-Kotlin%20%7C%20Rust-purple.svg)](#-项目结构)
[![GitHub Repo](https://img.shields.io/badge/GitHub-yqdszhdap1%2Fwcx-181717?logo=github)](https://github.com/yqdszhdap1/wcx)

一款基于 Xposed / LSPosed 框架的开源微信功能增强与逆向安全分析模块。项目代码内置 **200+ 个可独立配置的功能开关，涵盖 20 余个模块分类**，覆盖聊天增强、界面美化、隐私防护、朋友圈扩展、自动化脚本及去混淆分析等。

> 🚀 **项目定位**：本项目不仅是一个日常交互体验增强模块，更深度集成了**底层去混淆分析工具套件 (deobf)** —— 覆盖从 Android 逆向分析、动态脱壳解密到功能实现的完整闭环。

---

## ⚠️ 免责声明与合规规则 (Disclaimer & Compliance)

本项目严格遵循 [GitHub 社区准则 (Community Guidelines)](https://docs.github.com/en/site-policy/github-community-guidelines) 与 [开源许可证 (GPL-3.0)](LICENSE)：

1. **学术与研究用途**：本项目仅供 Android 框架学习、底层 Hook 机制研究、逆向安全攻防交流及个人无害化交互体验测试使用，代码完全开源透明。
2. **禁止非法用途**：严禁将本项目用于任何形式的商业营利、网络黑灰产、灰产营销、恶意骚扰、侵害他人合法权益或破坏网络安全秩序的行为。
3. **风险自负原则**：微信及其相关知识产权归腾讯公司所有。使用 Xposed 框架注入可能触发应用风控机制，由此导致的任何账号受限、数据丢失或合规风险，使用者须自行承担，本项目及贡献者不承担任何直接或间接法律责任。
4. **开源协议合规**：本项目属于开源衍生作品，遵循 GNU General Public License v3.0，所有衍生修改均已公开源码，保留原作者及上游版权声明。

---

## 📌 上游与衍生谱系 (Lineage & Acknowledgements)

- **核心上游**：衍生自 [WeKit](https://github.com/Ujhhgtg/WeKit) 与 [Johnny520/wcx](https://github.com/Johnny520/wcx)；
- **当前仓库维护**：[yqdszhdap1/wcx](https://github.com/yqdszhdap1/wcx)；
- **历史说明**：从 **v332** 起，早期版本中曾包含的第三方冗余实现已整包彻底移除，全部功能基于自主锚点与独立 Hook 体系重构，不包含任何侵权或闭源二进制残留。

---

## ✨ 功能总览

| 分类 | 数量 | 代表功能 |
|------|-----:|----------|
| 💬 聊天增强 | 84 | 防撤回、AI 自动回复、对话归拢、消息过滤屏蔽、悬浮标题栏 / 输入框 |
| 🎨 界面美化 | 17 | 主题商城、四 Tab 主题背景、莫奈引擎、主页侧边栏、自定义气泡 |
| 👥 联系人与群组 | 21 | 隐藏联系人、单向删好友检测、批量删好友、群成员实名 / 身份 |
| 📸 朋友圈 | 21 | 朋友圈防撤回、自动点赞、拦截广告、一键转发、伪集赞 |
| 💰 红包与支付 | 9 | 自动抢红包、金额统计、捡漏历史红包、指纹支付、自动收转账 |
| 📦 批量操作 | 8 | 批量删好友 / 删记录 / 打标签 / 免打扰 / 隐藏对话、群发、导出记录 |
| 🔧 系统与隐私 | 28 | 反 Xposed 检测、Native 反检测、环境伪装、虚拟定位、强制平板 |
| 📺 小程序 / 公众号 | 12 | 去开屏 / 视频 / 嵌入广告、跳过启动页、网页多开、Eruda 调试 |
| 🎬 视频号 | 4 | 下载媒体、移除评论区广告、崩溃防御 |
| 🧩 自动化与脚本 | 15 | 脚本 Tab、在线插件市场、JS / Java 双引擎、MCP & REST API 服务器 |
| 🎭 娱乐 / 彩蛋 / 通话 / 调试 | 20 | 彩虹文本、假群组、画中画通话、崩溃拦截、内存分析 |

> 每个功能均具备独立开关与独立配置项，在「WCX 设置 → 功能」里按分类浏览配置。

### 💬 聊天增强
- **防撤回**：支持新版「原消息半透明 + 徽标」与旧版「插入系统提示」两种样式；
- **智能回复**：支持 AI 自动回复、关键词规则匹配及长按快捷回复；
- **对话归拢**：主页 Tab 分类收敛，群聊/会话智能整理文件夹；
- **消息过滤**：按规则过滤私聊、群聊与公众号消息，支持本地拦截日志回看；
- **媒体与文件**：图片、语音、文件及表情一键无损保存，半屏相册选择器。

### 🎨 界面美化
- **主题引擎**：支持动态主题商城、四 Tab 独立背景、全局半透明毛玻璃特效；
- **莫奈动态取色**：原生对接 Android 12+ Monet 壁纸颜色引擎；
- **侧边栏与布局**：主页侧边面板、底部导航栏微调及自定义气泡样式。

### 👥 联系人与群组
- **隐藏联系人**：全链路隐藏会话、通讯录、朋友圈、音视频及消息角标；
- **关系检测**：单向好友检测、自动备注、群成员实名与进群邀请溯源。

### 🔧 系统与隐私安全
- **深度反检测**：屏蔽微信对 Xposed / LSPosed 框架探测，Native 层拦截 `/proc/self/maps` 内存库遍历；
- **环境伪装**：伪装开发者选项、ADB 调试状态与网络环境；
- **实用工具**：虚拟定位辅助、平板双列模式强制适配、省电优化。

### 🔬 核心去混淆套件 (deobf)
- 基于 **unidbg ARM64** 模拟器的动态脱壳与解密流水线；
- 自动化捕获加密载荷并导出解密 DEX；
- 配套 Python 符号清洗、字符串还原与结构提取工具集。

---

## 📋 适配版本与运行环境

> 💡 **推荐版本**：推荐运行于 **微信\wechta play 8.0.77 ~ 8.0.79**

| 状态 | 版本区间 | 详细说明 |
|:---:|:---:|:---|
| ✅ **推荐使用** | 8.0.77 ~ 8.0.79 | 经过完整实机测试，Hook 点全功能适配 |
| 🔧 **维护中** | 8.0.69 ~ 8.0.76 | 绝大部分核心能力正常运行 |
| ⚠️ **旧版本** | < 8.0.69 | 部分新版 API 缺少对应锚点，不建议使用 |

- **支持框架**：LSPosed（推荐）、EdXposed、KernelSU / APatch 模块化加载
- **系统要求**：Android 8.0 及以上（推荐 Android 10 ~ 14）
- **架构支持**：`arm64-v8a`（主力）、`armeabi-v7a`

---

## 🏗️ 项目架构

```text
wcx/
├── app/                # 📱 Xposed 模块主程序（Kotlin）
│   ├── src/main/       # 主源码与业务实现
│   ├── src/standard/   # 标准构建变体（libxposed 入口）
│   └── embedded/       # 原生加载与 Hook 动态库
├── deobf/              # 🔬 微信模块去混淆与动态脱壳分析套件 (unidbg ARM64)
│   ├── unidbg-harness/ # 模拟器运行载荷
│   ├── moduledata/     # 加密载荷分析目录
│   └── tools/          # Python 逆向与符号清洗工具
├── xtask/              # 🦀 Rust 构建辅助与打包流水线
├── docs/               # 📖 完整技术文档、API 参考与设计说明
└── build.gradle.kts    # Gradle 构建配置
```

---

## 🔧 源码构建

```bash
# 1. 克隆代码仓库
git clone https://github.com/yqdszhdap1/wcx.git
cd wcx

# 2. 构建标准 Release 版本
./gradlew assembleRelease

# 3. 运行去混淆分析测试 (需要 Java 17+ 环境)
cd deobf && ./run_deobf.sh
```

---

## 📄 开源许可证与协议规范

本项目核心代码基于 **[GNU General Public License v3.0](LICENSE)** (GPL-3.0) 许可发布。
- `deobf/` 逆向分析工具基于 GPL-3.0 许可；
- 模块各子工程与第三方依赖遵循各自原始开源许可证协议；
- 任何二次修改与分发必须遵循 GPL-3.0 条款，保持源码开源并注明原作者署名。

---

## 🌐 仓库与反馈渠道

- **代码仓库**：[https://github.com/yqdszhdap1/wcx](https://github.com/yqdszhdap1/wcx)
- **问题反馈**：[Issues 提交入口](https://github.com/yqdszhdap1/wcx/issues)
- **项目文档**：[docs/](docs/)
