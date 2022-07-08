package mal

import mal.Library.kmalAdd
import mal.Library.kmalDivide
import mal.Library.kmalEquals
import mal.Library.kmalMultiply
import mal.Library.kmalPrintln
import mal.Library.kmalSubtract
import mal.MalTypes.KMalFunction
import mal.MalTypes.KMalSymbol
import mal.MalTypes.toKMalSymbol

typealias KMalEnvironment = MutableMap<KMalSymbol, KMalType>

val replEnvironment: KMalEnvironment = mutableMapOf(
    "+".toKMalSymbol() to KMalFunction(kmalAdd),
    "-".toKMalSymbol() to KMalFunction(kmalSubtract),
    "*".toKMalSymbol() to KMalFunction(kmalMultiply),
    "/".toKMalSymbol() to KMalFunction(kmalDivide),
    "=".toKMalSymbol() to KMalFunction(::kmalEquals),
    "println".toKMalSymbol() to KMalFunction(::kmalPrintln)
)