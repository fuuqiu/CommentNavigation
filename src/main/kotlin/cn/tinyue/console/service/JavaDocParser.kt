package cn.tinyue.console.service

import cn.tinyue.console.model.Comment
import com.intellij.openapi.editor.Document

/**
 * Java文档解析器
 */
interface JavaDocParser {
    /**
     * 解析文件中的Java文档注释
     * @param document 文档对象
     * @return 注释列表
     */
    fun parseComments(document: Document): List<Comment>
} 