package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.Type
import io.github.ranolp.bachmeer.parse.AssignNode
import io.github.ranolp.bachmeer.parse.ExpressionNode
import io.github.ranolp.bachmeer.parse.ExpressionStatementNode
import io.github.ranolp.bachmeer.parse.FuncCallNode
import io.github.ranolp.bachmeer.parse.IntegerNode
import io.github.ranolp.bachmeer.parse.Node
import io.github.ranolp.bachmeer.parse.StringNode
import io.github.ranolp.bachmeer.parse.TemplateNode
import io.github.ranolp.bachmeer.parse.VarDeclNode
import io.github.ranolp.bachmeer.parse.VariableNode
import java.util.*

class BatchCompiler(root: Node) : Compiler(root) {
    private var nameId = 0
    private val names = mutableListOf<String>()

    override fun compile(): ByteArray {
        val result = StringJoiner("\n")
        result.add(":: Auto generated code by BachMeer ::")
        result.add("@echo off")
        for (node in root.children) {
            result.add(compile(node))
        }
        return result.toString().toByteArray()
    }

    private fun compile(node: Node): String {
        return when (node) {
            is VarDeclNode -> varDecl(node)
            is ExpressionStatementNode -> compile(node.expression)
            is FuncCallNode -> funcCall(node)
            is AssignNode -> assign(node)
            else -> notImplemented(node)
        }
    }

    private fun evaluateExpression(node: ExpressionNode): Pair<String, String> {
        val name by lazy {
            "_${nameId++}"
        }
        return when (node) {
            is IntegerNode -> Pair("", node.value.toString())
            is StringNode -> Pair("", node.value)
            is TemplateNode -> {
                val code = node.datas.map {
                    when (it) {
                        is TemplateNode.Data.Str -> Pair("", it.value)
                        is TemplateNode.Data.Expr -> evaluateExpression(it.node)
                    }
                }.reduce { l, r ->
                    Pair(l.first + r.first, l.second + r.second)
                }.let {
                    it.first + "set \"$name=${it.second}\"\n"
                }

                Pair(code, "%$name%")
            }
            is VariableNode -> {
                Pair("", "%${node.variableName}%")
            }
            else -> notImplemented(node)
        }
    }

    private fun varDecl(node: VarDeclNode): String {
        return _assign(node.variableName, node.expression)
    }

    private fun funcCall(node: FuncCallNode): String {
        val params = node.params
        fun paramCount(functionName: String, expected: Int, actual: Int) {
            throw IllegalArgumentException("function $functionName expects $expected parameters, but $actual parameters received")
        }
        when (node.functionName) {
            "println" -> {
                if (params.size != 1) {
                    paramCount("println", 1, params.size)
                }
                val evaluated = evaluateExpression(params[0])

                return evaluated.first + "echo ${evaluated.second}"
            }
        }
        notImplemented(node)
    }

    private fun assign(node: AssignNode): String {
        return when (node.assignType) {
            AssignNode.AssignType.SIMPLE_ASSIGN -> {
                _assign(node.variableName, node.expression)
            }
            else -> notImplemented(node)
        }
    }

    private fun _assign(name: String, node: ExpressionNode): String {
        val evaluated = evaluateExpression(node)
        return evaluated.first + when (node) {
            is IntegerNode -> {
                "set /a $name=${evaluated.second}"
            }
            is StringNode -> {
                "set \"$name=${evaluated.second}\""
            }
            else -> {
                when (node.getType(this)) {
                    Type.INTEGER -> {
                        "set /a $name=${evaluated.second}"
                    }
                    Type.DECIMAL -> notImplemented(node)
                    Type.STRING -> notImplemented(node)
                    Type.ARRAY -> notImplemented(node)
                    Type.OBJECT -> notImplemented(node)
                    Type.UNKNOWN -> notImplemented(node)
                    Type.VOID -> notImplemented(node)
                }
            }
        }
    }

    private fun notImplemented(node: Node): Nothing {
        TODO("Compile fail\n\n${node.debug()}")
    }
}
