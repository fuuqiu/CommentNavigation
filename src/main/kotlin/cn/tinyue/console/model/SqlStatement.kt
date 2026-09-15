package cn.tinyue.console.model

/**
 * SQL语句数据模型
 */
data class SqlStatement(
    var content: String,      // SQL语句内容
    var lineNumber: Int,      // SQL语句行号
    var comment: Comment? = null  // 关联的注释
) 