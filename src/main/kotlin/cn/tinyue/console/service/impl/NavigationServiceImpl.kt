package cn.tinyue.console.service.impl

import cn.tinyue.console.CommentNavigatorBundle.message
import cn.tinyue.console.model.Comment
import cn.tinyue.console.service.JavaDocParser
import cn.tinyue.console.service.KotlinDocParser
import cn.tinyue.console.service.NavigationService
import cn.tinyue.console.service.SqlFileParser
import cn.tinyue.console.ui.CommentNavigationPopup
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Alarm

/**
 * 导航服务实现类
 */
class NavigationServiceImpl(
    private val project: Project,
    private val sqlFileParser: SqlFileParser,
    private val javaDocParser: JavaDocParser,
    private val kotlinDocParser: KotlinDocParser
) : NavigationService, Disposable {

    private var popup: CommentNavigationPopup? = null
    private var highlighter: RangeHighlighter? = null
    private val alarm = Alarm()
    private var comments: List<Comment> = emptyList()

    override fun navigateToComment(comment: Comment) {
        // 获取当前活动编辑器
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        
        // 计算目标行的偏移量
        val document = editor.document
        val lineStartOffset = document.getLineStartOffset(comment.lineNumber - 1)
        
        // 移动光标到目标行
        editor.caretModel.moveToOffset(lineStartOffset)
        
        // 确保目标行在可见区域中心
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        
        // 高亮显示目标行
        highlightLine(editor, comment.lineNumber)
        
        // 将焦点设置到编辑器
        WindowManager.getInstance().getStatusBar(project)?.info = message("navigation.done", comment.content)
        
        // 激活编辑器窗口
        FileEditorManager.getInstance(project).selectedTextEditor?.component?.requestFocus()
    }

    override fun getComments(): List<Comment> = comments

    fun showNavigationPopup(editor: Editor) {
        comments = updateComments(editor)
        if (comments.isEmpty()) {
            return
        }

        popup = CommentNavigationPopup(project, editor, comments)
        popup?.show()
    }

    private fun updateComments(editor: Editor): List<Comment> {
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val fileName = virtualFile?.name ?: ""
        return when {
            fileName.endsWith(".sql") -> sqlFileParser.parseComments(editor.document)
            fileName.endsWith(".java") -> javaDocParser.parseComments(editor.document)
            fileName.endsWith(".kt") -> kotlinDocParser.parseComments(editor.document)
            else -> emptyList()
        }
    }

    private fun highlightLine(editor: Editor, lineNumber: Int) {
        highlighter?.let {
            editor.markupModel.removeHighlighter(it)
        }

        val document = editor.document
        val startOffset = document.getLineStartOffset(lineNumber - 1)
        val endOffset = document.getLineEndOffset(lineNumber - 1)

        highlighter = editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION,
            editor.colorsScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES),
            HighlighterTargetArea.LINES_IN_RANGE
        )

        alarm.cancelAllRequests()
        alarm.addRequest({
            highlighter?.let {
                editor.markupModel.removeHighlighter(it)
            }
            highlighter = null
        }, 2000)
    }

    override fun dispose() {
        popup?.let {
            Disposer.dispose(it)
        }
        alarm.dispose()
    }
}
