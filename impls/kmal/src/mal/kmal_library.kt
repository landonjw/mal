package mal

import mal.MalTypes.KMAL_FALSE
import mal.MalTypes.KMAL_NIL
import mal.MalTypes.KMAL_TRUE
import mal.MalTypes.toKMalNumber
import mal.MalTypes.KMalNumber
import mal.MalTypes.KMalString
import mal.MalTypes.KMalBoolean

object Library {
    private fun forceParity(args: List<KMalType>, atLeast: Int = 0, atMost: Int = -1) {
        if (args.size < atLeast) throw IllegalArgumentException("illegal parity of ${args.size} offered")
        if (atMost != -1 && args.size > atMost) throw IllegalArgumentException("illegal parity of ${args.size} offered")
    }

    private fun arithmeticAggregate(arithmeticName: String, arithmetic: (KMalNumber, KMalNumber) -> KMalNumber): (List<KMalType>) -> KMalType {
        return lambda@{ args ->
            forceParity(args, atLeast = 2)
            return@lambda args.reduce { acc, element -> arithmeticOperation(arithmeticName, arithmetic, acc, element) }
        }
    }

    private fun arithmeticOperation(arithmeticName: String, arithmetic: (KMalNumber, KMalNumber) -> KMalNumber, operand1: KMalType, operand2: KMalType): KMalType {
        return when {
            operand1 is KMalNumber && operand2 is KMalNumber -> arithmetic(operand1, operand2)
            else -> throw IllegalArgumentException("cannot apply $arithmeticName to given types: ${operand1.toTypeString()} and ${operand2.toTypeString()}")
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

    fun kmalPrintln(args: List<KMalType>): KMalType {
        when (args.size) {
            0 -> println()
            1 -> {
                val arg = args.first()
                when {
                    arg is KMalString -> println(arg.value)
                    else -> println(arg)
                }
            }
            else -> args.forEach { println(it) }
        }
        return KMAL_NIL
    }
}