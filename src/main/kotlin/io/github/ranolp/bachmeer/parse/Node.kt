package io.github.ranolp.bachmeer.parse

import io.github.ranolp.bachmeer.Type
import io.github.ranolp.bachmeer.compile.Compiler
import io.github.ranolp.bachmeer.util.times
import java.lang.StringBuilder

sealed class INode(val name: String) {
    open val data: Map<String, Any?> = emptyMap()
    abstract fun debug(depth: Int = 0, step: Int = 2): String

    abstract fun getType(compiler: Compiler): Type
}

object NodeNothing : INode("Nothing") {
    override fun getType(compiler: Compiler): Type {
        return Type.VOID
    }

    override fun debug(depth: Int, step: Int): String = ""
}

abstract class Node(name: String, val tokenStart: Token, val tokenEnd: Token, val children: List<Node>) : INode(name) {
    override fun debug(depth: Int, step: Int): String {
        val print = StringBuilder(" " * depth + name)
        print.append('(')
        if (data.isNotEmpty()) {
            print.append("data={").append(data.entries.joinToString(", ") {
                "${it.key}=${it.value}"
            }).append('}')
            print.append(", ")
        }
        print.append("tokenStart={line=").append(tokenStart.line.start + 1).append(", column=")
                .append(tokenStart.column.start).append("}, tokenEnd={line=").append(tokenEnd.line.endInclusive)
                .append(", column=").append(tokenEnd.column.endInclusive).append("})")
        children.forEach { print.append('\n').append(it.debug(depth + step, step)) }
        return print.toString()
    }
}

class RootNode(tokenStart: Token, tokenEnd: Token, statements: List<StatementNode>) : Node(
        "<ROOT>", tokenStart, tokenEnd, statements
) {
    override fun getType(compiler: Compiler): Type {
        return Type.VOID
    }
}

abstract class StatementNode(name: String, tokenStart: Token, tokenEnd: Token, children: List<Node>) : Node(
        name, tokenStart, tokenEnd, children
) {
    override fun getType(compiler: Compiler): Type {
        return Type.VOID
    }
}

class ExpressionStatementNode(semicolon: Token, val expression: ExpressionNode) : StatementNode(
        "Expression Statement", expression.tokenStart, semicolon, listOf(expression)
) {
    override fun getType(compiler: Compiler): Type {
        return expression.getType(compiler)
    }
}

abstract class ExpressionNode(name: String, tokenStart: Token, tokenEnd: Token, children: List<Node>) : Node(
        name, tokenStart, tokenEnd, children
)

class AssignNode(val variableName: String, val expression: ExpressionNode, tokenStart: Token, tokenEnd: Token) :
        StatementNode("Assign", tokenStart, tokenEnd, listOf(expression)) {
    override val data: Map<String, Any?> = mapOf("variable-name" to variableName)
}

class VarDeclNode(val variableName: String, val modifiers: Set<String>, val expression: ExpressionNode,
        tokenStart: Token, tokenEnd: Token) : StatementNode(
        "Variable Declaration", tokenStart, tokenEnd, listOf(expression)
) {
    override val data: Map<String, Any?> = mapOf("variable-name" to variableName, "modifiers" to modifiers)
}

abstract class LiteralNode(name: String, token: Token) : ExpressionNode(
        name, token, token, emptyList()
)

class IntegerNode(token: Token) : LiteralNode("Integer", token) {
    val value: Long = token.data.toLong()
    override fun getType(compiler: Compiler): Type {
        return Type.INTEGER
    }
}

class DecimalNode(token: Token) : LiteralNode("Decimal", token) {
    val value: Double = token.data.toDouble()
    override fun getType(compiler: Compiler): Type {
        return Type.DECIMAL
    }
}

class StringNode(token: Token) : LiteralNode("String", token) {
    val value: String = token.data
    override fun getType(compiler: Compiler): Type {
        return Type.STRING
    }
}

class TemplateNode(token: Token) : LiteralNode("Template", token) {
    val value: String = token.data
    override fun getType(compiler: Compiler): Type {
        return Type.STRING
    }
}

class FuncCallNode(val functionName: String, val params: List<ExpressionNode>, tokenStart: Token, tokenEnd: Token) :
        ExpressionNode("Function Call", tokenStart, tokenEnd, params) {
    override val data: Map<String, Any?> = mapOf("function-name" to functionName)
    override fun getType(compiler: Compiler): Type {
        return Type.UNKNOWN
    }
}
