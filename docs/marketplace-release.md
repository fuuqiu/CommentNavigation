# MIT 开源版发布与 Marketplace 解封

## 本地准备结果

- 产品：Code Comment Navigator Pro。
- 保留 Marketplace 条目 27390 和插件 ID `cn.tinyue.console`。
- 版本：1.1.0；兼容 IDEA 2026.2（262.*）。
- 许可：MIT，唯一正文为根目录 `LICENSE.txt`；`LICENSE.md` 为阅读指引。
- 允许免费使用、修改和再分发，包括商业用途，须保留版权及许可声明。
- 安装包内 `META-INF/LICENSE.txt` 包含相同许可正文。
- 描述已清理购买、退款、授权到期和固定支持时限等旧商业条款。
- 联系邮箱统一为 fuuqiu@gmail.com；网站与源码统一指向当前 GitHub 仓库。

上述修改准备在本地，不能据此认定 GitHub 或 Marketplace 已经更新或解封。

## 发布顺序

### 1. 发布并核对 GitHub 内容

将本次代码、README 和 MIT 许可发布到 `fuuqiu/CommentNavigation` 的 `main` 分支后，检查以下地址显示的是新版内容：

- 源码：https://github.com/fuuqiu/CommentNavigation
- 许可：https://github.com/fuuqiu/CommentNavigation/blob/main/LICENSE.txt
- 说明：https://github.com/fuuqiu/CommentNavigation/blob/main/README.md
- 反馈：https://github.com/fuuqiu/CommentNavigation/issues

特别核对 LICENSE.txt 显示 MIT License，而非旧 Commercial License Agreement。公开源码仓库仍包含旧版许可时，不能把本地修改当作公开开源已经完成。

### 2. 向 Marketplace 确认解封要求

沿用原插件条目：https://plugins.jetbrains.com/plugin/27390-code-comment-navigator-pro

联系 marketplace@jetbrains.com，索取具体封禁原因、涉及版本及整改要求，并说明后续免费开源的计划。修改许可本身不会自动解除封禁，也不能证明旧封禁一定由商业描述造成。

下面是可直接使用的邮件草稿，尚未发送：

**Subject: Remediation and MIT-licensed free release — Plugin 27390**

Hello JetBrains Marketplace Team,

I am the developer of Code Comment Navigator Pro (Marketplace ID: 27390; XML ID: cn.tinyue.console).

The plugin page shows a block notice without a specific reason. Could you please provide the applicable policy clause, the affected version or listing content, and the original moderation notice?

I would like to distribute the plugin free of charge and make the source code available under the MIT License. I have prepared version 1.1.0 locally with:

- MIT licensing and an included license file;
- updated listing text without purchase, refund, subscription, or fixed support-period claims;
- consistent project and contact links;
- fixes to the navigation action registration and SQL block-comment parsing;
- a SQL/HTTP comment outline and compatibility with IntelliJ IDEA 2026.2.

Repository: https://github.com/fuuqiu/CommentNavigation
Plugin page: https://plugins.jetbrains.com/plugin/27390-code-comment-navigator-pro

Please advise how to submit the revised version and request a review of the block. I would like to retain the existing plugin ID and ensure that the listing is configured as a free plugin. If its current distribution type requires your assistance to change, please let me know the required steps.

Thank you,
Fuuqiu

### 3. 同步 Marketplace 元数据

按支持团队给出的处理方式，核对并更新以下字段：

| 内容 | 目标值 |
| --- | --- |
| 插件名称 | Code Comment Navigator Pro |
| XML ID | cn.tinyue.console |
| 分发类型 | Free；若当前为 Paid 或 Externally Paid，请支持团队确认转换流程 |
| 源码链接 | https://github.com/fuuqiu/CommentNavigation |
| License | MIT；链接到当前仓库的 LICENSE.txt |
| 联系邮箱 | fuuqiu@gmail.com |
| 问题反馈 | https://github.com/fuuqiu/CommentNavigation/issues |
| 描述 | 使用新版 src/main/resources/META-INF/plugin.xml 中的 description |
| 兼容范围 | 262.*，与实际包一致 |

旧条目的 License 指向另一个仓库 `CommentStructure`，需同步更新。后台可能保存了独立的描述覆盖值，上传 ZIP 后仍需检查页面最终显示内容。

免费不等于自动成为 Non-trader；保留基于实际活动作出的经营者身份声明，不仅因为改为免费而切换身份。

### 4. 提交安装包并申请复核

安装包：`build/distributions/console-1.1.0.zip`。

已执行的验证为单元测试、编译、打包及 `verifyPluginStructure`。后者不是 Plugin Verifier 二进制兼容检查，也不代表人工审核通过。正式提交前还应完成目标 IDE 中的点击跳转、编辑刷新及切换文件实测，并按官方要求执行 Plugin Verifier。

## 官方依据

- [审核指南](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html)
- [页面信息与描述来源](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)
- [Marketplace 协议：开源许可证](https://www.jetbrains.com/legal/docs/plugins_site/plugin_marketplace/)
- [封禁通知及异议处理](https://www.jetbrains.com/legal/docs/terms/marketplace-content-moderation/)
