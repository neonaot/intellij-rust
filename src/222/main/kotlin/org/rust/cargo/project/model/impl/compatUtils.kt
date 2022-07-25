/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.project.Project
import org.rust.cargo.project.settings.RustProjectSettingsService
import org.rust.cargo.project.settings.RustProjectSettingsService.RustSettingsChangedEvent
import org.rust.cargo.project.settings.RustProjectSettingsService.RustSettingsListener

fun registerProjectAware(project: Project, disposable: Disposable) {
    val cargoProjectAware = CargoExternalSystemProjectAware(project)
    val projectTracker = ExternalSystemProjectTracker.getInstance(project)
    projectTracker.register(cargoProjectAware, disposable)
    projectTracker.activate(cargoProjectAware.projectId)

    project.messageBus.connect(disposable)
        .subscribe(RustProjectSettingsService.RUST_SETTINGS_TOPIC, object : RustSettingsListener {
            override fun rustSettingsChanged(e: RustSettingsChangedEvent) {
                if (e.affectsCargoMetadata) {
                    AutoImportProjectTracker.getInstance(project).markDirty(cargoProjectAware.projectId)
                }
            }
        })
}
