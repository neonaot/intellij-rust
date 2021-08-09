/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

@file:Suppress("unused", "UNUSED_PARAMETER")

package org.rust.cargo.runconfig.target

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.project.Project
import org.rust.cargo.runconfig.RsProcessHandler
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

val CargoCommandConfiguration.targetEnvironment: TargetEnvironmentConfiguration?
    get() = null

val CargoCommandConfiguration.localBuildArgsForRemoteRun: List<String>
    get() = emptyList()

fun GeneralCommandLine.startProcess(
    project: Project,
    config: TargetEnvironmentConfiguration?,
    processColors: Boolean,
    uploadExecutable: Boolean
): ProcessHandler {
    val handler = RsProcessHandler(this)
    ProcessTerminatedListener.attach(handler)
    return handler
}
