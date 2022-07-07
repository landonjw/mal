package mal

import mal.MalTypes.KMalKeyword
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalBoolean
import mal.MalTypes.KMalNil
import mal.MalTypes.KMalInt
import mal.MalTypes.KMalFloat
import mal.MalTypes.KMalString
import mal.MalTypes.KMalHashMap
import mal.MalTypes.KMAL_NIL
import mal.MalTypes.toKMalBoolean
import mal.MalTypes.toKMalFloat
import mal.MalTypes.toKMalInt
import mal.MalTypes.toKMalString
import mal.MalTypes.toKMalSymbol
import java.io.EOFException

fun coerceToKeyword(str: String): KMalKeyword? {
    if (!str.startsWith(":")) return null
    val keywordName = str.substring(1)
    return KMalKeyword(keywordName)
}

fun coerceToString(str: String): KMalString? {
    if (str.startsWith("\"")) {
        if (!str.endsWith("\"") || str.length == 1) throw EOFException("value started with quote but was not enclosed: $str")
        val stringContents = str.substring(1, str.length - 1)
        return stringContents.toKMalString()
    }
    else {
        return null
    }
}

fun coerceToInt(str: String): KMalInt? = str.toLongOrNull()?.toKMalInt()

fun coerceToFloat(str: String): KMalFloat? = str.toFloatOrNull()?.toKMalFloat()

fun coerceToSymbol(str: String): KMalSymbol = str.toKMalSymbol()

fun coerceToBoolean(str: String): KMalBoolean? = str.toBooleanStrictOrNull()?.toKMalBoolean()

fun coerceToNil(str: String): KMalNil? = when (str) {
    "nil" -> KMAL_NIL
    else -> null
}

fun coerceToMap(list: List<KMalType>): KMalHashMap? {
    if (list.size % 2 != 0) return null
    val elements = mutableMapOf<KMalType, KMalType>()
    for (i in list.indices step 2) {
        elements[list[i]] = list[i + 1]
    }
    return KMalHashMap(elements)
}

fun coerceToList(map: KMalHashMap): List<KMalType> {
    val elements = mutableListOf<KMalType>()
    map.elements.entries.forEach {
        elements.add(it.key)
        elements.add(it.value)
    }
    return elements
}