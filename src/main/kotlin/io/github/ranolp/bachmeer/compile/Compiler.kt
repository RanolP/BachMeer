package io.github.ranolp.bachmeer.compile

import io.github.ranolp.bachmeer.parse.Node

abstract class Compiler(protected val root: Node) {
   abstract fun compile() : ByteArray
}
