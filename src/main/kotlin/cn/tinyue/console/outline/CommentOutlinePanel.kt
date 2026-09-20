package cn.tinyue.console.outline

import cn.tinyue.console.CommentNavigatorBundle.message
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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CommentOutlinePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode()))
    private val parser = CommentOutlineParser()
    private val captionText = JBLabel(message("outline.no.file"))
    private val caption = JButton()
    private val outlineSearch = SearchTextField(false)
    private val captionCards = CardLayout()
    private val captionPanel = JPanel(captionCards)
    private var headings: List<OutlineHeading> = emptyList()
    private val body = OnePixelSplitter(true, 0.6f)
    private val scratchFiles = ScratchFilesPanel(project) { hideScratchFiles() }
    private val scratchToggle = JButton("Scratches", AllIcons.General.ArrowRight)
    private val scratchSection = JPanel(BorderLayout()).apply {
        add(scratchFiles, BorderLayout.CENTER)
    }
    private var browsingFiles = false
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var currentDocument: Document? = null
    private var renderedStamp = -1L
    private var revision = 0
    @Volatile private var disposed = false

    init {
        caption.layout = BorderLayout(JBUI.scale(8), 0)
        caption.isContentAreaFilled = false
        caption.isBorderPainted = false
        caption.border = JBUI.Borders.empty(3, 8)
        captionText.foreground = UIUtil.getContextHelpForeground()
        captionText.minimumSize = Dimension(0, captionText.preferredSize.height)
        caption.add(captionText, BorderLayout.CENTER)
        caption.toolTipText = message("outline.search")
        caption.accessibleContext.accessibleName = message("outline.search")
        caption.minimumSize = Dimension(0, caption.preferredSize.height)
        caption.addActionListener {
            captionCards.show(captionPanel, "search")
            outlineSearch.requestFocusInWindow()
        }
        captionPanel.add(caption, "caption")
        captionPanel.add(outlineSearch, "search")
        outlineSearch.textEditor.emptyText.text = message("outline.search")
        outlineSearch.textEditor.accessibleContext.accessibleName = message("outline.search")
        outlineSearch.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(event: javax.swing.event.DocumentEvent) = render(headings, true)
        })
        outlineSearch.textEditor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                if (outlineSearch.text.isBlank()) captionCards.show(captionPanel, "caption")
            }
        })
        outlineSearch.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                when (event.keyCode) {
                    KeyEvent.VK_ESCAPE -> {
                        outlineSearch.text = ""
                        captionCards.show(captionPanel, "caption")
                        caption.requestFocusInWindow()
                    }
                    KeyEvent.VK_ENTER -> tree.selectionPath?.let(::navigate)
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN -> {
                        if (tree.rowCount == 0) return
                        val delta = if (event.keyCode == KeyEvent.VK_UP) -1 else 1
                        val row = (tree.leadSelectionRow + delta).coerceIn(0, tree.rowCount - 1)
                        tree.setSelectionRow(row)
                        tree.scrollRowToVisible(row)
                    }
                    else -> return
                }
                event.consume()
            }
        })
        Disposer.register(this, scratchFiles)
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.emptyText.text = message("outline.open")
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
            add(object : DumbAwareAction(message("expand"), message("expand.description"), AllIcons.Actions.Expandall) {
                override fun actionPerformed(e: AnActionEvent) {
                    expandAll()
                }
            })
            add(object : DumbAwareAction(message("collapse"), message("collapse.description"), AllIcons.Actions.Collapseall) {
                override fun actionPerformed(e: AnActionEvent) {
                    for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row)
                }
            })
        }
        val scratchActions = DefaultActionGroup().apply {
            add(object : DumbAwareAction(message("expand"), message("expand.description"), AllIcons.Actions.Expandall) {
                override fun actionPerformed(e: AnActionEvent) = scratchFiles.expandAll()
            })
            add(object : DumbAwareAction(message("collapse"), message("collapse.description"), AllIcons.Actions.Collapseall) {
                override fun actionPerformed(e: AnActionEvent) = scratchFiles.collapseAll()
            })
        }
        scratchActions.add(object : DumbAwareAction(message("new"), message("new.description"), AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                if (!browsingFiles) showScratchFiles()
                scratchFiles.startCreate()
            }
        })
        val toolbar = ActionManager.getInstance().createActionToolbar("CommentOutline", actions, true)
        toolbar.targetComponent = this
        add(JPanel(BorderLayout()).apply {
            add(captionPanel, BorderLayout.CENTER)
            add(toolbar.component, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        scratchToggle.horizontalAlignment = JButton.LEFT
        scratchToggle.isContentAreaFilled = false
        scratchToggle.isBorderPainted = false
        scratchToggle.border = JBUI.Borders.empty(6, 8)
        scratchToggle.toolTipText = message("scratch.toggle")
        scratchToggle.accessibleContext.accessibleName = message("scratch.toggle")
        scratchToggle.addActionListener { if (browsingFiles) hideScratchFiles() else showScratchFiles() }
        val scratchToolbar = ActionManager.getInstance().createActionToolbar("CommentOutlineScratches", scratchActions, true)
        scratchToolbar.targetComponent = scratchSection
        scratchSection.add(JPanel(BorderLayout()).apply {
            add(scratchToggle, BorderLayout.CENTER)
            add(scratchToolbar.component, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        body.firstComponent = JBScrollPane(tree).apply { minimumSize = JBUI.size(0, 80) }
        scratchFiles.isVisible = false
        add(scratchSection, BorderLayout.SOUTH)
        add(body, BorderLayout.CENTER)
        project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) = queueRefresh(0)
        })
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (event.document === currentDocument) queueRefresh(250)
            }
        }, this)
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val file = currentDocument?.let { FileDocumentManager.getInstance().getFile(it) } ?: return
                if (events.any { it is VFilePropertyChangeEvent && it.file == file && it.propertyName == VirtualFile.PROP_NAME }) {
                    queueRefresh(0)
                }
            }
        })
        queueRefresh(0)
    }

    private fun showScratchFiles() {
        browsingFiles = true
        scratchToggle.icon = AllIcons.General.ArrowDown
        remove(scratchSection)
        scratchFiles.isVisible = true
        scratchSection.minimumSize = JBUI.size(0, 120)
        body.secondComponent = scratchSection
        revalidate()
        repaint()
        val file = currentDocument?.let { FileDocumentManager.getInstance().getFile(it) }
        scratchFiles.activate(file)
    }

    private fun hideScratchFiles() {
        scratchFiles.deactivate()
        browsingFiles = false
        scratchToggle.icon = AllIcons.General.ArrowRight
        body.secondComponent = null
        scratchFiles.isVisible = false
        scratchSection.minimumSize = JBUI.size(0, 0)
        add(scratchSection, BorderLayout.SOUTH)
        revalidate()
        repaint()
        scratchToggle.requestFocusInWindow()
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
            headings = emptyList()
            outlineSearch.text = ""
            captionCards.show(captionPanel, "caption")
            tree.model = DefaultTreeModel(DefaultMutableTreeNode())
            renderedStamp = -1
        }
        if (document == null || file == null || !parser.supports(file.name)) {
            captionText.text = file?.name ?: message("outline.no.file")
            caption.isEnabled = false
            caption.toolTipText = message("outline.open")
            tree.emptyText.text = message("outline.open")
            return
        }
        captionText.text = file.name
        caption.isEnabled = true
        caption.toolTipText = message("outline.file.tooltip", file.presentableUrl)
        tree.emptyText.text = message("outline.loading")
        ReadAction.nonBlocking<Pair<Long, List<OutlineHeading>>> {
            document.modificationStamp to parser.parse(document.immutableCharSequence.toString(), file.name)
        }.expireWith(this).coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { (stamp, headings) ->
                if (request != revision || document !== currentDocument || stamp != document.modificationStamp) return@finishOnUiThread
                this.headings = headings
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
        val query = outlineSearch.text.trim()
        val root = DefaultMutableTreeNode().apply { OutlineSearch.filter(parser.buildTree(headings), query).forEach { add(toSwing(it)) } }
        tree.model = DefaultTreeModel(root)
        expandAll()
        for (node in root.depthFirstEnumeration().toList().filterIsInstance<DefaultMutableTreeNode>()) {
            val path = TreePath(node.path)
            if (query.isEmpty() && key(path) in collapsed) tree.collapsePath(path)
            if (key(path) == selected) tree.selectionPath = path
        }
        if (query.isNotEmpty() && tree.selectionPath == null) {
            val match = root.preorderEnumeration().toList().filterIsInstance<DefaultMutableTreeNode>()
                .firstOrNull { (it.userObject as? OutlineHeading)?.title?.contains(query, ignoreCase = true) == true }
            tree.selectionPath = match?.let { TreePath(it.path) }
        }
        tree.emptyText.text = message(if (query.isEmpty()) "outline.empty" else "outline.no.matches")
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
