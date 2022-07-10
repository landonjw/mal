package mal

import mal.MalTypes.KMAL_NIL
import mal.MalTypes.toKMalList
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalFunction
import mal.MalTypes.KMalList
import mal.MalTypes.KMalVector
import mal.MalTypes.KMalHashMap
import mal.MalTypes.asString
import mal.MalTypes.asTypeString
import mal.MalTypes.isTruthy
import mal.MalTypes.toKMalMap
import mal.MalTypes.toKMalVector
import mal.MalTypes.toList
import mal.SpecialForms.createLambda
import mal.SpecialForms.defBinding
import mal.SpecialForms.doFn
import mal.SpecialForms.ifFn
import mal.SpecialForms.letBinding
import mal.Step4IfFnDo.EVAL
import mal.Step4IfFnDo.REP
import java.io.EOFException

object Step4IfFnDo {
    fun READ(input: String): KMalType = Reader.readAST(input)

    fun evalAST(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when (ast) {
            is KMalSymbol -> environment.get(ast)
            is KMalList -> ast.elements.map { EVAL(it, environment) }.toKMalList()
            is KMalVector -> ast.elements.map { EVAL(it, environment) }.toKMalVector()
            is KMalHashMap -> ast.toList().map { EVAL(it, environment) }.toKMalMap()
            else -> ast
        }
    }

    fun checkForSpecialForm(ast: KMalList, environment: KMalEnvironment): KMalType? {
        val first = ast.first as? KMalSymbol ?: return null
        return when (first.value) {
            "def!" -> defBinding(ast, environment)
            "let*" -> letBinding(ast, environment)
            "do" -> doFn(ast, environment)
            "if" -> ifFn(ast, environment)
            "fn*" -> createLambda(ast, environment)
            else -> null
        }
    }

    fun evalFunction(ast: KMalList, environment: KMalEnvironment): KMalType {
        val evaluatedAST = evalAST(ast, environment) as KMalList
        val function = evaluatedAST.first!!
        if (function !is KMalFunction) throw IllegalStateException("expected function, got ${asTypeString(function)}")
        val args = evaluatedAST.rest ?: listOf()
        return function(args)
    }

    fun EVAL(ast: KMalType, environment: KMalEnvironment): KMalType {
        return when {
            ast !is KMalList -> evalAST(ast, environment)
            ast.size == 0 -> ast
            else -> checkForSpecialForm(ast, environment) ?: evalFunction(ast, environment)
        }
    }

    fun PRINT(ast: KMalType): String? = if (ast is KMalIgnorable) null else asString(ast, printReadably = true)

    fun REP(input: String, environment: KMalEnvironment): String? = PRINT(EVAL(READ(input), environment))
}

object SpecialForms {
    fun defBinding(ast: KMalList, environment: KMalEnvironment): KMalType {
        forceParity(ast.elements, atLeast = 2)
        val name = ast.elements[1] as? KMalSymbol ?: throw IllegalStateException("def! requires a symbol for name, got ${asTypeString(ast.elements[1])}")
        val evaluatedValue = EVAL(ast.elements[2], environment)
        environment.set(name, evaluatedValue)
        return evaluatedValue
    }

    fun letBinding(ast: KMalList, environment: KMalEnvironment): KMalType {
        forceParity(ast.elements, atLeast = 3, atMost = 3)
        val childEnvironment = KMalEnvironment(parent = environment)
        val bindings = ast.elements[1] as? KMalSequence ?: throw IllegalStateException("let* requires a sequence of bindings, got ${asTypeString(ast.elements[1])}")
        if (bindings.size % 2 != 0) throw IllegalStateException("bindings must contain an even number of forms")
        for (i in bindings.elements.indices step 2) {
            val bindingSymbol = bindings.elements[i]
            if (bindingSymbol !is KMalSymbol) throw IllegalStateException("expected symbol for binding, got ${asTypeString(bindingSymbol)}")
            val binding = Pair(bindingSymbol, EVAL(bindings.elements[i + 1], childEnvironment))
            childEnvironment.set(binding)
        }
        val letForm = ast.elements[2]
        return EVAL(letForm, childEnvironment)
    }

    fun doFn(ast: KMalList, environment: KMalEnvironment): KMalType {
        val toEvaluate = ast.rest ?: return KMAL_NIL
        return toEvaluate.map { EVAL(it, environment) }.last()
    }

    fun ifFn(ast: KMalList, environment: KMalEnvironment): KMalType {
        forceParity(ast.elements, atLeast = 3, atMost = 4)
        val conditional = EVAL(ast.elements[1], environment)
        val ifBlock = ast.elements[2]
        val elseBlock = if (ast.elements.size == 4) ast.elements[3] else null
        return when (isTruthy(conditional)) {
            true -> EVAL(ifBlock, environment)
            false -> elseBlock?.let { EVAL(it, environment) } ?: KMAL_NIL
        }
    }

    fun isVariadic(bindingSymbols: List<KMalSymbol>): Boolean {
        if (bindingSymbols.size < 2) return false
        val secondLastIndex = bindingSymbols.size - 2
        return bindingSymbols[secondLastIndex].value == "&"
    }

    fun validateVariadic(bindingSymbols: List<KMalSymbol>) {
        val secondLastIndex = bindingSymbols.size - 2
        for (i in bindingSymbols.indices) {
            if (bindingSymbols[i].value == "&" && i != secondLastIndex) {
                throw IllegalStateException("variadic symbol '&' found at position $i, must be second-last argument at $secondLastIndex")
            }
        }
    }

    fun createLambda(ast: KMalList, environment: KMalEnvironment): KMalFunction {
        forceParity(ast.elements, atLeast = 3, atMost = 3)
        val bindings = ast.elements[1]
        if (bindings !is KMalSequence) throw IllegalStateException("bindings must be sequence, got ${asTypeString(bindings)}")
        val bindingsAsSymbols = bindings.elements.map { it as? KMalSymbol ?: throw IllegalStateException("binding must be symbol, got ${asTypeString(it)}") }
        return KMalFunction lambda@{ args ->
            val localEnvironment = KMalEnvironment(parent = environment)
            when {
                isVariadic(bindingsAsSymbols) -> {
                    forceParity(args, atLeast = bindingsAsSymbols.size - 2)
                    validateVariadic(bindingsAsSymbols)
                    for (i in bindingsAsSymbols.indices) {
                        if (bindingsAsSymbols[i].value == "&") {
                            val variadicValues = KMalList(args.subList(i, args.size))
                            localEnvironment.set(bindingsAsSymbols[i + 1], variadicValues)
                            break
                        }
                        else {
                            localEnvironment.set(bindingsAsSymbols[i] to args[i])
                        }
                    }
                }
                else -> {
                    forceParity(args, atLeast = bindingsAsSymbols.size, atMost = bindingsAsSymbols.size)
                    for (i in bindingsAsSymbols.indices) {
                        localEnvironment.set(bindingsAsSymbols[i] to args[i])
                    }
                }
            }
            return@lambda EVAL(ast.elements[2], localEnvironment)
        }
    }
}

fun runStatement(input: String, environment: KMalEnvironment) {
    try {
        REP(input, environment)?.let { println(it) }
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

fun main() {
    val globalEnvironment = KMalEnvironment(null)
    Core.ns.forEach { globalEnvironment.set(it.toPair()) }
    while (true) {
        print("user> ")
        val input = readLine() ?: break
        runStatement(input, globalEnvironment)
    }
//    runTests()
}

fun runTests() {
    val globalEnvironment = KMalEnvironment(null)
    Core.ns.forEach { globalEnvironment.set(it.toPair()) }
    runStatement("(pr-str)", globalEnvironment)
}