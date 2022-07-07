package mal

import mal.Reader.readAST
import mal.Step1ReadPrint.REP
import java.io.EOFException

object Step1ReadPrint {
    fun READ(input: String): KMalType = readAST(input)

    fun EVAL(ast: KMalType): KMalType = ast

    fun PRINT(ast: KMalType): String? = if (ast is KMalIgnorable) null else ast.toString()

    fun REP(input: String): String? = PRINT(EVAL(READ(input)))
}

fun main() {
    while (true) {
        print("user> ")
        val input = readLine() ?: break
        try {
            REP(input)?.let { println(it) }
        }
        catch (ex: EOFException) {
            println("EOF")
        }
    }
}