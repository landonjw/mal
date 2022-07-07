package mal

interface KMalType {
    abstract fun toTypeString(): String
}

interface KMalIgnorable : KMalType

object MalTypes {
    fun getSequenceTypes(elements: List<KMalType>) = elements.joinToString(separator = " ") { it.toTypeString() }

    fun getSequenceString(elements: List<KMalType>) = elements.joinToString(separator = " ") { it.toString() }

    // KMal Type Definitions
    class KMalList(val elements: List<KMalType>) : KMalType {
        override fun toString() = "(${getSequenceString(elements)})"
        override fun toTypeString() = "List (${getSequenceTypes(elements)})"
    }

    class KMalVector(val elements: List<KMalType>) : KMalType {
        override fun toString() = "[${getSequenceString(elements)}]"
        override fun toTypeString() = "Vector [${getSequenceTypes(elements)}]"
    }

    class KMalHashMap(val elements: Map<KMalType, KMalType>) : KMalType {
        override fun toString() = "{${getSequenceString(coerceToList(this))}}"
        override fun toTypeString() = "Map {${getSequenceTypes(coerceToList(this))}}"
    }

    open class KMalString(val value: String): KMalType {
        override fun toString() = "\"$value\""
        override fun toTypeString() = "String"
    }

    class KMalSymbol(value: String): KMalString(value) {
        override fun toString() = value
        override fun toTypeString() = "Symbol"
    }

    class KMalKeyword(value: String): KMalType {
        val value: String

        init {
            this.value = "\u029E" + value
        }

        override fun toString() = ":" + value.substring(1)
        override fun toTypeString() = "Keyword"
    }

    class KMalInt(val value: Long): KMalType {
        override fun toString() = value.toString()
        override fun toTypeString() = "Int"
    }

    class KMalFloat(val value: Double): KMalType {
        override fun toString() = value.toString()
        override fun toTypeString() = "Float"
    }

    class KMalBoolean(val value: Boolean): KMalType {
        override fun toString() = value.toString()
        override fun toTypeString() = "Boolean"
    }

    class KMalNil: KMalType {
        override fun toString() = "nil"
        override fun toTypeString() = "Nil"
    }

    class KMalComment: KMalIgnorable {
        override fun toTypeString() = "Comment"
    }

    // KMal Type Constants
    val KMAL_TRUE = KMalBoolean(true)
    val KMAL_FALSE = KMalBoolean(false)
    val KMAL_NIL = KMalNil()
    val KMAL_COMMENT = KMalComment()

    // Extension functions for data type conversions
    fun List<KMalType>.toKMalList() = KMalList(this)
    fun List<KMalType>.toKMalVector() = KMalVector(this)
    fun List<KMalType>.toKMalMap() = coerceToMap(this)
    fun Map<KMalType, KMalType>.toKMalMap() = KMalHashMap(this)
    fun String.toKMalString() = KMalString(this)
    fun String.toKMalSymbol() = KMalSymbol(this)
    fun String.toKMalKeyword() = KMalKeyword(this)
    fun Int.toKMalInt() = KMalInt(this.toLong())
    fun Long.toKMalInt() = KMalInt(this)
    fun Float.toKMalFloat() = KMalFloat(this.toDouble())
    fun Double.toKMalDouble() = KMalFloat(this)
    fun Boolean.toKMalBoolean() = KMalBoolean(this)
}