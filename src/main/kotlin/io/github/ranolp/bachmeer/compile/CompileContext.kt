package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.compile.data.BMObject
import io.github.ranolp.bachmeer.platform.Platform

class CompileContext(val platform: Platform, val useMagicLookup: Boolean = false, val parent: CompileContext? = null) {
    data class Value(var real: BMObject, val immutable: Boolean, var type: String? = null)

    private val values = mutableMapOf<String, Value>()
    val names: Set<String>
        get() = values.keys.toSet()

    fun set(name: String, value: BMObject, immutable: Boolean, overwrite: Boolean = false) {
        if (name in names && !overwrite) {
            throw IllegalStateException("Name `$name` is duplicated")
        }
        val got = values[name]
        if (got != null) {
            if (got.immutable) {
                throw IllegalStateException("Variable `$name` is immutable value")
            }
            got.real = value
        } else {
            values[name] = Value(value, immutable)
        }
    }

    operator fun contains(name: String): Boolean {
        return get(name) != null
    }

    operator fun get(name: String): Value? {
        return getRecursively(name) ?: if (useMagicLookup) {
            manipulateName(name).forEach {
                val got = getRecursively(it)
                if (got != null) {
                    return got
                }
            }
            null
        } else {
            null
        }
    }

    private fun getRecursively(name: String): Value? {
        return values[name] ?: parent?.run { getRecursively(name) }
    }

    fun createSubContext(): CompileContext {
        return CompileContext(platform, useMagicLookup, this)
    }

    private fun manipulateName(name: String): List<String> {
        val contains = name.filter { it == '_' }.count()
        // snake_case
        if (contains >= 2 || (contains == 1 && name[0] != '_')) {
            val prefix = name[0] == '_'
            return if (prefix) {
                val words = name.substring(1).split('_')
                listOf("_" + lowerCamelCase(words), "_" + UpperCamelCase(words))
            } else {
                val words = name.split('_')
                listOf(lowerCamelCase(words), UpperCamelCase(words))
            }
        } else {
            var previousWasCapital = false
            val words = mutableListOf<String>()
            val builder = StringBuilder()
            name.decapitalize().forEach {
                if (Character.isUpperCase(it)) previousWasCapital = true
                if (Character.isLetterOrDigit(it) && previousWasCapital) {
                    previousWasCapital = false
                    words += builder.toString()
                    builder.setLength(0)
                    builder.append(it.toLowerCase())
                } else {
                    builder.append(it)
                }
            }
            if (builder.isNotEmpty()) {
                words += builder.toString()
            }

            return if (name[0].isUpperCase()) {
                listOf(snake_case(words), lowerCamelCase(words))
            } else {
                listOf(snake_case(words), UpperCamelCase(words))
            }
        }
    }

    private fun snake_case(words: List<String>): String {
        return words.joinToString("_") { it.toLowerCase() }
    }

    private fun lowerCamelCase(words: List<String>): String {
        return words[0].toLowerCase() + words.asSequence().drop(1).joinToString("") { it.capitalize() }
    }

    private fun UpperCamelCase(words: List<String>): String {
        return words.joinToString("") { it.capitalize() }
    }
}
