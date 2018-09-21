package io.github.ranolp.bachmeer.parse

import io.github.ranolp.bachmeer.util.where


data class ParseStatement(internal val source: String?,
        var index: Int,
        var tokens: List<Token>,
        private val parent: Pair<ParseStatement, Int>? = null
) {
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

    fun parseError(from: Int, message: String, customToken: Token? = null, customRange: IntRange? = null, index: Int = 0
    ): Nothing {
        if (parent != null) {
            parent.first.parseError(from, message, customToken, customRange, index + parent.second)
        } else {
            val start = tokens[from]
            val target = customToken ?: this.lastRead
            throw IllegalStateException(
                "$message, line=${target.line.start}, column=${target.column.start}" + if (source !== null) {
                    "\n\n" + source.where(start, target, customRange ?: target.column)
                } else {
                    ""
                }
            )
        }
    }
}
