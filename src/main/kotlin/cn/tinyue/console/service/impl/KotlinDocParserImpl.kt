package cn.tinyue.console.service.impl

import cn.tinyue.console.model.CommentType
import cn.tinyue.console.model.Comment
import cn.tinyue.console.service.KotlinDocParser
import com.intellij.openapi.editor.Document

/**
 * Kotlin文档解析器实现类
 */
class KotlinDocParserImpl : KotlinDocParser {
    // 匹配函数定义的正则表达式
    private val functionPattern = Regex("""^\s*(override\s+)?(public\s+|private\s+|protected\s+|internal\s+)?(suspend\s+)?(fun\s+[\w\d_]+|fun\s*\()""")

    override fun parseComments(document: Document): List<Comment> {
        val comments = mutableListOf<Comment>()
        val text = document.text
        val lines = text.lines()
        
        var currentComment = StringBuilder()
        var isInComment = false
        var commentStartLine = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            val nextLine = if (index + 1 < lines.size) lines[index + 1].trim() else ""
            val nextLineIsFunction = nextLine.isNotEmpty() && functionPattern.find(nextLine) != null
            
            when {
                // 多行注释结束
                trimmedLine.endsWith("*/") -> {
                    if (isInComment) {
                        // 检查下一行是否是函数定义
                        if (nextLineIsFunction) {
                            // 只取第一个非空内容行作为注释内容
                            val content = currentComment.toString().trim()
                            if (content.isNotEmpty()) {
                                comments.add(Comment(
                                    content = content,
                                    lineNumber = index + 2, // 注释后的下一行
                                    type = CommentType.KOTLIN_DOC,
                                    isFunction = true
                                ))
                            }
                        }
                        currentComment = StringBuilder()
                        isInComment = false
                    }
                }
                
                // 多行注释开始
                trimmedLine.startsWith("/**") -> {
                    isInComment = true
                    commentStartLine = index
                    // 提取第一行的内容（去掉/**）
                    val content = trimmedLine.substring(3).trim()
                    if (content.isNotEmpty()) {
                        currentComment.append(content)
                    }
                }
                
                // 多行注释中间
                isInComment -> {
                    // 如果currentComment为空，说明是第一行有效内容
                    if (currentComment.isEmpty()) {
                        // 去掉行首的 * 号
                        val content = trimmedLine.removePrefix("*").trim()
                        if (content.isNotEmpty()) {
                            currentComment.append(content)
                        }
                    }
                }
                
                // 单行注释
                trimmedLine.startsWith("//") -> {
                    // 检查是否是函数级别的注释
                    if (nextLineIsFunction) {
                        val content = trimmedLine.substring(2).trim()
                        if (content.isNotEmpty()) {
                            comments.add(Comment(
                                content = content,
                                lineNumber = index + 2, // 注释后的下一行
                                type = CommentType.KOTLIN_DOC,
                                isFunction = true
                            ))
                        }
                    }
                }
            }
        }
        
        return comments
    }
} 