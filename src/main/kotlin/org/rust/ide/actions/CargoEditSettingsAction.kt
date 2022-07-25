/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.rust.cargo.project.configurable.CargoConfigurable
import org.rust.openapiext.showSettingsDialog

class CargoEditSettingsAction : AnAction(), DumbAware {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.showSettingsDialog<CargoConfigurable>()
    }
}
