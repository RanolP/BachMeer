package io.github.ranolp.bachmeer

import com.importre.crayon.bgBlue
import com.importre.crayon.bgBrightBlue
import com.importre.crayon.bgBrightYellow
import com.importre.crayon.bold
import com.importre.crayon.brightBlue
import com.importre.crayon.brightGreen
import com.importre.crayon.brightMagenta
import com.importre.crayon.green
import com.importre.crayon.red
import io.github.ranolp.bachmeer.compile.Compiler
import io.github.ranolp.bachmeer.compile.CompilerOption
import io.github.ranolp.bachmeer.parse.Parser
import io.github.ranolp.bachmeer.parse.Tokenizer
import io.github.ranolp.bachmeer.util.Symbols
import io.github.ranolp.bachmeer.util.TimeChecker
import io.github.ranolp.bachmeer.util.tag
import io.github.ranolp.bachmeer.util.times
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

private val root = Paths.get(ClassLoader.getSystemClassLoader().getResource("testcase").toURI())
private val dist = root.parent.resolve("dist").also {
    println(tag("INFO").bgBrightBlue() + " Distribution Path : $it")
}

class TestCase(val name: String, val ext: String, val path: Path) {
    companion object {
        val LABEL_READ_SOURCE = "TestCase::ReadSource"
        val LABEL_COMPILE = "TestCase::Compile"
    }

    fun check(compiler: Compiler) {
        val checker = TimeChecker()
        val source = checker.does(LABEL_READ_SOURCE) {
            Files.readAllBytes(path).toString(Charsets.UTF_8)
        }

        try {
            val result = checker.does(LABEL_COMPILE) {
                compiler.compile(Parser(Tokenizer(source).tokens()).parse(), true)
            }

            val path = dist.resolve("$name.$ext")
            Files.createDirectories(path.parent)
            if (!path.toFile().exists()) {
                Files.createFile(path)
            }
            Files.write(path, result.toByteArray())

            println(
                "  ${Symbols.tick} $name".green() + " (all=" + "${checker.get(
                    TimeChecker.LABEL_GENERAL
                )}s".brightBlue() + ", compile=" + "${checker.get(LABEL_COMPILE)}s".brightBlue() + ")"
            )
        } catch (th: Throwable) {
            println(
                "  ${Symbols.cross} $name".red() + " (${th.message})"
            )
        }
    }
}

internal fun getTestCases(namespace: String, extension: String): List<TestCase> {
    return Files.newDirectoryStream(root.resolve(namespace)).map { path ->
        val nameCount = path.nameCount - root.nameCount - 1
        val name = (0..nameCount).map {
            path.getName(path.nameCount - it - 1)
        }.reversed().joinToString("/").let {
            it.substring(0, it.lastIndexOf('.'))
        }
        TestCase(name, extension, path)
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CompilerTest(private val compiler: Compiler) {
    val extension = compiler.option.resultExtension ?: compiler.platform.extension
    @BeforeAll
    fun init() {
        // for print
        dist
        println()
        println(tag("INFO").bgBrightBlue() + " Compiler Options")
        val DefaultValue = object {}

        data class CompilerOptionGetter(val name: String, val getter: CompilerOption.() -> Any?)

        fun formatData(data: Any?, depth: Int): String {
            return "  " * depth + when (data) {
                is String -> "string".brightBlue() + " " + data.bgBrightYellow()
                is Boolean -> if (data) "true".green().bold() else "false".red().bold()
                is Number -> data.toString().brightBlue()
                DefaultValue -> "(Default)".brightGreen()
                null -> "null".red()
                else -> data::class.qualifiedName ?: "Unknown"
            }
        }

        val options = listOf(CompilerOptionGetter("Magic Lookup") { useMagicLookup },
            CompilerOptionGetter("Result Extension") { resultExtension ?: DefaultValue })
        val labelMax = options.maxBy { it.name.length }!!.name.length + 2

        for (option in options) {
            println(
                String.format(
                    "%${labelMax - option.name.length}s", " "
                ) + option.name.brightMagenta() + " : " + formatData(option.getter(compiler.option), 0)
            )
        }
    }

    private fun test(namespace: String) {
        println(tag("TEST").bgBlue() + " $namespace")
        for (case in getTestCases(namespace, extension)) {
            case.check(compiler)
        }
    }

    @Test
    fun lang() = test("lang")

    @Test
    fun stdlib() = test("stdlib")
}
