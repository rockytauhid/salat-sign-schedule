// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

/**
 * Lightweight, zero-dependency JSON serializer and parser for the local LAN controller API.
 * Works identically on Android runtime (API 26+) and pure JVM unit tests.
 */
object SimpleJson {

    fun escape(value: String): String {
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u%04x".format(ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    fun toJson(value: Any?): String = when (value) {
        null -> "null"
        is Boolean, is Number -> value.toString()
        is String -> "\"${escape(value)}\""
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "\"${escape(k.toString())}\":${toJson(v)}"
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { toJson(it) }
        else -> "\"${escape(value.toString())}\""
    }

    const val MAX_DEPTH = 32

    fun parseObject(json: String): Map<String, Any?> {
        val parser = Parser(json, MAX_DEPTH)
        val result = parser.parseValue(depth = 0)
        @Suppress("UNCHECKED_CAST")
        return result as? Map<String, Any?> ?: emptyMap()
    }

    private class Parser(
        private val src: String,
        private val maxDepth: Int
    ) {
        private var pos = 0

        fun parseValue(depth: Int): Any? {
            if (depth > maxDepth) {
                throw IllegalArgumentException("JSON nesting depth exceeds limit ($maxDepth)")
            }
            skipWs()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> parseObj(depth + 1)
                '[' -> parseArr(depth + 1)
                '"' -> parseStr()
                't' -> { pos += 4; true }
                'f' -> { pos += 5; false }
                'n' -> { pos += 4; null }
                else -> parseNum()
            }
        }

        private fun parseObj(depth: Int): Map<String, Any?> {
            if (depth > maxDepth) {
                throw IllegalArgumentException("JSON nesting depth exceeds limit ($maxDepth)")
            }
            val map = LinkedHashMap<String, Any?>()
            pos++ // '{'
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                if (pos >= src.length || src[pos] != '"') break
                val key = parseStr()
                skipWs()
                if (pos < src.length && src[pos] == ':') pos++
                val value = parseValue(depth)
                map[key] = value
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == '}') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return map
        }

        private fun parseArr(depth: Int): List<Any?> {
            if (depth > maxDepth) {
                throw IllegalArgumentException("JSON nesting depth exceeds limit ($maxDepth)")
            }
            val list = mutableListOf<Any?>()
            pos++ // '['
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue(depth))
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == ']') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return list
        }

        private fun parseStr(): String {
            pos++ // opening '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                if (ch == '"') break
                if (ch == '\\' && pos < src.length) {
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> if (pos + 4 <= src.length) {
                            val hex = src.substring(pos, pos + 4)
                            pos += 4
                            hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun parseNum(): Number? {
            val start = pos
            while (pos < src.length && src[pos] in "-0123456789.eE+") pos++
            val token = src.substring(start, pos)
            return token.toLongOrNull() ?: token.toDoubleOrNull()
        }

        private fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }
    }
}
