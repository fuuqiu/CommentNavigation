package cn.tinyue.console.service.impl

import cn.tinyue.console.model.CommentType
import cn.tinyue.console.model.Comment
import cn.tinyue.console.service.SqlFileParser
import com.intellij.openapi.editor.Document

/** SQL 弹窗的普通注释解析器。 */
class SqlFileParserImpl : SqlFileParser {
    override fun parseComments(document: Document): List<Comment> {
        val comments = mutableListOf<Comment>()
        val blockLines = mutableListOf<String>()
        var inBlock = false

        document.text.lineSequence().forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (!inBlock && trimmed.startsWith("--")) {
                val content = trimmed.substring(2).trim()
                if (content.isNotEmpty()) comments.add(Comment(content, index + 1, CommentType.SQL))
                return@forEachIndexed
            }
            if (!inBlock && !trimmed.startsWith("/*")) return@forEachIndexed

            val fragment = if (inBlock) trimmed else trimmed.substring(2)
            inBlock = true
            val end = fragment.indexOf("*/")
            val content = (if (end >= 0) fragment.substring(0, end) else fragment)
                .trim().removePrefix("*").trim()
            if (content.isNotEmpty()) blockLines.add(content)
            if (end < 0) return@forEachIndexed

            // 保留原弹窗跳转到块注释结束行的约定。
            if (blockLines.isNotEmpty()) comments.add(Comment(blockLines.joinToString(" "), index + 1, CommentType.SQL))
            blockLines.clear()
            inBlock = false
        }
        return comments
    }

    override fun updateComments(document: Document) {
        parseComments(document)
    }
}
