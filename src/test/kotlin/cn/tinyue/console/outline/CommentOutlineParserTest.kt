package cn.tinyue.console.outline

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommentOutlineParserTest {
    private val parser = CommentOutlineParser()

    @Test
    fun `MySQL 普通井号注释与其他注释按行号进入旧脚本大纲`() {
        val sql = "-- 查询子商户\r\nSELECT 1;\r\n  # 查询角色具体权限 [层级显示]\r\nSELECT 2;\r\n#无空格说明\r\n/* 核对授权 */"
        assertEquals(listOf(
            OutlineHeading("查询子商户", 1, 1),
            OutlineHeading("查询角色具体权限 [层级显示]", 1, 3),
            OutlineHeading("无空格说明", 1, 5),
            OutlineHeading("核对授权", 1, 6)
        ), parser.parse(sql, "console.sql"))
    }

    @Test
    fun `MySQL 井号注释内的标题与现有标题混合保持层级`() {
        val sql = "# 普通说明\n# # 角色核对\n# ## 授权明细\n-- ### 页面权限\n/* ## 模块关联 */\n# ###### 最深标题\n# ###\n# ####### 太深"
        val headings = parser.parse(sql, "roles.SQL")
        assertEquals(listOf(
            OutlineHeading("角色核对", 1, 2),
            OutlineHeading("授权明细", 2, 3),
            OutlineHeading("页面权限", 3, 4),
            OutlineHeading("模块关联", 2, 5),
            OutlineHeading("最深标题", 6, 6)
        ), headings)
        assertEquals(listOf("授权明细", "模块关联"), parser.buildTree(headings).single().children.map { it.heading.title })
    }

    @Test
    fun `MySQL 行尾注释与注释中的引号不吞掉后续标题`() {
        val sql = "# user's note with /* and ' \"\nSELECT 1; # don't parse ' /* -- # 行尾\n# # 后续标题"
        assertEquals(listOf(OutlineHeading("后续标题", 1, 3)), parser.parse(sql, "sample.sql"))
    }

    @Test
    fun `字符串标识符和美元引用中的井号不会成为注释标题`() {
        val sql = """
            SELECT '# # 字符串', `# 标识符`;
            SELECT '多行
            # # 字符串内
            结尾';
            SELECT ${'$'}tag${'$'}
            # # 美元引用内
            ${'$'}tag${'$'};
            SELECT "多行标识符
            # # 标识符内
            ";
            SELECT 1; # # 行尾注释
            # # 真正标题
        """.trimIndent()
        assertEquals(listOf(OutlineHeading("真正标题", 1, 12)), parser.parse(sql, "sample.sql"))
    }

    @Test
    fun `SQL 标题优先且保留精确行号`() {
        val headings = parser.parse("-- 普通说明\r\n-- # 角色核对\r\nSELECT 1;\r\n  -- ### 授权明细 ###\r\n-- ## 模块关联", "sample.SQL")
        assertEquals(listOf(
            OutlineHeading("角色核对", 1, 2),
            OutlineHeading("授权明细", 3, 4),
            OutlineHeading("模块关联", 2, 5)
        ), headings)
    }

    @Test
    fun `层级跳跃同级回退及重复标题均保持文件顺序`() {
        val tree = parser.buildTree(listOf(
            OutlineHeading("角色", 1, 1), OutlineHeading("检查", 4, 2),
            OutlineHeading("检查", 4, 3), OutlineHeading("模块", 2, 4),
            OutlineHeading("明细", 6, 5), OutlineHeading("货柜", 1, 6)
        ))
        assertEquals(listOf("角色", "货柜"), tree.map { it.heading.title })
        assertEquals(listOf("检查", "检查", "模块"), tree[0].children.map { it.heading.title })
        assertEquals(3, tree[0].children[1].heading.lineNumber)
        assertEquals("明细", tree[0].children[2].children.single().heading.title)
    }

    @Test
    fun `SQL 块注释标题含单行和星号装饰`() {
        val headings = parser.parse("/* # 单行 */\n/*\n * ## 多行\n * 普通说明\n * ### 子项\n */\nSELECT 1;", "sample.sql")
        assertEquals(listOf(OutlineHeading("单行", 1, 1), OutlineHeading("多行", 2, 3), OutlineHeading("子项", 3, 5)), headings)
    }

    @Test
    fun `SQL 字符串标识符美元引用和行尾注释不会成为标题`() {
        val sql = """
            SELECT '-- # 字符串', '/* # 字符串 */';
            SELECT '多行
            -- # 字符串内
            结尾';
            SELECT ${'$'}tag${'$'}
            -- # 美元引用内
            ${'$'}tag${'$'};
            SELECT "多行标识符
            -- # 标识符内
            ";
            SELECT 1; -- # 行尾注释
            -- # 真正标题
        """.trimIndent()
        assertEquals(listOf(OutlineHeading("真正标题", 1, 12)), parser.parse(sql, "sample.sql"))
    }

    @Test
    fun `SQL 转义引号及嵌套块注释不破坏后续标题`() {
        val sql = "SELECT 'it''s -- # ignored';\n/* 外层\n /* ## 内层 */\n * # 外层标题\n */\n-- ## 后续"
        assertEquals(listOf("内层", "外层标题", "后续"), parser.parse(sql, "sample.sql").map { it.title })
    }

    @Test
    fun `HTTP 两种注释格式与请求分隔符组成层级`() {
        val http = """
            # 环境说明
            // # 货柜查询
            ### 正常请求
            GET {{host}}/freezers
            # ### 响应核对
            // #### 字段说明
            ### 异常请求
            GET {{host}}/freezers?ids=
            # # 补货单
            // ## 明细
        """.trimIndent()
        val headings = parser.parse(http, "sample.http")
        assertEquals(listOf(1, 2, 3, 4, 2, 1, 2), headings.map { it.level })
        assertEquals(listOf(2, 3, 5, 6, 7, 9, 10), headings.map { it.lineNumber })
        val tree = parser.buildTree(headings)
        assertEquals(listOf("货柜查询", "补货单"), tree.map { it.heading.title })
        assertEquals(listOf("正常请求", "异常请求"), tree[0].children.map { it.heading.title })
    }

    @Test
    fun `HTTP 兼容旧请求目录但忽略空分隔符和脚本注释`() {
        val http = """
            ###
            ### 查询
            GET {{host}}/query
            > {%
            // # 脚本内部
            client.test("query", () => {});
            %}
            < {% const x = 1; %}
            ### 校验
        """.trimIndent()
        assertEquals(listOf(OutlineHeading("查询", 2, 2), OutlineHeading("校验", 2, 9)), parser.parse(http, "sample.rest"))
    }

    @Test
    fun `没有 Markdown 时回退到普通注释`() {
        assertEquals(listOf(OutlineHeading("角色", 1, 1), OutlineHeading("模块", 1, 3)), parser.parse("-- 角色\nSELECT 1;\n-- 模块", "sample.sql"))
        assertEquals(listOf("说明", "请求说明"), parser.parse("# 说明\n// 请求说明\nGET {{host}}", "sample.http").map { it.title })
    }

    @Test
    fun `不合法和空标题不会进入大纲`() {
        assertTrue(parser.parse("-- #\n-- ####### 太深\n-- #无空格\n-- ## ###\n--", "sample.sql").isEmpty())
        assertTrue(parser.parse("", "sample.sql").isEmpty())
        assertTrue(parser.parse("SELECT 1;", "sample.sql").isEmpty())
        assertTrue(parser.parse("-- # 标题", "sample.txt").isEmpty())
        assertTrue(parser.buildTree(emptyList()).isEmpty())
    }

    @Test
    fun `重解析能反映未保存的插行改名和删除`() {
        val original = "-- # 角色\n-- ## 旧标题"
        assertEquals(2, parser.parse(original, "sample.sql")[1].lineNumber)
        val edited = "\n" + original.replace("旧标题", "新标题")
        assertEquals(OutlineHeading("新标题", 2, 3), parser.parse(edited, "sample.sql")[1])
        assertEquals(1, parser.parse("-- # 角色", "sample.sql").size)
    }
}
