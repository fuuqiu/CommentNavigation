package cn.tinyue.console.service.impl

import cn.tinyue.console.model.CommentType
import cn.tinyue.console.model.Comment
import cn.tinyue.console.service.JavaDocParser
import com.intellij.openapi.editor.Document

/**
 * Java文档解析器实现类
 */
class JavaDocParserImpl : JavaDocParser {
    // 匹配函数定义的正则表达式，包括接口方法和复杂泛型
    private val functionPattern = Regex("""^\s*(public\s+|private\s+|protected\s+)?\s*(static\s+)?\s*(final\s+)?\s*(?:<[^>]+>\s+)?(?:[\w<>,\s]+(?:<[^>]+>)?(?:\[\])*\s+\w+\s*\(.*|\w+\s*\(.*)""")

    override fun parseComments(document: Document): List<Comment> {
        val comments = mutableListOf<Comment>()
        val text = document.text
        val lines = text.lines()
        
        var currentComment = StringBuilder()
        var isInComment = false
        var commentStartLine = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            // 获取下一行，如果存在的话
            val nextLine = if (index + 1 < lines.size) {
                // 处理可能跨行的泛型声明
                var fullLine = lines[index + 1].trim()
                var i = index + 2
                while (i < lines.size && !fullLine.contains("(") && !fullLine.startsWith("//") && !fullLine.startsWith("/*")) {
                    fullLine += " " + lines[i].trim()
                    i++
                }
                fullLine
            } else ""
            
            val nextLineIsFunction = nextLine.isNotEmpty() && 
                                   !nextLine.startsWith("//") && 
                                   !nextLine.startsWith("/*") &&
                                   !nextLine.startsWith("*") &&
                                   (functionPattern.find(nextLine) != null || 
                                    nextLine.matches(Regex("""^[\w<>,\s]+(?:<[^>]+>)?(?:\[\])*\s+\w+\s*\(.*""")))
            
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
                                    type = CommentType.JAVA_DOC,
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
                                type = CommentType.JAVA_DOC,
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