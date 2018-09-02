package io.github.ranolp.bachmeer.parse

import io.github.ranolp.bachmeer.util.where

data class Token(val data: String, val type: TokenType, val line: IntRange, val column: IntRange, val index: Int)

enum class TokenType {
    LINE_COMMENT,
    BLOCK_COMMENT,
    KEYWORD,
    WEAK_KEYWORD,
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    STRING,
    TEMPLATE,
    ASSIGN,
    OPERATOR,
    /**
     * Equals to `(`
     */
    LPAREN,
    /**
     * Equals to `)`
     */
    RPAREN,
    /**
     * Equals to `[`
     */
    LBRACE,
    /**
     * Equals to `]`
     */
    RBRACE,
    /**
     * Equals to `{`
     */
    LBRACKET,
    /**
     * Equals to `}`
     */
    RBRACKET,
    COLON,
    SEMICOLON,
    DOT,
    NULL_SAFE_DOT,
    COMMA,
    ELVIS,
}

class Tokenizer(private val source: String, startLine: Int = 0, startCol: Int = 0) {

    private var index = 0
    private val current: Char
        get() = source[index]
    private val hasNext: Boolean
        get() = source.length > index
    private val next: Char
        get() = source[index + 1]
    private var line: Int = startLine + 1
    private var column: Int = startCol
    private val start: Triple<Int, Int, Int>
        get() = Triple(line, column, index)

    private val keywords = listOf(
        "let",
        "mut",
        "func",
        "for",
        "while",
        "break",
        "continue",
        "if",
        "else",
        "type",
        "vararg",
        "return",
        "match",
        "is",
        "as",
        "in",
        "true",
        "false",
        "extern"
    )
    private val weakKeywords = listOf(
        "Self"
    )

    private fun newLine() {
        line++
        column = 0
    }

    private fun skip(i: Int = 1) {
        when {
            i < 0 -> throw IllegalArgumentException("Can't back")
            i == 0 -> return
            else -> {
                column++
                val old = index
                index += i
                for (c in source.substring(old, minOf(index, source.length - 1))) {
                    if (c == '\n') {
                        newLine()
                    }
                }
            }
        }
    }

    fun tokens(): List<Token> {
        var result: Token?
        val tokens = mutableListOf<Token>()
        while (true) {
            result = next()
            if (result != null) {
                tokens += result
            } else {
                break
            }
        }
        return tokens
    }

    fun next(): Token? {
        while (hasNext && current.isWhitespace()) {
            skip()
        }
        if (!hasNext) {
            return null
        }
        return when (current) {
            '0' -> nextComplexNumber()
            in '1'..'9' -> nextNumber()
            '-', '+', '*', '%' -> {
                if (next == '=') {
                    str(TokenType.ASSIGN, "$current$next")
                } else {
                    str(TokenType.OPERATOR)
                }
            }
            '!' -> {
                if (next == '=') {
                    str(TokenType.OPERATOR, "$current$next")
                } else {
                    str(TokenType.OPERATOR)
                }
            }
            '/' -> {
                if (hasNext) {
                    when (next) {
                        '/' -> {
                            skip(2)
                            val comment = StringBuilder()
                            val (sl, sc, si) = start
                            while (hasNext && current != '\n') {
                                comment.append(current)
                                skip()
                            }
                            return Token(comment.toString().trim(), TokenType.LINE_COMMENT, sl..line, sc..column, si)
                        }
                        '*' -> {
                            skip(2)
                            val comment = StringBuilder()
                            val (sl, sc, si) = start
                            var star = false
                            while (hasNext) {
                                if (current == '*') {
                                    star = true
                                } else if (star && current == '/') {
                                    comment.setLength(comment.length - 1)
                                    break
                                }
                                comment.append(current)
                                skip()
                            }
                            return Token(
                                comment.toString().trimIndent(), TokenType.BLOCK_COMMENT, sl..line, sc..column, si
                            )
                        }
                        '=' -> {
                            str(TokenType.ASSIGN, "$current$next")
                        }
                        else -> str(TokenType.OPERATOR)
                    }
                } else {
                    str(TokenType.OPERATOR)
                }
            }
            '(' -> str(TokenType.LPAREN)
            ')' -> str(TokenType.RPAREN)
            '[' -> str(TokenType.LBRACE)
            ']' -> str(TokenType.RBRACE)
            '{' -> str(TokenType.LBRACKET)
            '}' -> str(TokenType.RBRACKET)
            ':' -> str(TokenType.COLON)
            '.' -> str(TokenType.DOT)
            ',' -> str(TokenType.COMMA)
            '=' -> {
                if (next == '=') {
                    str(TokenType.OPERATOR, "$current$next")
                } else {
                    str(TokenType.ASSIGN)
                }
            }
            ';' -> str(TokenType.SEMICOLON)
            '?' -> {
                when (next) {
                    '.' -> str(TokenType.NULL_SAFE_DOT, "$current$next")
                    ':' -> str(TokenType.ELVIS, "$current$next")
                    else -> str(TokenType.OPERATOR)
                }
            }
            '"' -> nextString('\"', TokenType.STRING)
            '\'' -> nextString('\'', TokenType.STRING)
            '`' -> nextString('`', TokenType.TEMPLATE)
            else -> nextIdentifier()
        }
    }

    private fun nextIdentifier(): Token? {
        val result = StringBuilder()
        val (sl, sc, si) = start
        while (hasNext && current !in "-+*%!/()[]{}:.,=;?\"' \t\r\n") {
            result.append(current)
            skip()
        }
        if (result.isEmpty()) {
            return null
        }
        return Token(
            result.toString(), when (result.toString()) {
                in keywords -> TokenType.KEYWORD
                in weakKeywords -> TokenType.WEAK_KEYWORD
                else -> TokenType.IDENTIFIER
            }, sl..line, sc..column, si
        )
    }

    private fun str(type: TokenType, data: String = "$current"): Token {
        val (sl, sc, si) = start
        skip(data.length)
        return Token(data, type, sl..line, sc..column, si)
    }

    private fun nextString(startChar: Char, type: TokenType): Token {
        val result = StringBuilder()
        val (sl, sc, si) = start
        var backslash = false
        var unicode = false
        skip()
        while (hasNext) {
            if (!backslash && current == startChar) {
                skip()
                break
            }
            if (unicode) {
                val (ssl, ssc, ssi) = start
                val hex = nextHexInt()
                if (hex.length < 4) {
                    tokenizeError("Incomplete unicode string found at line $ssl, column $ssc", ssi - 2)
                }
                result.append("\\u$hex")
                unicode = false
                continue
            } else if (current == '\\') {
                backslash = true
            } else if (backslash) {
                if (escapeable(current)) {
                    if (current == 'u') {
                        unicode = true
                    } else {
                        result.append("\\$current")
                    }
                    backslash = false
                } else {
                    tokenizeError("Unescapeable sequence found at line $line, column $column", index)
                }
            } else if (current == '\n') {
                tokenizeError("Line feed found in string at line $line, column $column", index)
            } else {
                backslash = false
                result.append(current)
            }
            skip()
        }
        return Token(result.toString(), type, sl..line, sc..column, si)
    }

    private fun escapeable(char: Char): Boolean = char in "0abrnrut\\\"'`"

    private fun nextComplexNumber(): Token {
        return if (hasNext && (next == 'x' || next == 'X')) {
            val (sl, sc, si) = start
            skip(2)
            Token(nextHexInt().toLong(16).toString(), TokenType.INTEGER, sl..line, sc..column, si)
        } else {
            val si = start.third
            val result = nextNumber()
            if (result.data.length > 1 && result.type == TokenType.INTEGER) {
                tokenizeError(
                    "Not allowed octal number found at line ${result.line}, column ${result.column.start}", si
                )
            }
            result
        }
    }

    private fun nextHexInt(): String {
        val result = StringBuilder()
        while (hasNext && (current.isDigit() || current in 'A'..'F' || current == '_')) {
            if (current != '_') {
                result.append(current)
            }
            skip()
        }
        return result.toString()
    }

    private fun nextNumber(): Token {
        val result = StringBuilder()
        val (sl, sc, si) = start
        var type = TokenType.INTEGER
        var dotFound = false
        var dotAtEnd = false
        while (hasNext && current in "0123456789_ ") {
            if (current == '.') {
                if (dotFound) {
                    if (dotAtEnd) {
                        tokenizeError("Duplicated dot found at line $line, column $column", index)
                    }
                    break
                } else {
                    dotFound = true
                    dotAtEnd = true
                    type = TokenType.DECIMAL
                }
                result.append(current)
                skip()
                continue
            } else if (current !in "_ ") {
                result.append(current)
            }
            dotAtEnd = false
            skip()
        }
        return Token(result.toString(), type, sl..line, sc..column, si)
    }

    private fun tokenizeError(reason: String, index: Int = this.index): NodeNothing {
        throw IllegalStateException(
            "$reason\n\nSource code is here :\n${source.where(index)}"
        )
    }
}
