package io.github.ranolp.bachmeer.util

import java.util.*


data class Table(val maxLength: Int = 45, val cells: List<Int>) {
    constructor(vararg cells: Int, maxLength: Int = 45) : this(maxLength, cells.toList())

    private val greedyCells = cells.mapIndexedNotNull { index, value ->
        if (value == -1) index else null
    }
    private val notGreedyCells = cells.indices.filterNot { it in greedyCells }
    private val notGreedySum = notGreedyCells.sumBy { cells[it] + 3 }
    private val cellWidthes: List<Int>

    init {
        val check = (notGreedySum + greedyCells.size * 3)
        if (check > maxLength) {
            throw IllegalArgumentException("Length must be bigger than $check")
        }

        cellWidthes = cells.indices.map {
            if (it in greedyCells) {
                (maxLength - notGreedySum) / greedyCells.size - 2 * notGreedyCells.size - 2
            } else {
                cells[it]
            } + 2
        }
    }

    fun header(label: String) {
        val spaces = (maxLength - label.length) / 2 - 1
        println("┌${"─" * (maxLength - 2)}┐")
        println("│${" " * spaces}$label${" " * (maxLength - label.length - spaces - 2)}│")
        val builder = StringJoiner("┬", "├", "┤")
        for (i in cells.indices) {
            builder.add("─" * cellWidthes[i])
        }
        println(builder)
    }

    fun cell(vararg data: String) {
        if (data.size != cells.size) {
            throw IllegalArgumentException("Expected data count is ${cells.size} but received count of data is ${data.size}")
        }
        val builder = StringJoiner("│", "│", "│")
        for (i in cells.indices) {
            builder.add(" " + data[i] + " " * (cellWidthes[i] - data[i].length - 1))
        }
        println(builder)
    }

    fun cell(vararg data: Any?) {
        cell(*data.map { it.toString() }.toTypedArray())
    }

    fun footer() {
        val builder = StringJoiner("┴", "└", "┘")
        for (i in cells.indices) {
            builder.add("─" * cellWidthes[i])
        }
        println(builder)
    }
}
