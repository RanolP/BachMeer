package io.github.ranolp.bachmeer.parse


val root = parse {
    val children = mutableListOf<StatementNode>()
    loop@ while (hasCurrent) {
        val result = statement() ?: break
        when (result) {
            is NodeNothing -> continue@loop
            is StatementNode -> children += result
        }
    }
    RootNode(tokens.first(), tokens.last(), children)
}

val statement: Explicit<INode?> = parse {
    val from = index
    when (current.type) {
        TokenType.KEYWORD -> {
            when (current.data) {
                "let" -> varDecl()
                "func" -> funcDecl()
                else -> parseError(index, "Not parsable keyword found")
            }
        }
        TokenType.SEMICOLON -> {
            skip()
            NodeNothing
        }
        TokenType.IDENTIFIER -> {
            if (!hasNext) {
                if (index > 0 && tokens[index - 1].line.endInclusive != current.line.start) {
                    parseError(
                        from,
                        "Semicolon expected",
                        tokens[index - 1],
                        (tokens[index - 1].column.endInclusive)..(tokens[index - 1].column.endInclusive + 1)
                    )
                } else {
                    parseError(from, "Semicolon expected")
                }
            } else when (next!!.type) {
                TokenType.ASSIGN -> assign()
                else -> null
            }
        }
        TokenType.LINE_COMMENT -> {
            skip()
            NodeNothing
        }
        TokenType.BLOCK_COMMENT -> {
            if (current.data[0] == '*' && hasNext && next!!.data == "func" || next!!.data == "let") {
                // todo: documentation
            }
            skip()
            NodeNothing
        }
        else -> null
    } ?: expression()?.let {
        ExpressionStatementNode(semicolon(from), it)
    } ?: parseError(index, "Not parsable token received")
}

val funcDecl = parse {
    val from = index
    val func = require(from, "Keyword 'func' Expected") { type == TokenType.KEYWORD && data == "func" }
    val name = require(from, "Identifier Expected") { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    require { type == TokenType.LPAREN }
    val params = mutableListOf<TypedIdentifier>()
    while (current.type != TokenType.RPAREN) {
        params += typedIdentifier()!!
    }
    require { type == TokenType.RPAREN }

    // TODO: Return type
    val returnType = TypeNode.VOID

    require { type == TokenType.LBRACKET }

    val body = mutableListOf<StatementNode>()
    while (current.type != TokenType.RBRACKET) {
        val statement = statement()
        if (statement is StatementNode) {
            body += statement
        }
    }
    val endBracket = require(from, "Right Bracket Expected") { type == TokenType.RBRACKET }
    FuncDeclNode(func, name.data, returnType, endBracket, params, body)
}

val assign = parse {
    val from = index
    val identifier = require(
        from, "Identifier Expected"
    ) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    val operator = require(from, "Assign operator expected") { type == TokenType.ASSIGN }
    val expr = expression() ?: parseError(from, "Expression expected")
    val semicolon = semicolon(from)
    val assignType = when (operator.data) {
        "+=" -> AssignNode.AssignType.ADD_ASSIGN
        "-=" -> AssignNode.AssignType.SUBTRACT_ASSIGN
        "*=" -> AssignNode.AssignType.MULTIPLY_ASSIGN
        "/=" -> AssignNode.AssignType.DIVIDE_ASSIGN
        "%=" -> AssignNode.AssignType.REMAINDER_ASSIGN
        else -> AssignNode.AssignType.SIMPLE_ASSIGN
    }
    AssignNode(identifier.data, expr, assignType, identifier, semicolon)
}

val varDecl = parse {
    val from = index
    val let = require(from, "Variable declaration must start with let") { type == TokenType.KEYWORD && data == "let" }
    val modifiers = mutableSetOf<String>()
    while (current.type == TokenType.KEYWORD) {
        when (current.data) {
            "mut" -> {
                if ("mutable" in modifiers) {
                    parseError(from, "Duplicated modifier found")
                }
                modifiers += "mutable"
            }
            else -> parseError(from, "Unexpected keyword ${current.data} found")
        }
        skip()
    }
    val identifier = typedIdentifier() ?: parseError(from, "Identifier Expected")
    require(from, "Assign operator expected") { data == "=" }
    val expression = expression() ?: parseError(from, "Expression expected")
    val semicolon = semicolon(from)
    VarDeclNode(identifier, modifiers, expression, let, semicolon)
}
