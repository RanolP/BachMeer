package io.github.ranolp.bachmeer.util

import io.github.ranolp.bachmeer.parse.Token

fun String.where(start: Token, where: Token, range: IntRange = where.column): String {
    return where(start.line.start, start.column.start, range)
}

fun String.where(line: Int, col: Int, capture: IntRange? = null): String {
    var index = 0
    var linefeeds = line - 1
    var lastLinefeed = 0
    while (linefeeds > 0 && index < length) {
        if (get(index) == '\n') {
            lastLinefeed = index
            linefeeds--
        }
        index++
    }
    val start = lastLinefeed + maxOf(col, 1)
    if (linefeeds > 0 || start >= length) {

        throw StringIndexOutOfBoundsException("Out of source code")
    }
    return where(start) + if (capture != null) {
        "\n" + "-" * (capture.start) + "^" * (capture.endInclusive - capture.start).let { if (it < 0) 1 else it }
    } else {
        ""
    }
}


fun String.where(start: Int): String {
    val linefeed = indexOf('\n', start)
    val before = lastIndexOf('\n', linefeed - 1)
    return substring(maxOf(before, 0), if(linefeed < 0) length else linefeed)
}
