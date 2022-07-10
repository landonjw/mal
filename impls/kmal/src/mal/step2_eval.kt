package mal

import mal.Step2Eval.REP
import java.io.EOFException
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalList
import mal.MalTypes.toKMalList
import mal.MalTypes.KMalFunction
import mal.MalTypes.KMalVector
import mal.MalTypes.toKMalVector
import mal.MalTypes.KMalHashMap
import mal.MalTypes.asString
import mal.MalTypes.asTypeString
import mal.MalTypes.toKMalMap
import mal.MalTypes.toKMalSymbol
import mal.MalTypes.toList

object Step2Eval {
    fun READ(input: String): KMalType = Reader.readAST(input)

    private fun evalAST(ast: KMalType, environment: Map<KMalSymbol, KMalFunction>): KMalType {
        return when (ast) {
            is KMalSymbol -> environment[ast] ?: throw IllegalStateException("symbol $ast is not defined in environment")
            is KMalList -> ast.elements.map { EVAL(it, environment) }.toKMalList()
            is KMalVector -> ast.elements.map { EVAL(it, environment) }.toKMalVector()
            is KMalHashMap -> ast.toList().map { EVAL(it, environment) }.toKMalMap()
            else -> ast
        }
    }

    fun EVAL(ast: KMalType, environment: Map<KMalSymbol, KMalFunction>): KMalType {
        return when {
            ast !is KMalList -> evalAST(ast, environment)
            ast.size == 0 -> ast
            else -> {
                val evaluatedAST = evalAST(ast, environment) as KMalList
                val function = evaluatedAST.first!!
                if (function !is KMalFunction) throw IllegalStateException("expected function, got ${asTypeString(function)}")
                val args = evaluatedAST.rest ?: listOf()
                return function(args)
            }
        }
    }

    fun PRINT(ast: KMalType): String? = if (ast is KMalIgnorable) null else asString(ast, printReadably = true)

    fun REP(input: String, environment: Map<KMalSymbol, KMalFunction>): String? = PRINT(EVAL(READ(input), environment))
}

fun main() {
    val replEnvironment: Map<KMalSymbol, KMalFunction> = mapOf(
        "+".toKMalSymbol() to KMalFunction(Core.kmalAdd),
        "-".toKMalSymbol() to KMalFunction(Core.kmalSubtract),
        "*".toKMalSymbol() to KMalFunction(Core.kmalMultiply),
        "/".toKMalSymbol() to KMalFunction(Core.kmalDivide),
        "=".toKMalSymbol() to KMalFunction(Core::kmalEquals),
        "println".toKMalSymbol() to KMalFunction(Core::kmalPrintln)
    )
    while (true) {
        print("user> ")
        val input = readLine() ?: break
        try {
            REP(input, replEnvironment)?.let { println(it) }
        }
        catch (ex: Exception) {
            when (ex) {
                is EOFException -> {
                    println("EOF")
                }
                else -> {
                    println(ex)
                }
            }
        }
    }
}