package cn.tinyue.console.service

import cn.tinyue.console.model.Comment
import com.intellij.openapi.editor.Document

/**
 * SQL文件解析器
 */
interface SqlFileParser {
    /**
     * 解析文件中的注释
     * @param document 文档对象
     * @return 注释列表
     */
    fun parseComments(document: Document): List<Comment>

    /**
     * 更新注释
     * @param document 文档对象
     */
    fun updateComments(document: Document)
} 