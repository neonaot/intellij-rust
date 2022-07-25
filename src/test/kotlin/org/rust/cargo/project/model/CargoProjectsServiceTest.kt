/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model

import com.intellij.testFramework.LightPlatformTestCase
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl
import org.rust.openapiext.elementFromXmlString
import org.rust.openapiext.toXmlString
import java.nio.file.Paths

class CargoProjectsServiceTest : LightPlatformTestCase() {
    fun `test serialization`() {
        val service = CargoProjectsServiceImpl(project)
        val text = """
            <state>
              <cargoProject FILE="/foo" />
              <cargoProject FILE="/bar" />
            </state>
        """
        service.loadState(elementFromXmlString(text))
        val actual = service.state.toXmlString()
        assertEquals(text.trimIndent(), actual)
        val projects = service.allProjects.sortedBy { it.manifest }.map { it.manifest }
        val expectedProjects = listOf(Paths.get("/bar"), Paths.get("/foo"))
        assertEquals(expectedProjects, projects)
    }
}
