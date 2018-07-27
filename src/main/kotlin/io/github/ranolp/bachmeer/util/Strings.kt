package io.github.ranolp.bachmeer.util

operator fun String.times(value: Byte): String {
    val result = StringBuilder()
    for (i in 1..value) {
        result.append(this)
    }
    return result.toString()
}

operator fun String.times(value: Short): String {
    val result = StringBuilder()
    for (i in 1..value) {
        result.append(this)
    }
    return result.toString()
}

operator fun String.times(value: Int): String {
    val result = StringBuilder()
    for (i in 1..value) {
        result.append(this)
    }
    return result.toString()
}

operator fun String.times(value: Long): String {
    val result = StringBuilder()
    for (i in 1..value) {
        result.append(this)
    }
    return result.toString()
}
