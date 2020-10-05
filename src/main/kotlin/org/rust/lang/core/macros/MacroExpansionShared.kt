/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon
import com.intellij.psi.stubs.*
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.RsMacro
import org.rust.lang.core.psi.RsMacroCall
import org.rust.lang.core.psi.ext.bodyHash
import org.rust.lang.core.stubs.RsFileStub
import org.rust.stdext.HashCode
import org.rust.stdext.readVarInt
import org.rust.stdext.writeVarInt
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Path
import java.util.*

@Suppress("UnstableApiUsage")
@Service
class MacroExpansionShared : Disposable {
    private val expansions: PersistentHashMap<HashCode, ExpansionResult> = persistentHashMap(
        getBaseMacroDir().resolve("cache").resolve("expansion-cache"),
        HashCodeKeyDescriptor,
        ExpansionResultExternalizer,
        1 * 1024 * 1024,
        MacroExpander.EXPANDER_VERSION
    )

    private val sm: SerializationManagerEx = SerializationManagerEx.getInstanceEx() // SerializationManagerImpl(getBaseMacroDir().resolve("expansion-stubs-cache.names"), true)
    private val ex: StubForwardIndexExternalizer<*> = StubForwardIndexExternalizer.createFileLocalExternalizer()

    private val stubs: PersistentHashMap<HashCode, SerializedStubTree> = persistentHashMap(
        getBaseMacroDir().resolve("cache").resolve("expansion-stubs-cache"),
        HashCodeKeyDescriptor,
        SerializedStubTreeDataExternalizer(
            /* includeInputs = */ true,
            sm,
            ex
        ),
        1 * 1024 * 1024,
        MacroExpander.EXPANDER_VERSION + RsFileStub.Type.stubVersion
    )

    private val flusher = FlushingDaemon.everyFiveSeconds {
        expansions.force()
        stubs.force()
    }

    override fun dispose() {
        flusher.cancel(false)
        expansions.close()
        stubs.close()
    }

    fun cachedExpand(expander: MacroExpander, defData: RsMacroDataWithHash, call: RsMacroCall): ExpansionResult? {
        val callData = RsMacroCallDataWithHash(call)
        return cachedExpand(expander, defData, callData)
    }

    private fun cachedExpand(
        expander: MacroExpander,
        def: RsMacroDataWithHash,
        call: RsMacroCallDataWithHash
    ): ExpansionResult? {
        val hash = HashCode.mix(def.bodyHash ?: return null, call.bodyHash ?: return null)
        return cachedExpand(expander, def, call, hash)
    }

    /** [hash] passed as optimization for [createExpansionStub] */
    fun cachedExpand(
        expander: MacroExpander,
        def: RsMacroDataWithHash,
        call: RsMacroCallDataWithHash,
        hash: HashCode
    ): ExpansionResult? {
        val cached: ExpansionResult? = expansions.get(hash)
        return if (cached == null) {
            val (text, ranges) = expander.expandMacroAsText(def.data, call.data) ?: return null
            val result = ExpansionResult(text.toString(), ranges)
            expansions.put(hash, result)
            result
        } else {
            cached
        }
    }

    fun cachedBuildStub(fileContent: FileContent, hash: HashCode): SerializedStubTree? {
        return cachedBuildStub(hash) { fileContent }
    }

    private fun cachedBuildStub(hash: HashCode, fileContent: () -> FileContent?): SerializedStubTree? {
        val cached: SerializedStubTree? = stubs.get(hash)
        return if (cached == null) {
            val stub = StubTreeBuilder.buildStubTree(fileContent() ?: return null) ?: return null
            val serializedStubTree = SerializedStubTree.serializeStub(
                stub,
                sm,
                ex
            )
            stubs.put(hash, serializedStubTree)
            serializedStubTree
        } else {
            cached
        }
    }

    fun createExpansionStub(
        project: Project,
        expander: MacroExpander,
        def: RsMacroDataWithHash,
        call: RsMacroCallDataWithHash
    ): Pair<RsFileStub, ExpansionResult>? {
        val hash = HashCode.mix(def.bodyHash ?: return null, call.bodyHash ?: return null)
        val result = cachedExpand(expander, def, call, hash) ?: return null
        val file = ReadOnlyLightVirtualFile("macro.rs", RsLanguage, result.text).apply {
            charset = Charsets.UTF_8
        }
        val stub = cachedBuildStub(hash) {
            FileContentImpl(file, result.text, file.modificationStamp).also { it.project = project }
        } ?: return null
        return Pair(stub.stub as RsFileStub, result)
    }

    companion object {
        fun getInstance(): MacroExpansionShared = service()

        private fun <K, V> persistentHashMap(
            file: Path,
            keyDescriptor: KeyDescriptor<K>,
            valueExternalizer: DataExternalizer<V>,
            initialSize: Int,
            version: Int
        ): PersistentHashMap<K, V> {
            return IOUtil.openCleanOrResetBroken({
                PersistentHashMap(
                    file,
                    keyDescriptor,
                    valueExternalizer,
                    initialSize,
                    version
                )
            }, file)
        }
    }
}

class RsMacroDataWithHash(val data: RsMacroData, val bodyHash: HashCode?) {
    constructor(def: RsMacro) : this(RsMacroData(def), def.bodyHash)
}

class RsMacroCallDataWithHash(val data: RsMacroCallData, val bodyHash: HashCode?) {
    constructor(call: RsMacroCall) : this(RsMacroCallData(call), call.bodyHash)
}

object HashCodeKeyDescriptor : KeyDescriptor<HashCode> {
    override fun getHashCode(value: HashCode): Int {
        return value.hashCode()
    }

    override fun isEqual(val1: HashCode?, val2: HashCode?): Boolean {
        return Objects.equals(val1, val2)
    }

    override fun save(out: DataOutput, value: HashCode) {
        out.write(value.toByteArray())
    }

    override fun read(inp: DataInput): HashCode {
        val bytes = ByteArray(HashCode.ARRAY_LEN)
        inp.readFully(bytes)
        return HashCode.fromByteArray(bytes)
    }
}

class ExpansionResult(
    val text: String,
    val ranges: RangeMap,
    /** Optimization: occurrences of [MACRO_DOLLAR_CRATE_IDENTIFIER] */
    val dollarCrateOccurrences: IntArray = MACRO_DOLLAR_CRATE_IDENTIFIER_REGEX.findAll(text)
        .map { it.range.first }
        .toList()
        .toIntArray()
)

object ExpansionResultExternalizer : DataExternalizer<ExpansionResult> {
    override fun save(out: DataOutput, value: ExpansionResult) {
        IOUtil.writeUTF(out, value.text)
        value.ranges.writeTo(out)
        out.writeIntArray(value.dollarCrateOccurrences)
    }

    override fun read(inp: DataInput): ExpansionResult {
        return ExpansionResult(
            IOUtil.readUTF(inp),
            RangeMap.readFrom(inp),
            inp.readIntArray()
        )
    }
}

private fun DataOutput.writeIntArray(array: IntArray) {
    writeInt(array.size)
    for (element in array) {
        writeVarInt(element)
    }
}

private fun DataInput.readIntArray(): IntArray {
    val size = readInt()
    val array = IntArray(size)
    for (i in 0 until size) {
        array[i] = readVarInt()
    }
    return array
}
