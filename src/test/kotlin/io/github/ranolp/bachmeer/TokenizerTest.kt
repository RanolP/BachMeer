package io.github.ranolp.bachmeer

import io.github.ranolp.bachmeer.compile.BatchCompiler
import io.github.ranolp.bachmeer.parse.Parser
import io.github.ranolp.bachmeer.parse.Token
import io.github.ranolp.bachmeer.parse.Tokenizer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class TokenizerTest {
    @Test
    fun yeah() {
        val code = """
            let mut age = 0x1_DEAD;
            let name = 'Ranol';
            println(`My name is ${'$'}{name}`);
            println(`My age is ${'$'}{age}`);
            println('Oh, It was joke ;)');
            age = 1 4;
            println(`My real age is ${'$'}{age}`);
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
        while(true) {
            result = tokenizer.next()
            if (result != null) {
                tokens += result
            } else {
                break
            }
        }

        // tokens.forEach(::println)

        val root = Parser(tokens, code).parse()

        // println(root.debug())

        val compiled = BatchCompiler(root).compile().toString(Charsets.UTF_8)

        println("--------------- COMPILED CODE ---------------")
        val compiledCodes = compiled.split("\n")
        val compiledLinePad = compiledCodes.size.toString().length
        compiledCodes.forEachIndexed { i, str ->
            println("${(i + 1).toString().padStart(compiledLinePad, ' ')} | $str")
        }
        println("---------------------------------------------")

        Files.write(Paths.get("compiled.bat"), compiled.toByteArray(Charsets.ISO_8859_1))
    }
}
