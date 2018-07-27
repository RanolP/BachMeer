package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.Type
import io.github.ranolp.bachmeer.parse.ExpressionNode
import io.github.ranolp.bachmeer.parse.ExpressionStatementNode
import io.github.ranolp.bachmeer.parse.Node
import io.github.ranolp.bachmeer.parse.VarDeclNode

class BatchCompiler(root: Node) : Compiler(root) {
    private class Value(type: Type, node: ExpressionNode, value: Any?)
    override fun compile(): ByteArray {
        val result = StringBuilder()
        for (node in root.children) {
            result.append(compile(node))
        }
        return result.toString().toByteArray()
    }

   private fun compile(node: Node): String {
        return when (node) {
            is VarDeclNode -> varDecl(node)
            is ExpressionStatementNode -> compile(node.expression)
            else -> {
                TODO("Compile fail\n\n${node.debug()}")
            }
        }
    }

    private fun varDecl(node: VarDeclNode): String {
        val type = node.expression.getType(this)
        return "// var declaration, ${node.name} = ${compile(node.expression)} ($type)"
    }
}
