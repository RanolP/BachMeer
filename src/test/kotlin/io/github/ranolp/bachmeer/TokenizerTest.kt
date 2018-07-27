package io.github.ranolp.bachmeer

import io.github.ranolp.bachmeer.compile.BatchCompiler
import io.github.ranolp.bachmeer.parse.Parser
import io.github.ranolp.bachmeer.parse.Token
import io.github.ranolp.bachmeer.parse.Tokenizer
import kotlin.test.Test

class TokenizerTest {
    @Test
    fun yeah() {
        val code = """
            let mut age = 0xCAFE_DEAD;
            let name = 'Ranol';
            print(`My name is ${'$'}{name}`);
            print(`My age is ${'$'}{age}`);
            print('Oh, It was joke ;)');
            age = 1 4;
            print(`My real age is ${'$'}{age}`);
        """.trimIndent()

        println("--------------- ORIGINAL CODE ---------------")
        val codes = code.split("\n")
        val linePad = codes.size.toString().length
        codes.forEachIndexed { i, str ->
            println("${(i + 1).toString().padStart(linePad, ' ')} | $str")
        }
        println("---------------------------------------------")

        val tokenizer = Tokenizer(code)

        var result: Token?
        val tokens = mutableListOf<Token>()
        do {
            result = tokenizer.next()
            if (result != null) {
                tokens += result
            } else {
                break
            }
        } while (true)

        // tokens.forEach(::println)

        val root = Parser(tokens, code).parse()

        // println(root.debug())

        val compiled = BatchCompiler(root).compile().toString()

        println("--------------- COMPILED CODE ---------------")
        val compiledCodes = compiled.split("\n")
        val compiledLinePad = compiledCodes.size.toString().length
        compiled.forEachIndexed { i, str ->
            println("${(i + 1).toString().padStart(compiledLinePad, ' ')} | $str")
        }
        println("---------------------------------------------")
    }
}
