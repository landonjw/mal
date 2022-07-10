package mal

interface KMalType

interface KMalSequence : KMalType {
    val elements: List<KMalType>
    val first: KMalType?
    val rest: List<KMalType>?
    val size: Int
}

interface KMalIgnorable : KMalType

object MalTypes {

    fun asString(type: KMalType, printReadably: Boolean = false): String {
        return when (type) {
            is KMalFunction -> "#<function>"
            is KMalList -> "(${sequenceAsString(type.elements, printReadably)})"
            is KMalVector -> "[${sequenceAsString(type.elements, printReadably)}]"
            is KMalHashMap -> "{${sequenceAsString(type.toList(), printReadably)}}"
            is KMalString ->
                if (printReadably) {
                    "\"${type.value}\""
                }
                else {
                    type.value
                }
            is KMalSymbol -> type.value
            is KMalKeyword -> ":${type.value.substring(1)}"
            is KMalNumber -> type.value.toString()
            is KMalBoolean -> type.value.toString()
            is KMalNil -> "nil"
            is KMalComment -> ""
            else -> throw NotImplementedError()
        }
    }

    fun asTypeString(type: KMalType): String {
        return when (type) {
            is KMalFunction -> "Function"
            is KMalList -> "List (${sequenceAsTypeString(type.elements)})"
            is KMalVector -> "Vector [${sequenceAsTypeString(type.elements)}]"
            is KMalHashMap -> "Map {${sequenceAsTypeString(type.toList())}}"
            is KMalString -> "String"
            is KMalSymbol -> "Symbol"
            is KMalKeyword -> "Keyword"
            is KMalNumber -> "Number"
            is KMalBoolean -> "Boolean"
            is KMalNil -> "Nil"
            is KMalComment -> "Comment"
            else -> throw NotImplementedError()
        }
    }

    fun sequenceAsString(elements: List<KMalType>, printReadably: Boolean): String {
        return elements.joinToString(separator = " ") { asString(it, printReadably) }
    }

    fun sequenceAsTypeString(elements: List<KMalType>): String {
        return elements.joinToString(separator = " ") { asTypeString(it) }
    }

    fun isTruthy(value: KMalType): Boolean {
        return when (value) {
            is KMalNil -> false
            is KMalBoolean -> value.value
            else -> true
        }
    }

    // KMal Type Definitions
    class KMalFunction(val fn: (List<KMalType>) -> KMalType): KMalType {
        operator fun invoke(types: List<KMalType>) = fn(types)
        operator fun invoke(vararg types: KMalType) = invoke(types.toList())
    }

    class KMalList(override val elements: List<KMalType>) : KMalSequence {
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalSequence && other.elements == elements
        override val first = elements.firstOrNull()
        override val rest = if (elements.size >= 2) elements.subList(1, elements.size) else null
        override val size = elements.size
    }

    class KMalVector(override val elements: List<KMalType>) : KMalSequence {
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalSequence && other.elements == elements
        override val first = elements.firstOrNull()
        override val rest = if (elements.size >= 2) elements.subList(1, elements.size) else null
        override val size = elements.size
    }

    class KMalHashMap(val elements: Map<KMalType, KMalType>) : KMalType {
        override fun hashCode() = elements.hashCode()
        override fun equals(other: Any?) = other is KMalHashMap && other.elements == elements
    }

    open class KMalString(val value: String): KMalType {
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalString && other.value == value
    }

    class KMalSymbol(val value: String): KMalType {
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?) = other is KMalSymbol && other.value == value
    }

    class KMalKeyword(value: String): KMalType {
        val value: String

        init {
            this.value = "\u029E" + value
        }

        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalKeyword && other.value == value
    }

    class KMalNumber(val value: Long): KMalType {
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalNumber && other.value == value
    }

    class KMalBoolean(val value: Boolean): KMalType {
        override fun hashCode() = value.hashCode()
        override fun equals(other: Any?) = other is KMalBoolean && other.value == value
    }

    class KMalNil: KMalType {
        override fun hashCode() = "nil".hashCode()
        override fun equals(other: Any?) = other is KMalNil
    }

    class KMalComment: KMalIgnorable

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
    fun ((List<KMalType>) -> KMalType).toKMalFunction(): KMalFunction = KMalFunction(this)
}