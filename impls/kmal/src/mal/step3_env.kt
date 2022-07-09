package mal

import mal.MalTypes.KMalFunction
import mal.MalTypes.KMalHashMap
import mal.MalTypes.KMalList
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalVector
import mal.MalTypes.toKMalList
import mal.MalTypes.toKMalMap
import mal.MalTypes.toKMalSymbol
import mal.MalTypes.toKMalVector
import mal.MalTypes.toList
import mal.Step3Eval.REP
import java.io.EOFException

object Step3Eval {
    fun READ(input: String): KMalType = Reader.readAST(input)

    private fun evalAST(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when (ast) {
            is KMalSymbol -> environment.get(ast)
            is KMalList -> ast.elements.map { EVAL(it, environment) }.toKMalList()
            is KMalVector -> ast.elements.map { EVAL(it, environment) }.toKMalVector()
            is KMalHashMap -> ast.toList().map { EVAL(it, environment) }.toKMalMap()
            else -> ast
        }
    }

    fun defBinding(ast: KMalList, environment: KMalEnvironment): KMalType {
        if (ast.size < 2) throw IllegalStateException("illegal parity of ${ast.size} offered")
        val name = ast.elements[1] as? KMalSymbol ?: throw IllegalStateException("def! requires a symbol for name, got ${ast.elements[1].toTypeString()}")
        val evaluatedValue = EVAL(ast.elements[2], environment)
        environment.set(name, evaluatedValue)
        return evaluatedValue
    }

    fun letBinding(ast: KMalList, environment: KMalEnvironment): KMalType {
        if (ast.size != 3) throw IllegalStateException("illegal parity of ${ast.size} offered")
        val childEnvironment = KMalEnvironment(parent = environment)
        val bindings = ast.elements[1] as? KMalSequence ?: throw IllegalStateException("let* requires a sequence of bindings, got ${ast.elements[1].toTypeString()}")
        if (bindings.size % 2 != 0) throw IllegalStateException("bindings must contain an even number of forms")
        for (i in bindings.elements.indices step 2) {
            val bindingSymbol = bindings.elements[i]
            if (bindingSymbol !is KMalSymbol) throw IllegalStateException("expected symbol for binding, got ${bindingSymbol.toTypeString()}")
            val binding = Pair(bindingSymbol, EVAL(bindings.elements[i + 1], childEnvironment))
            childEnvironment.set(binding)
        }
        val letForm = ast.elements[2]
        return EVAL(letForm, childEnvironment)
    }

    fun EVAL(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when {
            ast !is KMalList -> evalAST(ast, environment)
            ast.size == 0 -> ast
            else -> when {
                ast.first is KMalSymbol && ast.first.value == "def!" -> defBinding(ast, environment)
                ast.first is KMalSymbol && ast.first.value == "let*" -> letBinding(ast, environment)
                else -> {
                    val evaluatedAST = evalAST(ast, environment) as KMalList
                    val function = evaluatedAST.first!!
                    if (function !is KMalFunction) throw IllegalStateException("expected function, got ${function.toTypeString()}")
                    val args = evaluatedAST.rest ?: listOf()
                    return function(args)
                }
            }
        }
    }

    fun PRINT(ast: KMalType): String? = if (ast is KMalIgnorable) null else ast.toString()

    fun REP(input: String, environment: KMalEnvironment): String? = PRINT(EVAL(READ(input), environment))
}

fun main() {
    val replEnvironment = KMalEnvironment(null).apply {
        this.set(
            "+".toKMalSymbol() to KMalFunction(Library.kmalAdd),
            "-".toKMalSymbol() to KMalFunction(Library.kmalSubtract),
            "*".toKMalSymbol() to KMalFunction(Library.kmalMultiply),
            "/".toKMalSymbol() to KMalFunction(Library.kmalDivide),
            "=".toKMalSymbol() to KMalFunction(Library::kmalEquals),
            "println".toKMalSymbol() to KMalFunction(Library::kmalPrintln)
        )
    }
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
                    println(ex.message)
                }
            }
        }
    }
}
