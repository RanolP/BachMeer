package io.github.ranolp.bachmeer.compile.builtin

import io.github.ranolp.bachmeer.compile.Compiler
import io.github.ranolp.bachmeer.compile.data.BMObject
import io.github.ranolp.bachmeer.compile.data.BMVariable
import io.github.ranolp.bachmeer.compile.data.BMVoid
import io.github.ranolp.bachmeer.compile.data.CompileResult
import io.github.ranolp.bachmeer.platform.Platform

object bm_println : BuiltinFunction("println") {
    override fun invoke(compiler: Compiler, vararg args: BMObject): CompileResult.Data {
        val compiledArguments = args.map { it.compile(compiler) }
        return when (compiler.platform) {
            Platform.BATCH -> when (args.size) {
                0 -> CompileResult.Data("echo.", BMVoid)
                1 -> CompileResult.Data(
                    compiledArguments[0].preCode + "\necho ${compiledArguments[0].code}", BMVoid
                )
                else -> {
                    val preCode = compiledArguments.joinToString("\n") { it.preCode }
                    val code = compiledArguments.joinToString(", ") { it.code }
                    CompileResult.Data(
                        "$preCode\necho $code", BMVoid
                    )
                }
            }
            else -> TODO()
        }
    }
}

object bm_print : BuiltinFunction("print") {
    override fun invoke(compiler: Compiler, vararg args: BMObject): CompileResult.Data {
        val compiledArguments = args.map { it.compile(compiler) }
        return when (compiler.platform) {
            Platform.BATCH -> when (args.size) {
                0 -> CompileResult.Data("", BMVoid)
                1 -> CompileResult.Data(
                    compiledArguments[0].preCode + "\necho | set /p BM_NOP=\"${compiledArguments[0].code}\"", BMVoid
                )
                else -> {
                    val preCode = compiledArguments.joinToString("\n") { it.preCode }
                    val code = compiledArguments.joinToString(", ") { it.code }
                    CompileResult.Data(
                        "$preCode\necho | set /p BM_NOP=\"$code\"", BMVoid
                    )
                }
            }
            else -> TODO()
        }
    }
}

object bm_read_line : BuiltinFunction("read_line") {
    override fun invoke(compiler: Compiler, vararg args: BMObject): CompileResult.Data {
        return when (compiler.platform) {
            Platform.BATCH -> when (args.size) {
                0 -> {
                    val name = compiler.newName
                    CompileResult.Data("set /p $name=", BMVariable(name))
                }
                1 -> {
                    val name = compiler.newName
                    val compiled = args[0].compile(compiler)
                    CompileResult.Data("${compiled.preCode}\nset /p $name=${compiled.code}", BMVariable(name))
                }
                else -> paramCount("0 or 1", args.size)
            }
            Platform.POWER_SHELL -> TODO()
            Platform.BASH -> TODO()
        }
    }
}
