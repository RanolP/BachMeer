package io.github.ranolp.bachmeer.parse

class Parser(tokens: List<Token>, source: String? = null) {
    private val statement = ParseStatement(source, 0, tokens)

    fun parse(): RootNode = root(statement)!!
}

typealias Explicit<T> = ParseStatement.() -> T

internal fun <T : INode?> parse(body: ParseStatement.() -> T): ParseStatement.() -> T? = {
    if (hasCurrent) {
        body()
    } else {
        null
    }
}
