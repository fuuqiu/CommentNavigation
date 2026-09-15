# Code Comment Navigator — SQL / HTTP 注释大纲

在 IntelliJ IDEA 右侧显示当前 `.sql`、`.http`、`.rest` 文件的注释目录。适用于项目文件和 Scratches 中不断增长的迭代脚本。

## 免费与开源

本项目采用 **MIT License**，个人和企业均可免费使用，包括商业用途，无需购买、订阅或激活码。允许修改、再分发及销售，须按许可要求保留版权及许可声明。

完整条款见 [LICENSE.txt](LICENSE.txt)，安装包内同时附带该文件。软件按现状提供，不承诺固定更新周期或支持响应时间。

[源代码](https://github.com/fuuqiu/CommentNavigation) · [问题反馈](https://github.com/fuuqiu/CommentNavigation/issues) · 邮箱：fuuqiu@gmail.com

## 使用

1. 安装插件并打开 SQL / HTTP 文件。
2. 点击右侧 **Comment Outline**，或通过 **View → Tool Windows → Comment Outline** 打开。
3. 使用 `#` 到 `######` 标记标题层级；点击标题即可跳转到对应注释行。

面板跟随当前编辑器，修改后约 250 ms 自动更新，不需要保存。支持折叠/展开、全部展开/折叠、在树中直接输入标题搜索，以及上下键选择、Enter 跳转。同一文件编辑时保留折叠状态；切换文件默认展开。空文件及不支持的文件会清空目录。

编辑器右键或 Edit 菜单中的 **Comment Structure** 也可打开大纲。原有 Java / Kotlin 注释弹窗保留。

## 标题写法

### SQL

```sql
-- # 角色与权限核对
-- ## 角色本体
-- ### 系统模板
SELECT 'system role';

-- ### 商户角色
SELECT 'merchant role';

-- ## 授权明细
/*
 * ### 页面权限
 * #### PRO 页面
 */
SELECT 'page permission';
```

支持 `--` 行注释及 `/* ... */` 块注释中的 Markdown 标题。标题必须独占注释行；不提取 SQL 语句行尾注释、字符串或引用标识符中的伪标题。

### HTTP

推荐用 `//` 注释书写标题，也支持 `#` 注释后再加 Markdown 标题：

```http
// # 货柜查询
// ## 正常场景
# ### 响应字段核对

### 批量查询货柜
GET {{host}}/freezers

// # 补货单
### 补货单详情
GET {{host}}/replenishments/example
```

| 写法 | 大纲层级 |
| --- | --- |
| `// # 标题` 或 `# # 标题` | 一级 |
| `// ## 标题` 或 `# ## 标题` | 二级 |
| `// ### 标题` 或 `# ### 标题` | 三级 |
| `### 请求名称` | 二级，兼容 HTTP Client 原有请求分隔符 |

HTTP 的 `### 请求名称` 固定视为二级节点，因此不要用它表达三级标题。需要严格控制层级时使用 `// ### 标题` 等明确写法。空的 `###` 不进入目录；请求前置和响应处理脚本中的注释不进入目录。

标题标记后须留空格，支持 1–6 级。跳级时挂在前一个层级更浅的标题下，同级标题保持文件顺序。普通说明不会混入显式标题大纲；整个文件没有任何标题或请求名称时，回退到普通注释的平级列表。

可直接打开 [SQL 示例](examples/comment-outline.sql) 和 [HTTP 示例](examples/comment-outline.http) 体验层级目录。示例不包含真实业务地址或数据。

## 构建与安装

本版本构建及兼容范围为 **IntelliJ IDEA 2026.2（262.*）**，需要 **JDK 25**。解析使用平台 Document API，无需 SQL / HTTP 语言插件依赖。更早的 IDE 版本未纳入本次兼容范围。

```bash
./gradlew test buildPlugin
```

如果本机已经安装 IDEA 2026.2，可复用其 SDK，避免下载 IDE：

```bash
JAVA_HOME='/Users/apple/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  ./gradlew test buildPlugin \
  '-PlocalIdePath=/Users/apple/Applications/IntelliJ IDEA.app/Contents'
```

安装包位于 `build/distributions/comment-navigation-1.1.0.zip`。在 IDEA 的 **Settings → Plugins → 齿轮菜单 → Install Plugin from Disk…** 中选择 ZIP，并按 IDE 提示完成安装。

开发时运行 `./gradlew runIde`（可加上述 `-PlocalIdePath` 参数）会使用 Gradle 插件的隔离沙箱，不使用个人 IDEA 配置。
