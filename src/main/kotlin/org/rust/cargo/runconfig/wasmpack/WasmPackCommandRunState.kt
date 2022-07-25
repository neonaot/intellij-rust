/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack

import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.psi.search.ExecutionSearchScopes
import org.rust.cargo.runconfig.console.CargoConsoleView
import org.rust.cargo.toolchain.tools.WasmPack
import java.io.File

class WasmPackCommandRunState(
    environment: ExecutionEnvironment,
    runConfiguration: WasmPackCommandConfiguration,
    wasmPack: WasmPack,
    workingDirectory: File
): WasmPackCommandRunStateBase(environment, runConfiguration, wasmPack, workingDirectory) {
    init {
        val scope = ExecutionSearchScopes.executionScope(environment.project, runConfiguration)
        consoleBuilder = object : TextConsoleBuilderImpl(environment.project, scope) {
            override fun createConsole(): ConsoleView = CargoConsoleView(project, scope, isViewer, true)
        }
    }
}
