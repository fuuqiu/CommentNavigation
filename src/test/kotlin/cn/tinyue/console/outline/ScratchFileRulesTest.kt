package cn.tinyue.console.outline

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScratchFileRulesTest {
    @Test
    fun `仅显示 HTTP 和 SQL 文件且忽略扩展名大小写`() {
        listOf("shop.http", "shop.SQL", "shop.HTTP").forEach { assertTrue(ScratchFileRules.supports(it)) }
        listOf("shop.rest", "shop.sql.bak", "shop.json", "sql", "").forEach { assertFalse(ScratchFileRules.supports(it)) }
    }

    @Test
    fun `文件名包含匹配不把目录名当成文件名`() {
        assertTrue(ScratchFileRules.matches("nested/my-Shop.sql", " shop "))
        assertFalse(ScratchFileRules.matches("shop/orders.sql", "shop"))
        assertTrue(ScratchFileRules.matches("orders.sql", ""))
    }

    @Test
    fun `搜索保留所有目标目录且只展示匹配文件`() {
        val files = listOf("a/nested/shop.http", "ab/shop.sql", "a/orders.sql")
        val directories = listOf("a", "a/nested", "ab", "empty")
        assertEquals(setOf("a/nested/shop.http", "ab/shop.sql", "a", "a/nested", "ab", "empty"), ScratchFileRules.visiblePaths(files, directories, "shop"))
        assertEquals((files + directories).toSet(), ScratchFileRules.visiblePaths(files, directories, ""))
        assertEquals(directories.toSet(), ScratchFileRules.visiblePaths(files, directories, "absent"))
    }

    @Test
    fun `搜索结果移动到其他目录后继续可见`() {
        val directories = listOf("source", "archive", "archive/reports")
        val before = ScratchFileRules.visiblePaths(listOf("source/shop.sql", "source/other.http"), directories, "SHOP")
        assertTrue(before.contains("archive/reports"))
        val after = ScratchFileRules.visiblePaths(listOf("archive/reports/shop.sql", "source/other.http"), directories, "SHOP")
        assertTrue(after.contains("archive/reports/shop.sql"))
        assertFalse(after.contains("source/shop.sql"))
        assertFalse(after.contains("source/other.http"))
    }

    @Test
    fun `按后缀识别文件且无后缀保留为目录`() {
        listOf("shop.sql", "shop.SQL", "shop.HTTP", "shop.v2.sql", "shop.http").forEach {
            assertEquals(it, ScratchFileRules.normalizeName(" $it "))
            assertTrue(ScratchFileRules.supports(it))
        }
        assertEquals("shop", ScratchFileRules.normalizeName(" shop "))
        assertFalse(ScratchFileRules.supports("shop"))
        listOf("shop.json", "shop.rest", "shop.sql.bak", "shop.v2", ".sql", ".http", "shop.").forEach {
            assertNull(ScratchFileRules.normalizeName(it), it)
        }
    }

    @Test
    fun `拒绝路径穿越非法字符和空名称`() {
        listOf("", " ", ".", "..", "../shop", "a/b", "a\\b", "a\n", "a\u0000", "a:b", "a?b").forEach {
            assertNull(ScratchFileRules.normalizeName(it), it)
        }
    }

    @Test
    fun `新建模板能被大纲解析且 HTTP 标题在分隔符之后`() {
        val parser = CommentOutlineParser()
        assertEquals(listOf(OutlineHeading("shop", 1, 1)), parser.parse(ScratchFileRules.initialContent("shop.sql"), "shop.sql"))
        val http = ScratchFileRules.initialContent("shop.http")
        assertTrue(http.startsWith("###\n// # "))
        assertEquals(listOf("shop"), parser.parse(http, "shop.http").map { it.title })
    }
}
