/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.rust.cargo.runconfig.RsCommandConfiguration

abstract class CargoCommandConfigurationTargetAware(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : RsCommandConfiguration(project, name, factory) {
    var defaultTargetName: String? = null
}
