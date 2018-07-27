package io.github.ranolp.bachmeer.parse

import io.github.ranolp.bachmeer.util.where

data class ParseStatement(private val source: String?, var index: Int, var tokens: List<Token>) {
    val hasCurrent: Boolean
        get() = available(index)
    val current: Token
        get() = tokens[index]
    val hasNext: Boolean
        get() = available(index + 1)
    val next: Token?
        get() = if (hasNext) tokens[index + 1] else null
    val lastRead: Token
        get() = if (hasCurrent) current else tokens.last()

    fun available(value: Int) = value < tokens.size

    fun skip(i: Int = 1) {
        index += i
    }

    fun require(from: Int, message: String = "Not parsable", valid: Token.() -> Boolean): Token {
        return require(valid) ?: parseError(from, message)
    }

    fun require(valid: Token.() -> Boolean): Token? {
        if (!hasCurrent || !current.valid()) {
            return null
        }
        val result = current
        skip()
        return result
    }

    fun semicolon(from: Int): Token {
        if (!hasCurrent || current.data != ";") {
            if (!hasCurrent) {
                parseError(
                        from,
                        "Semicolon expected",
                        tokens[index - 1],
                        (tokens[index - 1].column.endInclusive + 1)..(tokens[index - 1].column.endInclusive + 2)
                )
            } else if (tokens[index - 1].line.endInclusive != current.line.start) {
                parseError(
                        from,
                        "Semicolon expected",
                        tokens[index - 1],
                        (tokens[index - 1].column.endInclusive)..(tokens[index - 1].column.endInclusive + 1)
                )
            } else {
                parseError(from, "Semicolon expected")
            }
        }
        val result = current
        skip()
        return result
    }

    fun parseError(from: Int, message: String, customToken: Token? = null, customRange: IntRange? = null): Nothing {
        val start = tokens[from]
        val target = customToken ?: this.lastRead
        throw IllegalStateException(
                "$message, line=${target.line.start + 1}, column=${target.column.start}" + if (source !== null) {
                    "\n\n" + source.where(start, target, customRange ?: target.column)
                } else {
                    ""
                }
        )
    }

}

class Parser(tokens: List<Token>, source: String? = null) {
    private val statement = ParseStatement(source, 0, tokens)

    fun parse(): RootNode = root(statement)!!
}

typealias Explicit<T> = ParseStatement.() -> T

private inline fun <T : INode?> parse(noinline body: ParseStatement.() -> T): ParseStatement.() -> T? = {
    if (hasCurrent) {
        body()
    } else {
        null
    }
}


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

val statement = parse {
    val from = index
    when (current.type) {
        TokenType.KEYWORD -> {
            when (current.data) {
                "let" -> {
                    varDecl()
                }
                else -> parseError(index, "Not parsable keyword found")
            }
        }
        TokenType.SEMICOLON -> {
            skip()
            NodeNothing
        }
        TokenType.IDENTIFIER -> {
            if (!hasNext) {
                if (tokens[index - 1].line.endInclusive != current.line.start) {
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
                TokenType.ASSIGN -> assignStatement()
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

val assignStatement = parse {
    val from = index
    val identifier = require(
            from, "Identifier Expected"
    ) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    require(from, "Assign operator expected") { type == TokenType.ASSIGN }
    val expr = expression() ?: parseError(from, "Expression expected")
    val semicolon = semicolon(from)
    AssignNode(identifier.data, expr, identifier, semicolon)
}

val varDecl = parse {
    val from = index
    val let = require(from, "Variable declaration must start with let") { type == TokenType.KEYWORD && data == "let" }
    val modifiers = mutableSetOf<String>()
    while (current.type == TokenType.KEYWORD) {
        when (current.data) {
            "mut" -> {
                if("mutable" in modifiers) {
                    parseError(from, "Duplicated modifier found")
                }
                modifiers += "mutable"
            }
            else -> parseError(from, "Unexpected keyword ${current.data} found")
        }
        skip()
    }
    val identifier = require(
            from, "Identifier Expected"
    ) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    require(from, "Assign operator expected") { data == "=" }
    val expression = expression() ?: parseError(from, "Expression expected")
    val semicolon = semicolon(from)
    VarDeclNode(
            identifier.data, modifiers, expression, let, semicolon
    )
}

val expression: Explicit<ExpressionNode?> = parse {
    literal() ?: functionCall()
}

val literal = parse {
    integer() ?: decimal() ?: string() ?: template()
}

val integer = parse {
    require { type == TokenType.INTEGER }?.let {
        IntegerNode(it)
    }
}

val decimal = parse {
    require { type == TokenType.DECIMAL }?.let {
        DecimalNode(it)
    }
}

val string = parse {
    require { type == TokenType.STRING }?.let {
        StringNode(it)
    }
}

val template = parse {
    require { type == TokenType.TEMPLATE }?.let {
        TemplateNode(it)
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