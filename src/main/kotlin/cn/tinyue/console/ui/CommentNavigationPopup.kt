package cn.tinyue.console.ui

import cn.tinyue.console.model.CommentType
import cn.tinyue.console.model.Comment
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * 注释结构导航弹窗
 */
class CommentNavigationPopup(
    private val project: Project,
    private val editor: Editor,
    private val comments: List<Comment>
) : Disposable {
    private val searchField = JBTextField()
    private val commentList = JBList<Comment>()
    private var popup: JBPopup? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        // 设置列表渲染器
        commentList.cellRenderer = object : ColoredListCellRenderer<Comment>() {
            override fun customizeCellRenderer(
                list: JList<out Comment>,
                value: Comment,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                // 根据注释类型设置不同的图标
                icon = when (value.type) {
                    CommentType.SQL -> AllIcons.Nodes.DataSchema
                    CommentType.JAVA_DOC -> AllIcons.Nodes.Class
                    CommentType.KOTLIN_DOC -> AllIcons.Nodes.Function
                }
                
                // 显示注释内容
                append(value.content, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                
                // 显示行号
                val lineInfo = when (value.type) {
                    CommentType.SQL -> "SQL行 ${value.lineNumber}"
                    CommentType.JAVA_DOC -> "函数行 ${value.lineNumber}"
                    CommentType.KOTLIN_DOC -> "函数行 ${value.lineNumber}"
                }
                append(" ($lineInfo)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }

        // 设置列表模型
        commentList.model = DefaultListModel<Comment>().apply {
            addAll(comments)
        }

        // 设置列表选择监听器
        commentList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                commentList.selectedValue?.let { comment ->
                    navigateToComment(comment)
                }
            }
        }

        // 添加鼠标双击监听器
        commentList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    commentList.selectedValue?.let { comment ->
                        navigateAndClose(comment)
                    }
                }
            }
        })

        // 设置搜索框监听器
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_DOWN -> {
                        commentList.selectedIndex = (commentList.selectedIndex + 1).coerceAtMost(commentList.model.size - 1)
                    }
                    KeyEvent.VK_UP -> {
                        commentList.selectedIndex = (commentList.selectedIndex - 1).coerceAtLeast(0)
                    }
                    KeyEvent.VK_ENTER -> {
                        commentList.selectedValue?.let { comment ->
                            navigateAndClose(comment)
                        }
                    }
                    KeyEvent.VK_ESCAPE -> {
                        popup?.cancel()
                    }
                    else -> {
                        updateFilter(searchField.text)
                    }
                }
            }
        })
    }

    private fun navigateToComment(comment: Comment) {
        // 计算目标行的偏移量
        val document = editor.document
        val lineStartOffset = document.getLineStartOffset(comment.lineNumber - 1)
        
        // 移动光标到目标行
        editor.caretModel.moveToOffset(lineStartOffset)
        
        // 确保目标行在可见区域中心
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    private fun navigateAndClose(comment: Comment) {
        navigateToComment(comment)
        popup?.closeOk(null)
    }

    private fun updateFilter(filter: String) {
        val filteredComments = if (filter.isEmpty()) {
            comments
        } else {
            comments.filter { it.content.contains(filter, ignoreCase = true) }
        }

        (commentList.model as DefaultListModel<Comment>).apply {
            clear()
            addAll(filteredComments)
        }

        if (filteredComments.isNotEmpty()) {
            commentList.selectedIndex = 0
        }
    }

    fun show() {
        val panel = JPanel(BorderLayout()).apply {
            add(searchField, BorderLayout.NORTH)
            add(JBScrollPane(commentList), BorderLayout.CENTER)
            preferredSize = Dimension(400, 300)
        }

        // 根据文件类型设置标题
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val title = when {
            virtualFile?.name?.endsWith(".sql", ignoreCase = true) == true -> "SQL注释结构"
            virtualFile?.name?.endsWith(".java", ignoreCase = true) == true -> "Java文档结构"
            virtualFile?.name?.endsWith(".kt", ignoreCase = true) == true -> "Kotlin文档结构"
            else -> "注释结构"
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, searchField)
            .setTitle(title)
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        popup?.showInBestPositionFor(editor)
    }

    override fun dispose() {
        popup?.cancel()
        popup = null
    }
}
