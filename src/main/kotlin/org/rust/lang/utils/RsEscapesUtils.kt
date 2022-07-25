/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN
import org.rust.lang.core.lexer.RsEscapesLexer
import org.rust.lang.core.lexer.parseStringCharacters
import org.rust.lang.core.lexer.tokenize

/**
 * Unescape string escaped using Rust escaping rules.
 */
fun String.unescapeRust(unicode: Boolean = true, eol: Boolean = true, extendedByte: Boolean = true): String =
    this.unescapeRust(RsEscapesLexer.dummy(unicode, eol, extendedByte))

/**
 * Unescape string escaped using Rust escaping rules.
 */
fun String.unescapeRust(escapesLexer: RsEscapesLexer): String =
    this.tokenize(escapesLexer)
        .joinToString(separator = "") {
            val (type, text) = it
            when (type) {
                VALID_STRING_ESCAPE_TOKEN -> decodeEscape(text)
                else -> text
            }
        }

fun parseRustStringCharacters(chars: String): Triple<StringBuilder, IntArray, Boolean> {
    val outChars = StringBuilder()
    val (offsets, success) = parseRustStringCharacters(chars, outChars)
    return Triple(outChars, offsets, success)
}

fun parseRustStringCharacters(chars: String, outChars: StringBuilder): Pair<IntArray, Boolean> {
    val sourceOffsets = IntArray(chars.length + 1)
    val result = parseRustStringCharacters(chars, outChars, sourceOffsets)
    return sourceOffsets to result
}

private fun parseRustStringCharacters(chars: String, outChars: StringBuilder, sourceOffsets: IntArray): Boolean {
    return parseStringCharacters(RsEscapesLexer.dummy(), chars, outChars, sourceOffsets, ::decodeEscape)
}

private fun decodeEscape(esc: String): String = when (esc) {
    "\\n" -> "\n"
    "\\r" -> "\r"
    "\\t" -> "\t"
    "\\0" -> "\u0000"
    "\\\\" -> "\\"
    "\\'" -> "\'"
    "\\\"" -> "\""

    else -> {
        assert(esc.length >= 2)
        assert(esc[0] == '\\')
        when (esc[1]) {
            'x' -> Integer.parseInt(esc.substring(2), 16).toChar().toString()
            'u' -> Integer.parseInt(esc.substring(3, esc.length - 1).filter { it != '_' }, 16).toChar().toString()
            '\r', '\n' -> ""
            else -> error("unreachable")
        }
    }
}

fun CharSequence.escapeRust(escapeNonPrintable: Boolean = true): String {
    val sb = StringBuilder(length)
    escapeRust(sb, escapeNonPrintable)
    return sb.toString()
}

fun CharSequence.escapeRust(out: StringBuilder, escapeNonPrintable: Boolean = true) {
    for (c in this) {
        when {
            c == '\n' -> out.append("\\n")
            c == '\r' -> out.append("\\r")
            c == '\t' -> out.append("\\t")
            c == '\u0000' -> out.append("\\0")
            c == '\'' -> out.append("\\'")
            c == '\"' -> out.append("\\\"")
            escapeNonPrintable && !StringUtil.isPrintableUnicode(c) -> {
                out.append("\\u{")
                out.append(c.code)
                out.append("}")
            }
            else -> out.append(c)
        }
    }
}
