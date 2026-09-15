package cn.tinyue.console.action

import cn.tinyue.console.service.JavaDocParser
import cn.tinyue.console.service.KotlinDocParser
import cn.tinyue.console.service.NavigationService
import cn.tinyue.console.service.SqlFileParser
import cn.tinyue.console.service.impl.JavaDocParserImpl
import cn.tinyue.console.service.impl.KotlinDocParserImpl
import cn.tinyue.console.service.impl.NavigationServiceImpl
import cn.tinyue.console.service.impl.SqlFileParserImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import cn.tinyue.console.outline.CommentOutlineParser
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 注释结构导航动作
 */
class CommentStructureAction : AnAction() {
    private var navigationService: NavigationService? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file != null && CommentOutlineParser().supports(file.name)) {
            ToolWindowManager.getInstance(project).getToolWindow("Comment Outline")?.activate(null)
            return
        }

        // 如果 navigationService 为空，则创建一个新的实例
        if (navigationService == null) {
            navigationService = createNavigationService(project)
        }

        // 显示导航弹窗
        (navigationService as? NavigationServiceImpl)?.showNavigationPopup(editor)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = project != null && editor != null
    }

    private fun createNavigationService(project: Project): NavigationService {
        val sqlFileParser: SqlFileParser = SqlFileParserImpl()
        val javaDocParser: JavaDocParser = JavaDocParserImpl()
        val kotlinDocParser: KotlinDocParser = KotlinDocParserImpl()
        return NavigationServiceImpl(project, sqlFileParser, javaDocParser, kotlinDocParser)
    }
} 