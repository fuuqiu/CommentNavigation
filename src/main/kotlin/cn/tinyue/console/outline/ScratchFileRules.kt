package cn.tinyue.console.outline

internal object ScratchFileRules {
    fun supports(name: String): Boolean = name.substringAfterLast('.', "").let {
        it.equals("http", ignoreCase = true) || it.equals("sql", ignoreCase = true)
    }

    fun matches(path: String, query: String): Boolean =
        path.substringAfterLast('/').contains(query.trim(), ignoreCase = true)

    // 搜索只过滤文件，保留全部目录作为拖动目标。
    fun visiblePaths(files: List<String>, directories: List<String>, query: String): Set<String> {
        if (query.isBlank()) return (files + directories).toSet()
        val matches = files.filter { matches(it, query) }
        return (matches + directories).toSet()
    }

    fun normalizeName(input: String): String? {
        if (input.any { it.code < 32 }) return null
        val name = input.trim()
        if (name.isEmpty() || name.startsWith('.') || name.endsWith('.') || name.any { it in "/\\:*?\"<>|" }) return null
        if ('.' !in name) return name
        return name.takeIf { supports(it) }
    }

    fun initialContent(name: String): String {
        val title = name.substringBeforeLast('.').replace(Regex("[\r\n]"), " ")
        return if (name.endsWith(".sql", ignoreCase = true)) "-- # $title\n\n"
        else "###\n// # $title\n\n"
    }
}
