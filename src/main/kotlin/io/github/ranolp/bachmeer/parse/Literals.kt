package io.github.ranolp.bachmeer.parse

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
        val data = mutableListOf<TemplateNode.Data>()

        var escape = false
        var dollar = false
        var capture = true

        val builder = StringBuilder()

        loop@ for ((i, c) in it.data.withIndex()) {
            when (c) {
                '\\' -> escape = true
                '$' -> {
                    if (!escape) {
                        dollar = true
                        escape = false
                    }
                }
                '{' -> {
                    if (dollar) {
                        dollar = false
                        capture = true
                        builder.setLength(builder.length - 1)
                        data += TemplateNode.Data.Str(builder.toString())
                        builder.setLength(0)
                        continue@loop
                    }
                }
                '}' -> {
                    if (capture) {
                        val tokens = Tokenizer(
                            builder.toString(), it.line.start - 1, it.column.start + i - builder.length + 1
                        ).tokens()
                        val statement = ParseStatement(
                            source, 0, tokens, Pair(this, it.index)
                        )
                        val expr = statement.expression()
                        if (statement.hasCurrent) {
                            parseError(
                                index - 1, "Expect one expression, but more expression received", statement.current
                            )
                        }
                        data += TemplateNode.Data.Expr(
                            expr ?: parseError(
                                index - 1,
                                "Expression expected",
                                customRange = (it.column.start + i - 1)..(it.column.start + i + 2)
                            )
                        )
                        builder.setLength(0)
                        continue@loop
                    }
                }
            }
            builder.append(c)
        }

        TemplateNode(it, data)
    }
}
