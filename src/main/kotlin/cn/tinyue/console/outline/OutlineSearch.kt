package cn.tinyue.console.outline

internal object OutlineSearch {
    // 保留匹配标题及其父级；父标题匹配时保留整个分组。
    fun filter(nodes: List<OutlineNode>, query: String): List<OutlineNode> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return nodes
        return nodes.mapNotNull { node ->
            if (node.heading.title.contains(keyword, ignoreCase = true)) node
            else {
                val children = filter(node.children, keyword)
                if (children.isEmpty()) null else OutlineNode(node.heading, children.toMutableList())
            }
        }
    }
}
