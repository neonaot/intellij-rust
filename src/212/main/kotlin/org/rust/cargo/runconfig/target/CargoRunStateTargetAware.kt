/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.text.nullize
import org.rust.cargo.runconfig.CargoRunStateBase
import org.rust.cargo.runconfig.buildtool.CargoPatch
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import kotlin.reflect.KClass

@Suppress("UnstableApiUsage")
abstract class CargoRunStateTargetAware(
    environment: ExecutionEnvironment,
    runConfiguration: CargoCommandConfiguration,
    config: CargoCommandConfiguration.CleanConfiguration.Ok
) : CargoRunStateBase(environment, runConfiguration, config) {

    override fun startProcess(processColors: Boolean): ProcessHandler {
        val targetEnvironment = runConfiguration.targetEnvironment ?: return super.startProcess(processColors)

        val remoteRunPatch: CargoPatch = { commandLine ->
            if (runConfiguration.buildOnRemoteTarget && sshEnvironmentClass?.isInstance(targetEnvironment) == true) {
                commandLine.prependArgument("--target-dir=${targetEnvironment.projectRootOnTarget}/target")
            } else {
                commandLine
            }
        }

        val commandLine = cargo().toColoredCommandLine(project, prepareCommandLine(remoteRunPatch))
        commandLine.exePath = targetEnvironment.languageRuntime?.cargoPath.nullize(true) ?: "cargo"
        return commandLine.startProcess(project, targetEnvironment, processColors, uploadExecutable = false)
    }

    companion object {
        private val sshEnvironmentClass: KClass<*>? by lazy {
            try {
                Class.forName("com.jetbrains.plugins.remotesdk.target.ssh.target.SshTargetEnvironmentConfiguration").kotlin
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }
}
