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
import mal.MalTypes.toKMalMap
import mal.MalTypes.toList

object Step2Eval {
    fun READ(input: String): KMalType = Reader.readAST(input)

    private fun evalAST(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when (ast) {
            is KMalSymbol -> environment[ast] ?: throw IllegalStateException("symbol $ast is not defined in environment")
            is KMalList -> ast.elements.map { EVAL(it, environment) }.toKMalList()
            is KMalVector -> ast.elements.map { EVAL(it, environment) }.toKMalVector()
            is KMalHashMap -> ast.toList().map { EVAL(it, environment) }.toKMalMap()
            else -> ast
        }
    }

    fun EVAL(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when {
            ast !is KMalList -> evalAST(ast, environment)
            ast.size == 0 -> ast
            else -> lambda@{
                val evaluatedAST = evalAST(ast, environment) as KMalList
                val function = evaluatedAST.first!!
                if (function !is KMalFunction) throw IllegalStateException("expected function, got ${function.toTypeString()}")
                val args = evaluatedAST.rest ?: listOf()
                return function(args)
            }
        }
    }

    fun PRINT(ast: KMalType): String? = if (ast is KMalIgnorable) null else ast.toString()

    fun REP(input: String, environment: KMalEnvironment): String? = PRINT(EVAL(READ(input), environment))
}

fun main() {
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