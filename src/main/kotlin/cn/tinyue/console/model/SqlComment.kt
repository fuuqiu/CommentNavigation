package cn.tinyue.console.model

/**
 * 注释类型
 */
enum class CommentType {
    SQL,        // SQL注释
    JAVA_DOC,   // Java文档注释
    KOTLIN_DOC  // Kotlin文档注释
}

/**
 * 注释模型类
 * @property content 注释内容
 * @property lineNumber 目标行号
 * @property type 注释类型
 * @property isFunction 是否是函数级别的注释
 */
data class Comment(
    val content: String,
    val lineNumber: Int,
    val type: CommentType = CommentType.SQL,
    val isFunction: Boolean = false
) 