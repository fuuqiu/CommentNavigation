package cn.tinyue.console.outline

/** 行号从 1 开始，层级保留 Markdown 原始深度。 */
data class OutlineHeading(val title: String, val level: Int, val lineNumber: Int)

data class OutlineNode(val heading: OutlineHeading, val children: MutableList<OutlineNode> = mutableListOf())

class CommentOutlineParser {
    private val headingPattern = Regex("^(#{1,6})\\s+(.+?)\\s*$")
    private val dollarQuotePattern = Regex("\\$(?:[A-Za-z_][A-Za-z_0-9]*)?\\$")

    fun supports(fileName: String): Boolean = fileName.substringAfterLast('.').lowercase() in setOf("sql", "http", "rest")

    fun parse(text: String, fileName: String): List<OutlineHeading> {
        if (!supports(fileName)) return emptyList()
        val sql = fileName.endsWith(".sql", ignoreCase = true)
        val headings = mutableListOf<OutlineHeading>()
        val legacyComments = mutableListOf<OutlineHeading>()
        var blockDepth = 0
        var quote: Char? = null
        var dollarQuote: String? = null
        var inScript = false

        fun collect(content: String, lineNumber: Int) {
            val value = content.trim().removePrefix("*").trim()
            if (value.isBlank()) return
            val match = headingPattern.matchEntire(value)
            if (match != null) {
                val title = match.groupValues[2].replace(Regex("\\s+#+\\s*$"), "").trim()
                if (title.isNotEmpty() && title.any { it != '#' }) {
                    headings.add(OutlineHeading(title, match.groupValues[1].length, lineNumber))
                }
            } else if (!value.startsWith('#')) {
                legacyComments.add(OutlineHeading(value, 1, lineNumber))
            }
        }

        text.lineSequence().forEachIndexed { lineIndex, line ->
            val trimmed = line.trimStart()
            val lineNumber = lineIndex + 1
            if (!sql) {
                // HTTP 脚本内的注释不是请求目录。
                if (inScript) {
                    if (trimmed.startsWith("%}")) inScript = false
                    return@forEachIndexed
                }
                if (trimmed.startsWith("> {%") || trimmed.startsWith("< {%")) {
                    inScript = !trimmed.contains("%}")
                    return@forEachIndexed
                }
                when {
                    trimmed.startsWith("### ") || trimmed.startsWith("###\t") -> {
                        val title = trimmed.substring(3).trim()
                        if (title.isNotEmpty()) headings.add(OutlineHeading(title, 2, lineNumber))
                    }
                    trimmed.startsWith("//") -> collect(trimmed.substring(2), lineNumber)
                    trimmed.startsWith('#') -> collect(trimmed.substring(1), lineNumber)
                }
                return@forEachIndexed
            }

            var index = 0
            var onlyComment = true
            while (index < line.length) {
                val delimiter = dollarQuote
                when {
                    delimiter != null -> {
                        val end = line.indexOf(delimiter, index)
                        if (end < 0) break
                        dollarQuote = null
                        index = end + delimiter.length
                        onlyComment = false
                    }
                    quote != null -> {
                        val current = line[index++]
                        if (current == '\\' && index < line.length) {
                            index++
                        } else if (current == quote) {
                            if (index < line.length && line[index] == quote) index++ else quote = null
                        }
                        onlyComment = false
                    }
                    blockDepth > 0 -> {
                        val start = index
                        while (index < line.length && !line.startsWith("*/", index) && !line.startsWith("/*", index)) index++
                        if (onlyComment) collect(line.substring(start, index), lineNumber)
                        when {
                            line.startsWith("*/", index) -> { blockDepth--; index += 2 }
                            line.startsWith("/*", index) -> { blockDepth++; index += 2 }
                        }
                    }
                    line.startsWith("--", index) -> {
                        if (onlyComment) collect(line.substring(index + 2), lineNumber)
                        break
                    }
                    line[index] == '#' -> {
                        // MySQL 注释中的引号不参与后续 SQL 的字符串状态。
                        if (onlyComment) collect(line.substring(index + 1), lineNumber)
                        break
                    }
                    line.startsWith("/*", index) -> { blockDepth++; index += 2 }
                    line[index].isWhitespace() -> index++
                    line[index] in "'\"`[" -> {
                        quote = if (line[index] == '[') ']' else line[index]
                        onlyComment = false
                        index++
                    }
                    line[index] == '$' -> {
                        val match = dollarQuotePattern.find(line, index)?.takeIf { it.range.first == index }
                        dollarQuote = match?.value
                        index += match?.value?.length ?: 1
                        onlyComment = false
                    }
                    else -> { onlyComment = false; index++ }
                }
            }
        }
        // 老脚本可直接导航；出现显式标题后，只展示标题以减少噪声。
        return headings.ifEmpty { legacyComments }
    }

    fun buildTree(headings: List<OutlineHeading>): List<OutlineNode> {
        val roots = mutableListOf<OutlineNode>()
        val parents = ArrayDeque<OutlineNode>()
        for (heading in headings) {
            while (parents.isNotEmpty() && parents.last().heading.level >= heading.level) parents.removeLast()
            val node = OutlineNode(heading)
            if (parents.isEmpty()) roots.add(node) else parents.last().children.add(node)
            parents.addLast(node)
        }
        return roots
    }
}
