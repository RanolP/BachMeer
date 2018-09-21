package io.github.ranolp.bachmeer.compile.data

import io.github.ranolp.bachmeer.Type
import io.github.ranolp.bachmeer.compile.Compiler
import io.github.ranolp.bachmeer.parse.FuncDeclNode
import io.github.ranolp.bachmeer.parse.TemplateNode
import io.github.ranolp.bachmeer.platform.Platform
import java.util.*

interface BMObject {
    fun getType(compiler: Compiler): Type

    fun evaluate(compiler: Compiler): Any?

    fun compile(compiler: Compiler): CompileResult
}

class BMInteger(private val value: Long) : BMObject {
    override fun getType(compiler: Compiler): Type = Type.INTEGER

    override fun evaluate(compiler: Compiler): Long = value

    override fun compile(compiler: Compiler): CompileResult = CompileResult.Simple(value)

    override fun toString(): String = "BachMeer::Int($value)"
}

class BMDecimal(val value: Double) : BMObject {
    override fun getType(compiler: Compiler): Type = Type.DECIMAL

    override fun evaluate(compiler: Compiler): Double = value

    override fun compile(compiler: Compiler): CompileResult {
        TODO()
    }

    override fun toString(): String = "BachMeer::Decimal($value)"
}

class BMString(private val value: String) : BMObject {
    override fun getType(compiler: Compiler): Type = Type.STRING

    override fun evaluate(compiler: Compiler): String = value

    override fun compile(compiler: Compiler): CompileResult {
        return when (compiler.platform) {
            // TODO: Batch에서 이스케이프도 해야 해.
            Platform.BATCH -> CompileResult.Simple(value)
            else -> TODO()
        }

    }

    override fun toString(): String = "BachMeer::String($value)"
}

class BMVariable(private val name: String) : BMObject {
    override fun getType(compiler: Compiler): Type = compiler.context[name]?.real?.getType(compiler) ?: Type.UNKNOWN

    override fun evaluate(compiler: Compiler): Any? = compiler.context[name]?.real?.evaluate(compiler)

    override fun compile(compiler: Compiler): CompileResult = when (compiler.platform) {
        Platform.BATCH -> CompileResult.Simple("%$name%")
        else -> TODO()
    }

    override fun toString(): String = "BachMeer::Variable($name)"
}

class BMTemplate(private val node: TemplateNode) : BMObject {
    override fun getType(compiler: Compiler): Type = Type.STRING

    override fun evaluate(compiler: Compiler): Any? {
        return node.datas.joinToString("") {
            when (it) {
                is TemplateNode.Data.Str -> it.value
                is TemplateNode.Data.Expr -> compiler.evaluateExpression(it.node).bmObject.evaluate(compiler).toString()
            }
        }
    }

    override fun compile(compiler: Compiler): CompileResult {
        return when (compiler.platform) {
            // TODO: Batch에서 이스케이프도 해야 해.
            Platform.BATCH -> node.datas.asSequence().map {
                when (it) {
                    is TemplateNode.Data.Str -> CompileResult.Data(BMString(it.value))
                    is TemplateNode.Data.Expr -> compiler.evaluateExpression(it.node)
                }.compile(compiler).toPair
            }.reduce { l, r ->
                Pair(l.first + r.first, l.second + r.second)
            }.let {
                val name = compiler.newName
                val code = "set \"$name=${it.second}\""
                if (it.first.isEmpty()) {
                    CompileResult.Complex(code, "%$name%")
                } else {
                    CompileResult.Complex("${it.first}\n$code", "%$name")
                }
            }
            else -> TODO()
        }
    }

    override fun toString(): String = "BachMeer::Template(${node.datas.joinToString("") {
        when (it) {
            is TemplateNode.Data.Expr -> "${'$'}{expression}"
            is TemplateNode.Data.Str -> it.value
        }
    }})"
}

abstract class BMFunction : BMObject {
    override fun getType(compiler: Compiler): Type = Type.FUNCTION
    abstract fun invoke(compiler: Compiler, vararg args: BMObject): CompileResult.Data

    override fun evaluate(compiler: Compiler): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compile(compiler: Compiler): CompileResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class BMFunctionInCode(private val node: FuncDeclNode) : BMFunction() {
    override fun invoke(compiler: Compiler, vararg args: BMObject): CompileResult.Data {
        return when (compiler.platform) {
            Platform.BATCH -> {
                CompileResult.Data("call :${node.functionName}", BMVoid)
            }
            Platform.POWER_SHELL -> TODO()
            Platform.BASH -> TODO()
        }
    }

    override fun compile(compiler: Compiler): CompileResult {
        return when (compiler.platform) {
            Platform.BATCH -> {
                val builder = StringJoiner("\n")

                builder.add(":${node.functionName}")
                builder.add("setlocal")
                for (statement in node.statements) {
                    builder.add(compiler.compile(statement, false))
                }
                // TODO: Return value
                builder.add("endlocal")

                CompileResult.Simple(builder.toString())
            }
            Platform.POWER_SHELL -> TODO()
            Platform.BASH -> TODO()
        }
    }
}

object BMVoid : BMObject {
    override fun getType(compiler: Compiler): Type = Type.VOID

    override fun evaluate(compiler: Compiler) {}

    override fun compile(compiler: Compiler): CompileResult = CompileResult.Nop
}
