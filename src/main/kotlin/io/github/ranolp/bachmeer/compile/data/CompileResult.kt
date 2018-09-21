package io.github.ranolp.bachmeer.compile.data

import io.github.ranolp.bachmeer.compile.Compiler


sealed class CompileResult(val preCode: String, val code: String) {
    companion object {
        fun Auto(preCode: String? = null, code: String? = null): CompileResult {
            return if (preCode == null || preCode.isEmpty()) {
                if (code == null || code.isEmpty()) Nop else Simple(code)
            } else {
                if (code == null || code.isEmpty()) Simple(preCode) else Complex(preCode, code)
            }
        }
    }

    val toPair: Pair<String, String> by lazy { Pair(preCode, code) }

    operator fun component1(): String = preCode

    operator fun component2(): String = code

    abstract fun asCode(compiler: Compiler): String

    open fun withPreCode(code: String): CompileResult = Complex(
        code + if (preCode.isNotEmpty()) "\n$preCode" else "", this.code
    )

    class Complex(preCode: String, code: String) : CompileResult(preCode, code) {
        constructor(pair: Pair<String, String>) : this(pair.first, pair.second)

        override fun asCode(compiler: Compiler): String = "$preCode\n$code"

        override fun toString(): String {
            return "CompileResult.Complex(\n\t$preCode\n;\n\t$code\n)"
        }
    }

    class Simple(code: String) : CompileResult("", code) {

        constructor(data: Any?) : this(data.toString())

        override fun asCode(compiler: Compiler): String = code

        override fun toString(): String {
            return "CompileResult.Simple(\n\t$code\n)"
        }
    }

    class Data(preCode: String, val bmObject: BMObject) : CompileResult(preCode, "") {
        constructor(bmObject: BMObject) : this("", bmObject)

        override fun withPreCode(code: String): CompileResult.Data = Data(
            code + if (preCode.isNotEmpty()) "\n$preCode" else "", bmObject
        )

        override fun asCode(compiler: Compiler): String {
            return this.preCode + "\n" + bmObject.compile(compiler).asCode(compiler)
        }

        fun compile(compiler: Compiler): CompileResult = bmObject.compile(compiler).withPreCode(preCode)
    }

    object Nop : CompileResult("", "") {
        override fun asCode(compiler: Compiler): String = ""

        override fun toString(): String = "CompileResult.Nop"
    }
}
