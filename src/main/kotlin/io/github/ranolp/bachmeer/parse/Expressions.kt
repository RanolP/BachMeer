package io.github.ranolp.bachmeer.parse

val expression: Explicit<ExpressionNode?> = parse {
    when (current.type) {
        TokenType.IDENTIFIER, TokenType.WEAK_KEYWORD -> {
            next?.type?.let {
                when (it) {
                    TokenType.LPAREN -> functionCall()
                    else -> null
                }
            } ?: variable()
        }
        else -> literal()
    }
}

val functionCall = parse {
    val from = index
    val identifier = require(from) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    require(from) { type == TokenType.LPAREN }
    val params = mutableListOf<ExpressionNode>()
    while (current.type != TokenType.RPAREN) {
        params += expression() ?: parseError(from, "Expression expected")
    }
    val end = require(from) { type == TokenType.RPAREN }
    FuncCallNode(identifier.data, params, identifier, end)
}

val variable = parse {
    require { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }?.let {
        VariableNode(it)
    }
}
