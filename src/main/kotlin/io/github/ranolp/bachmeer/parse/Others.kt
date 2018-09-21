package io.github.ranolp.bachmeer.parse

val typedIdentifier = parse {
    val from = index
    val accessName = require(
        from, "Identifier Expected"
    ) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }
    require { type == TokenType.COLON } ?: return@parse TypedIdentifier(accessName, null)

    TypedIdentifier(accessName, type()!!)
}

val type = parse {
    val from = index
    val accessName = require(
        from, "Identifier Expected"
    ) { type == TokenType.IDENTIFIER || type == TokenType.WEAK_KEYWORD }

    TypeNode(listOf(accessName))
}
