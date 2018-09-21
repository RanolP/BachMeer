@file:JvmName("BachMeer")

package io.github.ranolp.bachmeer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import io.github.ranolp.bachmeer.platform.Platform
import java.io.File

fun main(args: Array<String>) = GAheuiCliOption().main(args)

class GAheuiCliOption : CliktCommand(
    help = "The BachMeer Transpiler",
    epilog = "This software is free software, its source code under the MIT license can be found at Github.",
    name = "bachmeer"
) {
    val version: Boolean by option(
        "-v", "--version", help = "Print the version of this compiler"
    ).flag()

    val optimize: Int? by option(
        "-O", "--optimize", help = "Set the optimization level of this program"
    ).int().restrictTo(0..2)

    val target: Platform by option(
        "-o", "--output", help = """Set the type of output.
            use bat: Batch (Windows);\n
            use sh: Shell Script (Linux)\n"""
    ).choice(
        "bat" to Platform.BATCH, "sh" to Platform.BASH, "ps" to Platform.POWER_SHELL
    ).default(Platform.BATCH)

    val path: File? by argument("PATH").file(
        exists = true, fileOkay = true, folderOkay = false, readable = true
    ).optional()

    override fun run() {
        if (version) {
            TermUi.echo("BachMeer 0.1.0-SNAPSHOT 2018-09-21")
            return
        }
        TermUi.echo("Target Platform : $target")
        TermUi.echo("아직 구현 안 되었읍니다.")
    }
}
