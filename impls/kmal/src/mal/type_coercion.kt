package mal

import mal.MalTypes.KMalKeyword
import mal.MalTypes.KMalSymbol
import mal.MalTypes.KMalBoolean
import mal.MalTypes.KMalNil
import mal.MalTypes.KMalNumber
import mal.MalTypes.KMalString
import mal.MalTypes.KMalHashMap
import mal.MalTypes.KMAL_NIL
import mal.MalTypes.toKMalBoolean
import mal.MalTypes.toKMalNumber
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

fun coerceToNumber(str: String): KMalNumber? = str.toDoubleOrNull()?.toKMalNumber()

fun coerceToSymbol(str: String): KMalSymbol = str.toKMalSymbol()

fun coerceToBoolean(str: String): KMalBoolean? = str.toBooleanStrictOrNull()?.toKMalBoolean()

fun coerceToNil(str: String): KMalNil? = when (str) {
    "nil" -> KMAL_NIL
    else -> null
}