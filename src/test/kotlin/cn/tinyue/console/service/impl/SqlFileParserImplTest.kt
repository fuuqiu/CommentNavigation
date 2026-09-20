package cn.tinyue.console.service.impl

import cn.tinyue.console.model.CommentType
import com.intellij.openapi.editor.Document
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("SQL文件解析器测试")
class SqlFileParserImplTest {
    private lateinit var parser: SqlFileParserImpl
    private lateinit var document: Document

    @BeforeEach
    fun setUp() {
        parser = SqlFileParserImpl()
        document = mockk()
    }

    @Test
    fun `SQL 弹窗兼容 MySQL 井号注释并保留原始行号`() {
        every { document.text } returns "# 查询角色\nSELECT '# 字符串';\n  # ## 授权明细\n#\n-- 其他说明"
        val comments = parser.parseComments(document)
        assertEquals(listOf("查询角色", "## 授权明细", "其他说明"), comments.map { it.content })
        assertEquals(listOf(1, 3, 5), comments.map { it.lineNumber })
    }

    @Test
    @DisplayName("测试解析单行注释")
    fun testParseSingleLineComment() {
        // 准备测试数据
        val sql = """
            -- 查询用户信息
            SELECT * FROM users;
            
            -- 查询订单信息
            SELECT * FROM orders;
        """.trimIndent()
        
        every { document.text } returns sql

        // 执行测试
        val comments = parser.parseComments(document)

        // 验证结果
        assertEquals(2, comments.size)
        
        with(comments[0]) {
            assertEquals("查询用户信息", content)
            assertEquals(1, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[1]) {
            assertEquals("查询订单信息", content)
            assertEquals(4, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
    }

    @Test
    @DisplayName("测试解析多行注释")
    fun testParseMultiLineComment() {
        // 准备测试数据
        val sql = """
            /* 这是一个
               多行注释
               用于测试 */
            SELECT * FROM users;
            
            /*
             * 另一个多行注释
             * 包含星号
             */
            SELECT * FROM orders;
        """.trimIndent()
        
        every { document.text } returns sql

        // 执行测试
        val comments = parser.parseComments(document)

        // 验证结果
        assertEquals(2, comments.size)
        
        with(comments[0]) {
            assertEquals("这是一个 多行注释 用于测试", content)
            assertEquals(3, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[1]) {
            assertEquals("另一个多行注释 包含星号", content)
            assertEquals(9, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
    }

    @Test
    @DisplayName("测试混合注释")
    fun testParseMixedComments() {
        // 准备测试数据
        val sql = """
            -- 单行注释1
            SELECT * FROM users;
            
            /* 这是一个
               多行注释 */
            SELECT * FROM orders;
            
            -- 单行注释2
            DELETE FROM users;
        """.trimIndent()
        
        every { document.text } returns sql

        // 执行测试
        val comments = parser.parseComments(document)

        // 验证结果
        assertEquals(3, comments.size)
        
        with(comments[0]) {
            assertEquals("单行注释1", content)
            assertEquals(1, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[1]) {
            assertEquals("这是一个 多行注释", content)
            assertEquals(5, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[2]) {
            assertEquals("单行注释2", content)
            assertEquals(8, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
    }

    @Test
    @DisplayName("测试空文件")
    fun testParseEmptyFile() {
        every { document.text } returns ""
        
        val comments = parser.parseComments(document)
        
        assertTrue(comments.isEmpty())
    }

    @Test
    @DisplayName("测试没有注释的SQL")
    fun testParseNoComments() {
        val sql = """
            SELECT * FROM users;
            SELECT * FROM orders;
            DELETE FROM users;
        """.trimIndent()
        
        every { document.text } returns sql
        
        val comments = parser.parseComments(document)
        
        assertTrue(comments.isEmpty())
    }

    @Test
    @DisplayName("测试注释后没有SQL语句")
    fun testParseCommentsWithoutSQL() {
        val sql = """
            -- 这是一个注释
            -- 这是另一个注释
            /* 这是多行注释
               没有SQL语句 */
        """.trimIndent()
        
        every { document.text } returns sql
        
        val comments = parser.parseComments(document)
        
        assertFalse(comments.isEmpty())
        assertEquals(3, comments.size)
        
        with(comments[0]) {
            assertEquals("这是一个注释", content)
            assertEquals(1, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[1]) {
            assertEquals("这是另一个注释", content)
            assertEquals(2, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
        
        with(comments[2]) {
            assertEquals("这是多行注释 没有SQL语句", content)
            assertEquals(4, lineNumber)
            assertEquals(CommentType.SQL, type)
            assertFalse(isFunction)
        }
    }
}
