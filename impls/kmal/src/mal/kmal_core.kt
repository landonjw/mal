package mal

import mal.MalTypes.KMAL_FALSE
import mal.MalTypes.KMAL_NIL
import mal.MalTypes.KMAL_TRUE
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalNil
import mal.MalTypes.KMalBoolean
import mal.MalTypes.KMalList
import mal.MalTypes.KMalFunction
import mal.MalTypes.toKMalNumber
import mal.MalTypes.KMalNumber
import mal.MalTypes.KMalString
import mal.MalTypes.asString
import mal.MalTypes.asTypeString
import mal.MalTypes.isTruthy
import mal.MalTypes.toKMalFunction
import mal.MalTypes.toKMalString
import mal.MalTypes.toKMalSymbol

fun forceParity(args: List<KMalType>, atLeast: Int = 0, atMost: Int = -1) {
    if (args.size < atLeast) throw IllegalArgumentException("illegal parity of ${args.size} offered")
    if (atMost != -1 && args.size > atMost) throw IllegalArgumentException("illegal parity of ${args.size} offered")
}

typealias KMalNamespace = MutableMap<KMalSymbol, KMalFunction>

object Core {
    private fun arithmeticAggregate(arithmeticName: String, arithmetic: (KMalNumber, KMalNumber) -> KMalNumber): (List<KMalType>) -> KMalType {
        return lambda@{ args ->
            forceParity(args, atLeast = 2)
            return@lambda args.reduce { acc, element -> arithmeticOperation(arithmeticName, arithmetic, acc, element) }
        }
    }

    private fun arithmeticOperation(arithmeticName: String, arithmetic: (KMalNumber, KMalNumber) -> KMalNumber, operand1: KMalType, operand2: KMalType): KMalType {
        return when {
            operand1 is KMalNumber && operand2 is KMalNumber -> arithmetic(operand1, operand2)
            else -> throw IllegalArgumentException("cannot apply $arithmeticName to given types: ${asTypeString(operand1)} and ${asTypeString(operand2)}")
        }
    }

    val kmalAdd: (List<KMalType>) -> KMalType = arithmeticAggregate("addition") { a, b -> (a.value + b.value).toKMalNumber() }
    val kmalSubtract: (List<KMalType>) -> KMalType = arithmeticAggregate("subtraction") { a, b -> (a.value - b.value).toKMalNumber() }
    val kmalMultiply: (List<KMalType>) -> KMalType = arithmeticAggregate("multiplication") { a, b -> (a.value * b.value).toKMalNumber() }
    val kmalDivide: (List<KMalType>) -> KMalType = arithmeticAggregate("division") { a, b -> (a.value / b.value).toKMalNumber() }

    fun kmalEquals(args: List<KMalType>): KMalType {
        forceParity(args, atLeast = 2)
        val first = args.first()
        for (i in 1 until args.size) {
            if (args[i] != first) {
                return KMAL_FALSE
            }
        }
        return KMAL_TRUE
    }

    fun kmalPrintln(args: List<KMalType>): KMalNil {
        when (args.size) {
            0 -> println()
            else -> {
                val str = args.joinToString(separator = " ") { asString(it, printReadably = false) }
                val cleanedStr = str
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\")
                println(cleanedStr)
            }
        }
        return KMAL_NIL
    }

    fun kmalPrn(args: List<KMalType>): KMalNil {
        if (args.isEmpty()) {
            println()
            return KMAL_NIL
        }
        else {
            val asString = args.joinToString(" ") { asString(it, printReadably = true) }
            println(asString)
            return KMAL_NIL
        }
    }

    private fun kmalPrStrSingle(arg: KMalType): String {
        return asString(arg, printReadably = true)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    fun kmalPrStr(args: List<KMalType>): KMalString {
        return when (args.size) {
            0 -> KMalString("")
            else -> args.joinToString(separator = " ") { kmalPrStrSingle(it) }.toKMalString()
        }
    }

    fun kmalStr(args: List<KMalType>): KMalString {
        return args.joinToString(separator = "") { asString(it, printReadably = false) }.toKMalString()
    }

    fun toList(args: List<KMalType>): KMalList {
        return KMalList(args)
    }

    fun isList(args: List<KMalType>): KMalBoolean {
        forceParity(args, atLeast = 1, atMost = 1)
        return when {
            args.first() is KMalList -> KMAL_TRUE
            else -> KMAL_FALSE
        }
    }

    fun isEmpty(args: List<KMalType>): KMalBoolean {
        forceParity(args, atLeast = 1, atMost = 1)
        val arg = args.first()
        return when {
            arg is KMalSequence && arg.size == 0 -> KMAL_TRUE
            else -> KMAL_FALSE
        }
    }

    fun count(args: List<KMalType>): KMalNumber {
        forceParity(args, atLeast = 1, atMost = 1)
        val arg = args.first()
        return when {
            arg is KMalSequence -> arg.size.toKMalNumber()
            else -> 0.toKMalNumber()
        }
    }

    fun compareSequence(args: List<KMalType>, predicate: (KMalNumber, KMalNumber) -> Boolean): KMalBoolean {
        forceParity(args, atLeast = 2)
        for (i in 0 until args.size - 1) {
            val first = args[i] as? KMalNumber ?: throw IllegalStateException("expected type Number, got ${asTypeString(args[0])}")
            val second = args[i + 1] as? KMalNumber ?: throw IllegalStateException("expected type Number, got ${asTypeString(args[1])}")
            if (!predicate(first, second)) {
                return KMAL_FALSE
            }
        }
        return KMAL_TRUE
    }

    fun greaterThan(args: List<KMalType>) = compareSequence(args) { a, b -> a.value > b.value }

    fun greaterThanOrEqual(args: List<KMalType>) = compareSequence(args) { a, b -> a.value >= b.value }

    fun lessThan(args: List<KMalType>) = compareSequence(args) { a, b -> a.value < b.value }

    fun lessThanOrEqual(args: List<KMalType>) = compareSequence(args) { a, b -> a.value <= b.value }

    fun not(args: List<KMalType>): KMalBoolean {
        forceParity(args, atLeast = 1, atMost = 1)
        val arg = args.first()
        return if (isTruthy(arg)) KMAL_FALSE else KMAL_TRUE
    }

    fun and(args: List<KMalType>): KMalType {
        if (args.isEmpty()) return KMAL_TRUE
        for (arg in args) {
            if (!isTruthy(arg)) return arg
        }
        return args.last()
    }

    fun or(args: List<KMalType>): KMalType {
        if (args.isEmpty()) return KMAL_NIL
        for (arg in args) {
            if (isTruthy(arg)) return arg
        }
        return args.last()
    }

    val ns: KMalNamespace = mutableMapOf(
        "+".toKMalSymbol() to kmalAdd.toKMalFunction(),
        "-".toKMalSymbol() to kmalSubtract.toKMalFunction(),
        "*".toKMalSymbol() to kmalMultiply.toKMalFunction(),
        "/".toKMalSymbol() to kmalDivide.toKMalFunction(),
        "println".toKMalSymbol() to ::kmalPrintln.toKMalFunction(),
        "prn".toKMalSymbol() to ::kmalPrn.toKMalFunction(),
        "pr-str".toKMalSymbol() to ::kmalPrStr.toKMalFunction(),
        "str".toKMalSymbol() to ::kmalStr.toKMalFunction(),
        "list".toKMalSymbol() to ::toList.toKMalFunction(),
        "list?".toKMalSymbol() to ::isList.toKMalFunction(),
        "empty?".toKMalSymbol() to ::isEmpty.toKMalFunction(),
        "count".toKMalSymbol() to ::count.toKMalFunction(),
        "=".toKMalSymbol() to ::kmalEquals.toKMalFunction(),
        ">".toKMalSymbol() to ::greaterThan.toKMalFunction(),
        ">=".toKMalSymbol() to ::greaterThanOrEqual.toKMalFunction(),
        "<".toKMalSymbol() to ::lessThan.toKMalFunction(),
        "<=".toKMalSymbol() to ::lessThanOrEqual.toKMalFunction(),
        "not".toKMalSymbol() to ::not.toKMalFunction(),
        "and".toKMalSymbol() to ::and.toKMalFunction(),
        "or".toKMalSymbol() to ::or.toKMalFunction()
    )
}