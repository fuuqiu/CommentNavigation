package cn.tinyue.console.service

import cn.tinyue.console.model.Comment
import com.intellij.openapi.editor.Document

/**
 * Kotlin文档解析器
 */
interface KotlinDocParser {
    /**
     * 解析文件中的Kotlin文档注释
     * @param document 文档对象
     * @return 注释列表
     */
    fun parseComments(document: Document): List<Comment>
} 