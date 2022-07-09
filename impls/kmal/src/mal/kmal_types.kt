package mal

interface KMalType {
    fun toTypeString(): String
}

interface KMalSequence : KMalType {
    val elements: List<KMalType>
    val first: KMalType?
    val rest: List<KMalType>?
    val size: Int
}

interface KMalIgnorable : KMalType

object MalTypes {
    fun getSequenceTypes(elements: List<KMalType>) = elements.joinToString(separator = " ") { it.toTypeString() }

    fun getSequenceString(elements: List<KMalType>) = elements.joinToString(separator = " ") { it.toString() }

    // KMal Type Definitions
    class KMalFunction(val fn: (List<KMalType>) -> KMalType): KMalType {
        override fun toTypeString() = "Function"
        operator fun invoke(types: List<KMalType>) = fn(types)
        operator fun invoke(vararg types: KMalType) = invoke(types.toList())
    }

    class KMalList(override val elements: List<KMalType>) : KMalSequence {
        override fun toString() = "(${getSequenceString(elements)})"
        override fun toTypeString() = "List (${getSequenceTypes(elements)})"
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalList && other.elements == elements
        override val first = elements.firstOrNull()
        override val rest = if (elements.size >= 2) elements.subList(1, elements.size) else null
        override val size = elements.size
    }

    class KMalVector(override val elements: List<KMalType>) : KMalSequence {
        override fun toString() = "[${getSequenceString(elements)}]"
        override fun toTypeString() = "Vector [${getSequenceTypes(elements)}]"
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalVector && other.elements == elements
        override val first = elements.firstOrNull()
        override val rest = if (elements.size >= 2) elements.subList(1, elements.size) else null
        override val size = elements.size
    }

    class KMalHashMap(val elements: Map<KMalType, KMalType>) : KMalType {
        override fun toString() = "{${getSequenceString(this.toList())}}"
        override fun toTypeString() = "Map {${getSequenceTypes(this.toList())}}"
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalHashMap && other.elements == elements
    }

    open class KMalString(val value: String): KMalType {
        override fun toString() = "\"$value\""
        override fun toTypeString() = "String"
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalString && other.value == value
    }

    class KMalSymbol(value: String): KMalString(value) {
        override fun toString() = value
        override fun toTypeString() = "Symbol"
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?) = other is KMalSymbol && other.value == value
    }

    class KMalKeyword(value: String): KMalType {
        val value: String

        init {
            this.value = "\u029E" + value
        }

        override fun toString() = ":" + value.substring(1)
        override fun toTypeString() = "Keyword"
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalKeyword && other.value == value
    }

    class KMalNumber(val value: Long): KMalType {
        override fun toString() = value.toString()
        override fun toTypeString() = "Number"
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalNumber && other.value == value
    }

    class KMalBoolean(val value: Boolean): KMalType {
        override fun toString() = value.toString()
        override fun toTypeString() = "Boolean"
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalBoolean && other.value == value
    }

    class KMalNil: KMalType {
        override fun toString() = "nil"
        override fun toTypeString() = "Nil"
        override fun hashCode() = "nil".hashCode()
        override fun equals(other: Any?) = other is KMalNil
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
    fun List<KMalType>.toKMalMap(): KMalHashMap {
        if (this.size % 2 != 0) throw IllegalStateException("map must contain an even number of forms")
        val elements = mutableMapOf<KMalType, KMalType>()
        for (i in this.indices step 2) {
            elements[this[i]] = this[i + 1]
        }
        return KMalHashMap(elements)
    }
    fun KMalVector.toKMalMap(): KMalHashMap = this.elements.toKMalMap()
    fun Map<KMalType, KMalType>.toKMalMap() = KMalHashMap(this)
    fun String.toKMalString() = KMalString(this)
    fun String.toKMalSymbol() = KMalSymbol(this)
    fun String.toKMalKeyword() = KMalKeyword(this)
    fun Int.toKMalNumber() = KMalNumber(this.toLong())
    fun Long.toKMalNumber() = KMalNumber(this.toLong())
    fun Float.toKMalNumber() = KMalNumber(this.toLong())
    fun Double.toKMalNumber() = KMalNumber(this.toLong())
    fun Boolean.toKMalBoolean() = KMalBoolean(this)
    fun KMalHashMap.toList(): List<KMalType> {
        val elements = mutableListOf<KMalType>()
        this.elements.entries.forEach {
            elements.add(it.key)
            elements.add(it.value)
        }
        return elements
    }
    fun KMalHashMap.toKMalVector(): KMalVector = KMalVector(this.toList())
}