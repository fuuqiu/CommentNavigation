package cn.tinyue.console.outline

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CommentOutlinePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode()))
    private val parser = CommentOutlineParser()
    private val caption = JBLabel("Comment Outline")
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var currentDocument: Document? = null
    private var renderedStamp = -1L
    private var revision = 0
    @Volatile private var disposed = false

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.emptyText.text = "打开 SQL / HTTP 文件以查看注释大纲"
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                val heading = (value as? DefaultMutableTreeNode)?.userObject as? OutlineHeading ?: return
                append(heading.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append("  ${heading.lineNumber}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
        TreeSpeedSearch.installOn(tree, true) { path -> heading(path)?.title.orEmpty() }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(event)) return
                val path = tree.getPathForLocation(event.x, event.y) ?: return
                navigate(path)
            }
        })
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_ENTER) {
                    tree.selectionPath?.let(::navigate)
                    event.consume()
                }
            }
        })
        val actions = DefaultActionGroup().apply {
            add(object : DumbAwareAction("全部展开", "展开所有注释标题", AllIcons.Actions.Expandall) {
                override fun actionPerformed(e: AnActionEvent) = expandAll()
            })
            add(object : DumbAwareAction("全部折叠", "折叠所有注释标题", AllIcons.Actions.Collapseall) {
                override fun actionPerformed(e: AnActionEvent) {
                    for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row)
                }
            })
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("CommentOutline", actions, true)
        toolbar.targetComponent = tree
        add(JPanel(BorderLayout()).apply {
            add(caption, BorderLayout.CENTER)
            add(toolbar.component, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)
        project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) = queueRefresh(0)
        })
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (event.document === currentDocument) queueRefresh(250)
            }
        }, this)
        queueRefresh(0)
    }

    private fun queueRefresh(delay: Int) {
        if (disposed) return
        alarm.cancelAllRequests()
        alarm.addRequest({ refresh() }, delay)
    }

    private fun refresh() {
        if (disposed || project.isDisposed) return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val document = editor?.document
        val file = document?.let { FileDocumentManager.getInstance().getFile(it) }
        val changedFile = document !== currentDocument
        currentDocument = document
        val request = ++revision
        if (changedFile || document == null || file == null || !parser.supports(file.name)) {
            tree.model = DefaultTreeModel(DefaultMutableTreeNode())
            renderedStamp = -1
        }
        if (document == null || file == null || !parser.supports(file.name)) {
            caption.text = "Comment Outline"
            tree.emptyText.text = "打开 SQL / HTTP 文件以查看注释大纲"
            return
        }
        caption.text = file.name
        caption.toolTipText = file.presentableUrl
        tree.emptyText.text = "正在读取注释标题…"
        ReadAction.nonBlocking<Pair<Long, List<OutlineHeading>>> {
            document.modificationStamp to parser.parse(document.immutableCharSequence.toString(), file.name)
        }.expireWith(this).coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { (stamp, headings) ->
                if (request != revision || document !== currentDocument || stamp != document.modificationStamp) return@finishOnUiThread
                render(headings, changedFile)
                renderedStamp = stamp
            }.submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun render(headings: List<OutlineHeading>, changedFile: Boolean) {
        val oldRoot = tree.model.root as DefaultMutableTreeNode
        val collapsed = if (changedFile) emptySet() else oldRoot.depthFirstEnumeration().toList()
            .filterIsInstance<DefaultMutableTreeNode>()
            .filter { !it.isLeaf && !tree.isExpanded(TreePath(it.path)) }
            .map { key(TreePath(it.path)) }.toSet()
        val selected = if (changedFile) null else tree.selectionPath?.let(::key)
        fun toSwing(node: OutlineNode): DefaultMutableTreeNode = DefaultMutableTreeNode(node.heading).apply {
            node.children.forEach { add(toSwing(it)) }
        }
        val root = DefaultMutableTreeNode().apply { parser.buildTree(headings).forEach { add(toSwing(it)) } }
        tree.model = DefaultTreeModel(root)
        expandAll()
        for (node in root.depthFirstEnumeration().toList().filterIsInstance<DefaultMutableTreeNode>()) {
            val path = TreePath(node.path)
            if (key(path) in collapsed) tree.collapsePath(path)
            if (key(path) == selected) tree.selectionPath = path
        }
        tree.emptyText.text = "暂无标题：SQL 使用 -- # 标题；HTTP 使用 // # 标题"
    }

    // 同名标题按兄弟节点中的出现次序区分，插入正文不影响折叠状态。
    private fun key(path: TreePath): List<Pair<String, Int>> = path.path.filterIsInstance<DefaultMutableTreeNode>().map { node ->
        val title = (node.userObject as? OutlineHeading)?.title.orEmpty()
        val siblings = (node.parent as? DefaultMutableTreeNode)?.children()?.toList().orEmpty()
        title to siblings.takeWhile { it !== node }.count { ((it as DefaultMutableTreeNode).userObject as? OutlineHeading)?.title == title }
    }

    private fun heading(path: TreePath): OutlineHeading? = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? OutlineHeading

    private fun navigate(path: TreePath) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (editor.document !== currentDocument || editor.document.modificationStamp != renderedStamp) {
            queueRefresh(0)
            return
        }
        val heading = heading(path) ?: return
        editor.caretModel.moveToOffset(editor.document.getLineStartOffset(heading.lineNumber - 1))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        editor.contentComponent.requestFocusInWindow()
    }

    private fun expandAll() {
        var row = 0
        while (row < tree.rowCount) tree.expandRow(row++)
    }

    override fun dispose() {
        disposed = true
        currentDocument = null
    }
}
