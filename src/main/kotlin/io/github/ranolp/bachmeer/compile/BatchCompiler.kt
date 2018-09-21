package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.Type
import io.github.ranolp.bachmeer.compile.data.BMFunction
import io.github.ranolp.bachmeer.compile.data.BMFunctionInCode
import io.github.ranolp.bachmeer.compile.data.BMInteger
import io.github.ranolp.bachmeer.compile.data.BMString
import io.github.ranolp.bachmeer.compile.data.BMTemplate
import io.github.ranolp.bachmeer.compile.data.BMVariable
import io.github.ranolp.bachmeer.compile.data.CompileResult
import io.github.ranolp.bachmeer.parse.AssignNode
import io.github.ranolp.bachmeer.parse.ExpressionNode
import io.github.ranolp.bachmeer.parse.ExpressionStatementNode
import io.github.ranolp.bachmeer.parse.FuncCallNode
import io.github.ranolp.bachmeer.parse.FuncDeclNode
import io.github.ranolp.bachmeer.parse.IntegerNode
import io.github.ranolp.bachmeer.parse.Node
import io.github.ranolp.bachmeer.parse.StringNode
import io.github.ranolp.bachmeer.parse.TemplateNode
import io.github.ranolp.bachmeer.parse.VarDeclNode
import io.github.ranolp.bachmeer.parse.VariableNode
import io.github.ranolp.bachmeer.platform.Platform
import java.util.*

class BatchCompiler(compilerOption: CompilerOption = CompilerOption.DEFAULT) : Compiler(
    Platform.BATCH, compilerOption
) {

    override fun compile(parentNode: Node, root: Boolean): String {
        val result = StringJoiner("\n")
        if (root) {
            result.add(":: Auto generated code by BachMeer ::")
            result.add("@echo off")
        }

        for (node in parentNode.children) {
            val compiled = when (node) {
                is VarDeclNode -> varDecl(node)
                is ExpressionStatementNode -> compile(node, false)
                is FuncCallNode -> funcCall(node).asCode(this)
                is FuncDeclNode -> funcDecl(node)
                is AssignNode -> assign(node)
                else -> notImplemented(node)
            }.split("\n")
            compiled.forEach {
                if (!it.isEmpty()) {
                    result.add(it)
                }
            }
        }
        return result.toString()
    }

    override fun evaluateExpression(node: ExpressionNode): CompileResult.Data {
        return when (node) {
            is IntegerNode -> CompileResult.Data(BMInteger(node.value))
            is StringNode -> CompileResult.Data(BMString(node.value))
            is TemplateNode -> CompileResult.Data(BMTemplate(node))
            is VariableNode -> CompileResult.Data(BMVariable(node.variableName))
            is FuncCallNode -> funcCall(node)

            else -> notImplemented(node)
        }
    }

    private fun varDecl(node: VarDeclNode): String {
        return _assign(node.identifier.accessName.data, node.expression, "mutable" !in node.modifiers, false)
    }

    private fun funcCall(node: FuncCallNode): CompileResult.Data {
        val name = node.functionName
        when (name) {
            in context -> {
                val value = context[name]
                val real = value?.real as? BMFunction ?: error(
                    "Expect `$name` is function. but it's ${value?.type ?: Type.UNKNOWN}"
                )
                val evaluated = node.params.map { evaluateExpression(it) }
                return real.invoke(this, *evaluated.map { it.bmObject }.toTypedArray()).let {
                    it.withPreCode(evaluated.joinToString("\n") { it.preCode })
                }
            }
        }
        throw IllegalStateException("Function $name is not found.")
    }

    private fun assign(node: AssignNode): String {
        return when (node.assignType) {
            AssignNode.AssignType.SIMPLE_ASSIGN -> {
                _assign(node.variableName, node.expression, false, true)
            }
            else -> notImplemented(node)
        }
    }

    private fun _assign(name: String, node: ExpressionNode, immutable: Boolean, ignoreDuplicate: Boolean): String {
        val evaluated = evaluateExpression(node)
        context.set(name, evaluated.bmObject, immutable, ignoreDuplicate)
        val compiled = evaluated.compile(this)
        return compiled.preCode + when (node) {
            is IntegerNode -> {
                "::BMA_Type=Int\nset /a $name=${compiled.code}"
            }
            is StringNode -> {
                "::BMA_Type=String\nset \"$name=${compiled.code}\""
            }
            else -> {
                when (node.getType(this)) {
                    Type.INTEGER -> {
                        "::BMA_Type=Int\nset /a $name=${compiled.code}"
                    }
                    Type.DECIMAL -> notImplemented(node)
                    Type.STRING -> notImplemented(node)
                    Type.ARRAY -> notImplemented(node)
                    Type.OBJECT -> notImplemented(node)
                    Type.UNKNOWN -> notImplemented(node)
                    Type.VOID -> notImplemented(node)
                    Type.FUNCTION -> notImplemented(node)
                }
            }
        }
    }

    private fun funcDecl(funcDeclNode: FuncDeclNode): String {
        val func = BMFunctionInCode(funcDeclNode)
        context.set(funcDeclNode.functionName, func, true, false)
        return func.compile(this).asCode(this)
    }

    private fun notImplemented(node: Node): Nothing {
        TODO("Compile fail\n\n${node.debug()}")
    }
}
