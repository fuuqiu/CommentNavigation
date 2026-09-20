package cn.tinyue.console.outline

import cn.tinyue.console.CommentNavigatorBundle.message
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Cursor
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

internal class ScratchFilesPanel(
    private val project: Project,
    private val onClose: () -> Unit
) : JPanel(BorderLayout(0, JBUI.scale(8))), Disposable {
    private data class Entry(val path: String, val file: VirtualFile)

    private val rootPath = ScratchFileService.getInstance().getRootPath(ScratchRootType.getInstance())
    private val search = SearchTextField(false)
    private val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode("Scratches")))
    private val status = JBLabel(message("scratch.loading"))
    private var root: VirtualFile? = null
    private var files: Map<String, VirtualFile> = emptyMap()
    private var loaded = false
    private var revision = 0
    private var suppressOpenClick = false
    private val nameField = JBTextField()
    private val createButton = JButton(message("create"))
    private val cancelButton = JButton(message("cancel"))
    private val creationPanel = JPanel(BorderLayout(0, JBUI.scale(4)))
    private var creating = false
    private var creationDirectory: VirtualFile? = null
    @Volatile private var disposed = false
    private var pendingCreation = false

    init {
        search.textEditor.emptyText.text = message("scratch.search")
        search.textEditor.accessibleContext.accessibleName = message("scratch.search.accessible")
        tree.isRootVisible = false
        tree.showsRootHandles = true
        // 列表较长时仍保留可拖回根目录的空白区域。
        tree.border = JBUI.Borders.emptyBottom(24)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.emptyText.text = message("scratch.loading")
        tree.accessibleContext.accessibleName = message("scratch.tree")
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                val entry = (value as? DefaultMutableTreeNode)?.userObject as? Entry
                append(if (entry == null || entry.path.isEmpty()) "Scratches" else entry.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                icon = if (entry == null || entry.file.isDirectory) AllIcons.Nodes.Folder else entry.file.fileType.icon
                toolTipText = entry?.file?.presentableUrl
            }
        }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) = showRenameMenu(event)
            override fun mouseReleased(event: MouseEvent) = showRenameMenu(event)

            override fun mouseClicked(event: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(event) || event.isPopupTrigger || event.isControlDown || suppressOpenClick) return
                val path = tree.getPathForLocation(event.x, event.y)
                if (path == null) {
                    tree.clearSelection()
                    return
                }
                val entry = entry(path) ?: return
                if (!entry.file.isDirectory) open(entry.file)
            }
        })
        val keys = object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                when (event.keyCode) {
                    KeyEvent.VK_F2 -> if (event.source === tree) {
                        startRename()
                        event.consume()
                    }
                    KeyEvent.VK_ENTER -> {
                        val path = tree.selectionPath ?: return
                        val file = entry(path)?.file ?: return
                        if (file.isDirectory) {
                            if (tree.isExpanded(path)) tree.collapsePath(path) else tree.expandPath(path)
                        } else open(file)
                        event.consume()
                    }
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN -> if (event.source === search.textEditor && tree.rowCount > 0) {
                        val delta = if (event.keyCode == KeyEvent.VK_UP) -1 else 1
                        val row = (tree.leadSelectionRow + delta).coerceIn(0, tree.rowCount - 1)
                        tree.setSelectionRow(row)
                        tree.scrollRowToVisible(row)
                        event.consume()
                    }
                }
            }
        }
        tree.addKeyListener(keys)
        search.textEditor.addKeyListener(keys)
        search.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) = render(selected()?.file)
        })
        tree.addTreeSelectionListener { updateControls() }
        creationPanel.add(JBLabel(message("new.rule")), BorderLayout.NORTH)
        creationPanel.add(nameField, BorderLayout.CENTER)
        creationPanel.add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), JBUI.scale(4))).apply {
            add(createButton)
            add(cancelButton)
        }, BorderLayout.SOUTH)
        creationPanel.isVisible = false
        nameField.accessibleContext.accessibleName = message("new.name")
        nameField.emptyText.text = message("new.hint")
        nameField.toolTipText = message("new.rule")
        nameField.addActionListener { if (createButton.isEnabled) create() }
        nameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) = validateCreation()
        })
        createButton.addActionListener { create() }
        cancelButton.addActionListener { cancelCreation() }
        border = JBUI.Borders.empty(8)
        minimumSize = JBUI.size(0, 0)
        status.minimumSize = JBUI.size(0, status.preferredSize.height)
        add(creationPanel, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)
        add(JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(status, BorderLayout.NORTH)
            add(search, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeScratchBrowser")
        actionMap.put("closeScratchBrowser", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent) {
                if (creating) cancelCreation() else onClose()
            }
        })
        installFileDragging()
        updateControls()
    }

    fun activate(currentFile: VirtualFile?) {
        loaded = false
        search.text = ""
        updateControls()
        refresh(currentFile)
        SwingUtilities.invokeLater { if (alive() && isShowing && !creating) search.requestFocusInWindow() }
    }

    fun deactivate() {
        pendingCreation = false
        if (creating) cancelCreation()
    }

    fun expandAll() {
        var row = 0
        while (row < tree.rowCount) tree.expandRow(row++)
    }

    fun collapseAll() {
        for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row)
    }

    private fun alive(): Boolean = !disposed && !project.isDisposed

    override fun dispose() {
        disposed = true
        files = emptyMap()
        root = null
    }

    private fun refresh(selectFile: VirtualFile? = selected()?.file) {
        val request = ++revision
        // 异步刷新目录，包含在 IDE 外新增或删除的文件，不在读锁内刷新磁盘。
        LocalFileSystem.getInstance().refreshNioFiles(listOf(Path.of(rootPath)), true, true) {
            if (!alive()) return@refreshNioFiles
            ReadAction.nonBlocking<Pair<VirtualFile?, Map<String, VirtualFile>>> {
                val directory = ScratchFileService.getInstance().getVirtualFile(ScratchRootType.getInstance())
                val result = linkedMapOf<String, VirtualFile>()
                if (directory != null) {
                    // 隐藏目录整棵子树都不进入列表和搜索，避免遍历 .git 等元数据。
                    VfsUtilCore.iterateChildrenRecursively(directory, {
                        !it.`is`(VFileProperty.SYMLINK) && (!it.isDirectory || !it.name.startsWith('.'))
                    }) { file ->
                        ProgressManager.checkCanceled()
                        if (file != directory && file.isValid && (file.isDirectory || ScratchFileRules.supports(file.name))) {
                            VfsUtilCore.getRelativePath(file, directory, '/')?.let { result[it] = file }
                        }
                        true
                    }
                }
                directory to result
            }.expireWith(this).expireWith(project)
                .finishOnUiThread(ModalityState.any()) { (directory, found) ->
                    if (!alive() || request != revision) return@finishOnUiThread
                    root = directory
                    files = found
                    loaded = true
                    render(selectFile)
                    if (pendingCreation) startCreate()
                }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    private fun render(selectFile: VirtualFile?) {
        val visible = ScratchFileRules.visiblePaths(
            files.filterValues { !it.isDirectory }.keys.toList(),
            files.filterValues { it.isDirectory }.keys.toList(), search.text
        )
        val rootNode = DefaultMutableTreeNode(root?.let { Entry("", it) } ?: "Scratches")
        val nodes = mutableMapOf("" to rootNode)
        val sorted = visible.sortedWith(compareBy<String> { it.count { char -> char == '/' } }
            .thenBy { files[it]?.isDirectory != true }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.substringAfterLast('/') }.thenBy { it })
        for (path in sorted) {
            val node = DefaultMutableTreeNode(Entry(path, files.getValue(path)))
            nodes[path] = node
            nodes[path.substringBeforeLast('/', "")]?.add(node)
        }
        tree.model = DefaultTreeModel(rootNode)
        tree.expandPath(TreePath(rootNode.path))
        var row = 0
        while (row < tree.rowCount) tree.expandRow(row++)
        val preferred = nodes.values.firstOrNull { it !== rootNode && (it.userObject as? Entry)?.file == selectFile }
            ?: if (search.text.isNotBlank()) nodes.values.firstOrNull { (it.userObject as? Entry)?.file?.isDirectory == false } else null
        tree.selectionPath = preferred?.let { TreePath(it.path) }
        tree.selectionPath?.let(tree::scrollPathToVisible)
        tree.emptyText.text = message("scratch.empty")
        updateControls()
    }

    private fun entry(path: TreePath?): Entry? = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Entry
    private fun selected(): Entry? = entry(tree.selectionPath)
    private fun selectedDirectory(): VirtualFile? = selected()?.file?.let { if (it.isDirectory) it else it.parent } ?: root

    private fun updateControls() {
        if (creating) { validateCreation(); return }
        val visibleCount = files.count { !it.value.isDirectory && ScratchFileRules.matches(it.key, search.text) }
        val target = selectedDirectory()?.let { directory -> root?.let { VfsUtilCore.getRelativePath(directory, it, '/') } }.orEmpty()
        status.text = when {
            !loaded -> message("scratch.loading")
            search.text.isNotBlank() && visibleCount == 0 -> message("scratch.empty")
            else -> message("scratch.count", visibleCount)
        }
        status.toolTipText = message("scratch.help", target.ifEmpty { "Scratches" })
    }

    private fun open(file: VirtualFile) {
        if (!file.isValid || file.isDirectory || !ScratchFileRules.supports(file.name)) {
            refresh()
            return
        }
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun canRename(file: VirtualFile): Boolean = alive() && loaded && !creating &&
        file.isValid && !file.isDirectory && file.isWritable && file.parent?.isWritable == true &&
        ScratchFileRules.supports(file.name) && root?.let { VfsUtilCore.isAncestor(it, file, true) } == true

    private fun showRenameMenu(event: MouseEvent) {
        if (!event.isPopupTrigger) return
        val path = tree.getPathForLocation(event.x, event.y) ?: return
        tree.selectionPath = path
        val file = entry(path)?.file ?: return
        if (!canRename(file)) return
        JPopupMenu().apply {
            add(JMenuItem(message("rename")).apply { addActionListener { startRename() } })
            show(tree, event.x, event.y)
        }
        event.consume()
    }

    private fun startRename() {
        val file = selected()?.file ?: return
        if (!canRename(file)) return
        val validator = object : InputValidatorEx {
            override fun getErrorText(inputString: String): String? {
                if (!canRename(file)) return message("rename.target.error")
                val name = ScratchFileRules.normalizeRenameName(inputString) ?: return message("rename.invalid")
                val existing = file.parent.findChild(name)
                return if (existing != null && existing != file) message("new.duplicate") else null
            }
            override fun checkInput(inputString: String): Boolean = getErrorText(inputString) == null
            override fun canClose(inputString: String): Boolean = checkInput(inputString)
        }
        val input = Messages.showInputDialog(project, message("rename.prompt"), message("rename"),
            null, file.name, validator) ?: return
        if (!alive()) return
        val name = ScratchFileRules.normalizeRenameName(input) ?: return
        if (file.name == name) return
        try {
            WriteCommandAction.runWriteCommandAction(project, message("rename.command"), null, Runnable {
                // 对话框关闭后再次检查目标和冲突，避免覆盖并发创建的文件。
                validator.getErrorText(name)?.let { throw IOException(it) }
                file.rename(this, name)
            })
            // 新名称不再匹配搜索时清除过滤，保留重命名后的文件选中状态。
            if (!ScratchFileRules.matches(file.name, search.text)) search.text = ""
            refresh(file)
        } catch (error: IOException) {
            status.text = message("rename.failed", error.localizedMessage.orEmpty())
        }
    }

    fun startCreate() {
        if (!loaded) {
            pendingCreation = true
            return
        }
        pendingCreation = false
        if (creating) {
            nameField.requestFocusInWindow()
            return
        }
        creationDirectory = selectedDirectory()
        creating = true
        creationPanel.isVisible = true
        search.isEnabled = false
        search.textEditor.isEnabled = false
        nameField.text = ""
        updateControls()
        revalidate()
        repaint()
        nameField.requestFocusInWindow()
    }

    private fun typeLabel(name: String): String = message(when {
        name.endsWith(".sql", ignoreCase = true) -> "type.sql"
        name.endsWith(".http", ignoreCase = true) -> "type.http"
        else -> "type.folder"
    })

    private fun validateCreation() {
        if (!creating) return
        val name = ScratchFileRules.normalizeName(nameField.text)
        val duplicate = name != null && creationDirectory?.findChild(name) != null
        createButton.isEnabled = name != null && !duplicate
        val location = creationDirectory?.let { directory -> root?.let { VfsUtilCore.getRelativePath(directory, it, '/') } }.orEmpty().ifEmpty { "Scratches" }
        status.text = when {
            duplicate -> message("new.duplicate")
            nameField.text.isBlank() -> message("new.rule")
            name == null -> message("new.invalid")
            else -> message("new.location", typeLabel(name), location)
        }
    }

    private fun cancelCreation() {
        creating = false
        creationPanel.isVisible = false
        search.isEnabled = true
        search.textEditor.isEnabled = true
        updateControls()
        revalidate()
        repaint()
        search.requestFocusInWindow()
    }

    private fun create() {
        if (!creating || !alive()) return
        val directory = creationDirectory
        val name = ScratchFileRules.normalizeName(nameField.text) ?: return
        val title = message("new.command", typeLabel(name))
        try {
            var created: VirtualFile? = null
            WriteCommandAction.runWriteCommandAction(project, title, null, Runnable {
                val target = directory ?: VfsUtil.createDirectoryIfMissing(rootPath) ?: throw IOException(message("new.root.error"))
                val scratchRoot = LocalFileSystem.getInstance().findFileByPath(rootPath)
                if (!target.isValid || scratchRoot == null || !VfsUtilCore.isAncestor(scratchRoot, target, false)) throw IOException(message("new.target.error"))
                if (target.findChild(name) != null) throw IOException(message("new.duplicate"))
                created = if (!ScratchFileRules.supports(name)) target.createChildDirectory(this, name)
                else target.createChildData(this, name).also { VfsUtil.saveText(it, ScratchFileRules.initialContent(name)) }
            })
            cancelCreation()
            created?.let {
                if (!ScratchFileRules.supports(name)) {
                    search.text = ""
                    refresh(it)
                } else open(it)
            }
        } catch (error: IOException) {
            status.text = message("new.failed", error.localizedMessage.orEmpty())
        }
    }

    private fun installFileDragging() {
        // 仅处理面板内文件到目录的拖动，保留同名冲突检查。
        val dragging = object : MouseAdapter() {
            private var source: VirtualFile? = null
            private var start: Point? = null

            override fun mousePressed(event: MouseEvent) {
                suppressOpenClick = false
                if (!SwingUtilities.isLeftMouseButton(event) || creating) return
                source = entry(tree.getPathForLocation(event.x, event.y))?.file
                    ?.takeIf { !it.isDirectory && ScratchFileRules.supports(it.name) }
                start = event.point
            }

            override fun mouseDragged(event: MouseEvent) {
                val file = source ?: return
                val origin = start ?: return
                if (!suppressOpenClick && origin.distance(event.point) < JBUI.scale(5)) return
                suppressOpenClick = true
                tree.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                tree.scrollRectToVisible(Rectangle(event.x, event.y, 1, 1))
                val path = dropPath(event.point)
                val target = dropDirectory(event.point)
                if (target != null && canMove(file, target)) {
                    tree.selectionPath = path
                    if (path != null) tree.expandPath(path)
                    status.text = message("move.release", target.name)
                } else {
                    status.text = if (target?.isDirectory == true && target.findChild(file.name) != null && file.parent != target)
                        message("move.duplicate", file.name) else message("move.choose")
                }
                event.consume()
            }

            override fun mouseReleased(event: MouseEvent) {
                val file = source
                source = null
                start = null
                tree.cursor = Cursor.getDefaultCursor()
                if (!suppressOpenClick || file == null) return
                val target = dropDirectory(event.point)
                if (target != null) move(file, target) else updateControls()
                event.consume()
            }
        }
        tree.addMouseListener(dragging)
        tree.addMouseMotionListener(dragging)
    }

    private fun dropDirectory(point: Point): VirtualFile? {
        if (!tree.visibleRect.contains(point)) return null
        val path = dropPath(point)
        if (path != null) return entry(path)?.file?.takeIf { it.isDirectory }
        // 隐藏根节点后，列表底部空白区代表 Scratches；行右侧空白不作为移动目标。
        val lastRow = tree.getRowBounds(tree.rowCount - 1)
        return root.takeIf { lastRow == null || point.y >= lastRow.y + lastRow.height }
    }

    private fun dropPath(point: Point): TreePath? {
        // 文件夹整行均可接收拖放，不要求鼠标精确落在文字上。
        val path = tree.getClosestPathForLocation(point.x, point.y) ?: return null
        val bounds = tree.getPathBounds(path) ?: return null
        return path.takeIf { point.y >= bounds.y && point.y < bounds.y + bounds.height }
    }

    private fun canMove(source: VirtualFile, target: VirtualFile): Boolean {
        val scratchRoot = root ?: return false
        return !creating && source.isValid && !source.isDirectory && ScratchFileRules.supports(source.name) &&
            target.isValid && target.isDirectory && target.isWritable && source.parent != target &&
            VfsUtilCore.isAncestor(scratchRoot, source, true) && VfsUtilCore.isAncestor(scratchRoot, target, false) &&
            target.findChild(source.name) == null
    }

    private fun move(source: VirtualFile, target: VirtualFile) {
        if (!canMove(source, target)) {
            status.text = message("move.invalid")
            return
        }
        try {
            WriteCommandAction.runWriteCommandAction(project, message("move.command"), null, Runnable {
                if (!canMove(source, target)) throw IOException(message("move.target.error"))
                source.move(this, target)
            })
            refresh(source)
        } catch (error: IOException) {
            status.text = message("move.failed", error.localizedMessage.orEmpty())
        }
    }
}
