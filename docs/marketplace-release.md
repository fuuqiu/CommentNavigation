# MIT 开源版 Marketplace 发布

## 本地准备结果

- 产品：Code Comment Navigator。
- 新 Marketplace 条目使用插件 ID `cn.tinyue.commentnavigation`，不再关联已封禁的旧条目 27390。
- 版本：1.1.0；兼容 IDEA 2026.2（262.*）。
- 许可：MIT，唯一正文为根目录 `LICENSE.txt`；`LICENSE.md` 为阅读指引。
- 允许免费使用、修改和再分发，包括商业用途，须保留版权及许可声明。
- 安装包内 `META-INF/LICENSE.txt` 包含相同许可正文。
- 描述已清理购买、退款、授权到期和固定支持时限等旧商业条款。
- 联系邮箱统一为 fuuqiu@gmail.com；官网为 https://commentnavigation.plugins.topxup.com，源码指向当前 GitHub 仓库。

上述修改准备在本地，不能据此认定 GitHub 或 Marketplace 已经更新或解封。

## 发布顺序

### 1. 发布并核对 GitHub 内容

将本次代码、README 和 MIT 许可发布到 `fuuqiu/CommentNavigation` 的 `main` 分支后，检查以下地址显示的是新版内容：

- 源码：https://github.com/fuuqiu/CommentNavigation
- 许可：https://github.com/fuuqiu/CommentNavigation/blob/main/LICENSE.txt
- 说明：https://github.com/fuuqiu/CommentNavigation/blob/main/README.md
- 反馈：https://github.com/fuuqiu/CommentNavigation/issues

特别核对 LICENSE.txt 显示 MIT License，而非旧 Commercial License Agreement。公开源码仓库仍包含旧版许可时，不能把本地修改当作公开开源已经完成。

### 2. 新建 Marketplace 条目

在 https://plugins.jetbrains.com/plugin/add 上传新版安装包，并创建独立的免费插件条目。旧条目 27390 及其插件 ID `cn.tinyue.console` 保持不变；新版不得再使用该 ID，否则 Marketplace 会把安装包识别为旧插件或拒绝重复提交。

### 3. 同步 Marketplace 元数据

按支持团队给出的处理方式，核对并更新以下字段：

| 内容 | 目标值 |
| --- | --- |
| 插件名称 | Code Comment Navigator |
| XML ID | cn.tinyue.commentnavigation |
| 分发类型 | Free；若当前为 Paid 或 Externally Paid，请支持团队确认转换流程 |
| 源码链接 | https://github.com/fuuqiu/CommentNavigation |
| License | MIT；链接到当前仓库的 LICENSE.txt |
| 联系邮箱 | fuuqiu@gmail.com |
| 问题反馈 | https://github.com/fuuqiu/CommentNavigation/issues |
| 描述 | 使用新版 src/main/resources/META-INF/plugin.xml 中的 description |
| 兼容范围 | 262.*，与实际包一致 |

上传 ZIP 后仍需检查页面最终显示内容，确保没有沿用旧条目的商业描述和许可链接。

免费不等于自动成为 Non-trader；保留基于实际活动作出的经营者身份声明，不仅因为改为免费而切换身份。

### 4. 提交安装包并申请复核

安装包：`build/distributions/comment-navigation-1.1.0.zip`。

已执行的验证为单元测试、编译、打包及 `verifyPluginStructure`。后者不是 Plugin Verifier 二进制兼容检查，也不代表人工审核通过。正式提交前还应完成目标 IDE 中的点击跳转、编辑刷新及切换文件实测，并按官方要求执行 Plugin Verifier。

## 官方依据

- [审核指南](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html)
- [页面信息与描述来源](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)
- [Marketplace 协议：开源许可证](https://www.jetbrains.com/legal/docs/plugins_site/plugin_marketplace/)
- [封禁通知及异议处理](https://www.jetbrains.com/legal/docs/terms/marketplace-content-moderation/)

## 2026-09-16 页面完善

当前操作条目为 [34287 — Code Comment Navigator](https://plugins.jetbrains.com/plugin/34287-code-comment-navigator/edit)，页面仍显示审核中。页面保存不代表审核通过。

- Documentation URL： https://commentnavigation.plugins.topxup.com
- Bugtracker： https://github.com/fuuqiu/CommentNavigation/issues
- Copyright：Copyright © 2024–2026 Fuuqiu (Tinyue)
- Description：补充长 SQL、报表和 HTTP 请求集合的使用场景及官网链接；保留至下次版本更新。
- Getting Started：补充安装、支持文件、六级语法、打开工具窗口、导航和即时刷新步骤。
- Media：已上传 docs/media 中的三张真实 IDE 截图，分别展示复杂 SQL、76 张表结构和 HTTP 场景。
- 隐私正文：根目录 PRIVACY.md；已公开并将 GitHub 链接保存至 Privacy Policy 字段。
- 未提供真实视频或独立论坛，相关字段留空。

验证：16 项单元测试、buildPlugin、verifyPluginStructure 通过；76 条建表及 8 条查询通过 MySQL 方言解析，查询字段均可从结构解析。未连接数据库执行。独立 IDEA 中验证 SQL/HTTP 大纲切换、展开折叠，以及分析第 39 行和订单表第 309 行跳转。

演示环境出现的 ProfilerRunConfigurationsManager / JVM DTrace 配置保存异常来自 IDE 自带 Profiler；个人 IDE 安装时的索引冻结日志归因于自带 database 插件。本次实测不等同于全量 Plugin Verifier 检查。


## 2026-09-20 提交 1.2.2

- 已通过 Chrome 登录会话上传到现有条目 34287 的 Stable 渠道，未勾选隐藏更新。
- 版本 1.2.1 已发布，因此本次兼容性与重命名功能以 1.2.2 提交。
- 更新 ID：1175618；[审核详情](https://plugins.jetbrains.com/plugin/34287-code-comment-navigator/edit/versions/stable/1175618)。
- 提交后页面状态：Under review，尚不代表审核通过；页面提示附加审核可能需要最多 2 个工作日。
- 页面确认 Compatibility Range：251.0 — 262.*。
- 更新说明：支持 IDEA 2025.1–2026.2，Java 21 字节码；Scratches 文件右键 / F2 重命名及同名保护。
- 上传文件：`build/distributions/comment-navigation-1.2.2.zip`。
- SHA-256：`6ccbf89afe8bd52dca45096a740e7b9c1370dab382f4c5a2b321971228ac573a`。
- 提交前执行 `test buildPlugin verifyPluginStructure` 成功；功能代码此前已通过 2025.1 / 2026.2 Plugin Verifier 检查。
