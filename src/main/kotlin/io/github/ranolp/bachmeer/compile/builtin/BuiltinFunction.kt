package io.github.ranolp.bachmeer.compile.builtin

import io.github.ranolp.bachmeer.compile.data.BMFunction

abstract class BuiltinFunction(val name: String) : BMFunction() {
    fun paramCount(expected: String, actual: Int): Nothing {
        throw IllegalArgumentException("Function $name expects $expected parameters, but $actual parameters received")
    }
}
