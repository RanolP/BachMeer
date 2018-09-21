package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.compile.builtin.BuiltinFunction
import io.github.ranolp.bachmeer.compile.builtin.bm_print
import io.github.ranolp.bachmeer.compile.builtin.bm_println
import io.github.ranolp.bachmeer.compile.builtin.bm_read_line
import io.github.ranolp.bachmeer.compile.data.CompileResult
import io.github.ranolp.bachmeer.parse.ExpressionNode
import io.github.ranolp.bachmeer.parse.Node
import io.github.ranolp.bachmeer.platform.Platform

abstract class Compiler(val platform: Platform, val option: CompilerOption) {
    private var nameId = 0
    val newName: String
        get() = "_${nameId++}"

    val context = CompileContext(platform, option.useMagicLookup)
    private val _builtins: MutableMap<String, BuiltinFunction> = mutableMapOf()
    val builtins: Map<String, BuiltinFunction>
        get() = _builtins.toMap()

    abstract fun compile(parentNode: Node, root: Boolean): String

    abstract fun evaluateExpression(node: ExpressionNode):  CompileResult.Data

    private fun registerBuiltinFunction(function: BuiltinFunction) {
        context.set(function.name, function, true, false)
    }

    init {
        registerBuiltinFunction(bm_println)
        registerBuiltinFunction(bm_print)
        registerBuiltinFunction(bm_read_line)
    }
}
