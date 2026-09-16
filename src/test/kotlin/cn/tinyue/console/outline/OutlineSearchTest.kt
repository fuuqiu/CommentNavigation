package cn.tinyue.console.outline

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OutlineSearchTest {
    private val nodes = CommentOutlineParser().buildTree(listOf(
        OutlineHeading("订单", 1, 1),
        OutlineHeading("Shop 查询", 2, 4),
        OutlineHeading("详情", 3, 8),
        OutlineHeading("退款", 2, 12),
        OutlineHeading("商品", 1, 18)
    ))

    @Test
    fun `子标题匹配保留祖先与真实行号并移除无关分组`() {
        val result = OutlineSearch.filter(nodes, "详情")
        assertEquals(listOf("订单"), result.map { it.heading.title })
        assertEquals(listOf("Shop 查询"), result.single().children.map { it.heading.title })
        assertEquals(8, result.single().children.single().children.single().heading.lineNumber)
        assertEquals(2, nodes.first().children.size)
    }

    @Test
    fun `匹配不区分大小写且父标题匹配保留整个分组`() {
        val result = OutlineSearch.filter(nodes, " shop ")
        assertEquals("详情", result.single().children.single().children.single().heading.title)
        assertEquals(nodes.first(), OutlineSearch.filter(nodes, "订单").single())
    }

    @Test
    fun `空查询恢复全部标题且无结果不展示目录`() {
        assertEquals(nodes, OutlineSearch.filter(nodes, "  "))
        assertTrue(OutlineSearch.filter(nodes, "不存在").isEmpty())
    }
}
