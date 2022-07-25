/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow

import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.ColorUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.CargoProjectsService
import org.rust.cargo.project.model.CargoProjectsService.CargoProjectsListener
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.model.guessAndSetupRustProject
import org.rust.cargo.runconfig.hasCargoProject
import javax.swing.JComponent
import javax.swing.JEditorPane

class CargoToolWindowFactory : ToolWindowFactory, DumbAware {
    private val lock: Any = Any()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        guessAndSetupRustProject(project)
        val toolwindowPanel = CargoToolWindowPanel(project)
        // BACKCOMPAT: 2022.1
        @Suppress("DEPRECATION")
        val tab = ContentFactory.SERVICE.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)
    }

    override fun isApplicable(project: Project): Boolean {
        if (CargoToolWindow.isRegistered(project)) return false

        val cargoProjects = project.cargoProjects
        if (!cargoProjects.hasAtLeastOneValidProject
            && cargoProjects.suggestManifests().none()) return false

        synchronized(lock) {
            val res = project.getUserData(CARGO_TOOL_WINDOW_APPLICABLE) ?: true
            if (res) {
                project.putUserData(CARGO_TOOL_WINDOW_APPLICABLE, false)
            }
            return res
        }
    }

    companion object {
        private val CARGO_TOOL_WINDOW_APPLICABLE: Key<Boolean> = Key.create("CARGO_TOOL_WINDOW_APPLICABLE")
    }
}

private class CargoToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, false) {
    private val cargoTab = CargoToolWindow(project)

    init {
        toolbar = cargoTab.toolbar.component
        cargoTab.toolbar.targetComponent = this
        setContent(cargoTab.content)
    }

    override fun getData(dataId: String): Any? =
        when {
            CargoToolWindow.SELECTED_CARGO_PROJECT.`is`(dataId) -> cargoTab.selectedProject
            PlatformDataKeys.TREE_EXPANDER.`is`(dataId) -> cargoTab.treeExpander
            else -> super.getData(dataId)
        }
}

class CargoToolWindow(
    private val project: Project
) {
    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(CARGO_TOOLBAR_PLACE, actionManager.getAction("Rust.Cargo") as DefaultActionGroup, true)
    }

    val note = JEditorPane("text/html", html("")).apply {
        background = UIUtil.getTreeBackground()
        isEditable = false
    }

    private val projectTree = CargoProjectsTree()
    private val projectStructure = CargoProjectTreeStructure(projectTree, project)

    val treeExpander: TreeExpander = object : DefaultTreeExpander(projectTree) {
        override fun isCollapseAllVisible(): Boolean = project.hasCargoProject
        override fun isExpandAllVisible(): Boolean = project.hasCargoProject
    }

    val selectedProject: CargoProject? get() = projectTree.selectedProject

    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, 0)

    init {
        with(project.messageBus.connect()) {
            subscribe(CargoProjectsService.CARGO_PROJECTS_TOPIC, CargoProjectsListener { _, projects ->
                invokeLater {
                    projectStructure.updateCargoProjects(projects.toList())
                }
            })
        }

        invokeLater {
            projectStructure.updateCargoProjects(project.cargoProjects.allProjects.toList())
        }
    }

    private fun html(body: String): String = """
        <html>
        <head>
            ${UIUtil.getCssFontDeclaration(UIUtil.getLabelFont())}
            <style>body {background: #${ColorUtil.toHex(UIUtil.getTreeBackground())}; text-align: center; }</style>
        </head>
        <body>
            $body
        </body>
        </html>
    """

    companion object {
        private val LOG: Logger = logger<CargoToolWindow>()

        @JvmStatic
        val SELECTED_CARGO_PROJECT: DataKey<CargoProject> = DataKey.create("SELECTED_CARGO_PROJECT")

        const val CARGO_TOOLBAR_PLACE: String = "Cargo Toolbar"

        private const val ID: String = "Cargo"

        fun initializeToolWindow(project: Project) {
            try {
                val manager = ToolWindowManager.getInstance(project) as? ToolWindowManagerEx ?: return
                val bean = ToolWindowEP.EP_NAME.extensionList.find { it.id == ID }
                if (bean != null) {
                    @Suppress("DEPRECATION", "UnstableApiUsage")
                    manager.initToolWindow(bean)
                }
            } catch (e: Exception) {
                LOG.error("Unable to initialize $ID tool window", e)
            }
        }

        fun isRegistered(project: Project): Boolean {
            val manager = ToolWindowManager.getInstance(project)
            return manager.getToolWindow(ID) != null
        }
    }
}
