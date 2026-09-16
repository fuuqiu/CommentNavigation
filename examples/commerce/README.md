# 大型电商注释导航示例

在 IDEA 中打开本目录的文件，再打开右侧 **Comment Outline**。无需连接数据库即可点击标题、展开业务模块、按标题搜索。

| 文件 | 场景 |
| --- | --- |
| [commerce-schema.sql](commerce-schema.sql) | 76 张表、1,068 行；按商品、订单、营销、会员权限、内容运营分组 |
| [commerce-analytics.sql](commerce-analytics.sql) | 8 个独立只读查询、363 行；CTE、窗口排名、留存分群、RFM、库存和售后 |
| [commerce-api.http](commerce-api.http) | 原创电商 API 演示；业务模块 → 用例 → 请求的层级目录 |

## 使用说明

- SQL 使用 MySQL 8.0+ 语法。结构文件包含建表语句，执行时须选择独立的空数据库；不包含 DROP 或上游 INSERT 数据。
- 分析查询使用固定的 2025 年演示区间。口径、退款归属及库存假设写在对应注释中；这些是演示报表，不是上游官方报表。
- 空库执行分析查询通常返回空集，完整性巡检返回零计数。示例不声称有真实经营结果。
- HTTP 为虚构接口约定，使用不可解析的 `example.invalid` 占位域名，不是 mall 的可执行 API 测试。
- SQL：`-- #` → 业务域，`-- ##` → 模块，`-- ###` → 报表，`-- ####` → 查询阶段。
- HTTP：使用 `// #` 到 `// ######`，在每组请求标题之前用无标题的 `###` 分隔请求，避免原生有标题分隔符的二级节点影响层级。

## 上游来源与许可

结构文件派生自 [macrozheng/mall](https://github.com/macrozheng/mall)，固定提交：
`dcaa93b3150352e5044708d7211b5bed0af4509f`。

- [原始 SQL](https://github.com/macrozheng/mall/blob/dcaa93b3150352e5044708d7211b5bed0af4509f/document/sql/mall.sql)
- [上游 Apache License 2.0](https://github.com/macrozheng/mall/blob/dcaa93b3150352e5044708d7211b5bed0af4509f/LICENSE)
- 随附完整许可：[licenses/mall-Apache-2.0.txt](licenses/mall-Apache-2.0.txt)

修改说明：提取全部 76 条 CREATE TABLE，去除数据导入、DROP、导出工具头及会话设置；按业务域重排；增加 Markdown 注释标题；将表级自增起点重置为 1。保留上游字段、索引、表注释及表名中的原始拼写。

`commerce-schema.sql` 中的上游派生内容仍适用 Apache-2.0，不因本插件采用 MIT 而改为 MIT。原创分析查询、HTTP 示例及说明适用本仓库 MIT License。上游项目与作者未参与或背书本插件。
