package mal

import java.io.EOFException
import mal.MalTypes.KMalList
import mal.MalTypes.KMalVector
import mal.MalTypes.KMalHashMap
import mal.MalTypes.KMalComment
import mal.MalTypes.toKMalList
import mal.MalTypes.toKMalMap
import mal.MalTypes.toKMalVector

class TokenReader(private val tokens: List<String>) {
    private var position = 0

    fun hasNext() = position < tokens.size

    fun peek(): String {
        if (!hasNext()) throw EOFException()
        return tokens[position]
    }

    fun next() = peek().also { position++ }
}

typealias SequenceFactory<T> = (List<KMalType>) -> T

object Reader {
    val regex = """[\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"?|;.*|[^\s\[\]{}('"`,;)]*)""".toRegex()
    val commaRegex = """(?!\B"[^"]*),(?![^"]*"\B)""".toRegex()

    fun tokenize(str: String): List<String> {
        return regex
            .findAll(str)
            .map { it.value.replace(commaRegex, "").trim() }
            .toList()
            .dropLast(1)
    }

    fun <T> readSequence(reader: TokenReader, prefix: String, postfix: String, seqFactory: SequenceFactory<T>): T {
        var token = reader.next()
        if (token != prefix) {
            throw IllegalStateException("expected beginning delimiter '$prefix', got $token")
        }
        val elements = mutableListOf<KMalType>()
        while (reader.hasNext()) {
            token = reader.peek()
            if (token == postfix) {
                return seqFactory(elements)
            }
            else {
                val element = readForm(reader)
                if (element !is KMalIgnorable) {
                    elements.add(element)
                }
                reader.next()
            }
        }
        throw EOFException("EOF reached before sequence ended")
    }

    fun readList(reader: TokenReader): KMalList = readSequence(reader, "(", ")") { it.toKMalList() }
    fun readVector(reader: TokenReader): KMalVector = readSequence(reader, "[", "]") { it.toKMalVector() }
    fun readMap(reader: TokenReader): KMalHashMap = readSequence(reader, "{", "}") { it.toKMalMap() ?: throw IllegalStateException("imbalanced hashmap declaration") }

    val atomicCoercers = listOf(
        ::coerceToNil,
        ::coerceToBoolean,
        ::coerceToInt,
        ::coerceToFloat,
        ::coerceToKeyword,
        ::coerceToString,
        ::coerceToSymbol // Should always be last.
    )

    fun readAtom(reader: TokenReader): KMalType {
        val token = reader.peek()
        for (coercer in atomicCoercers) {
            val coercedToken = coercer(token)
            if (coercedToken != null) {
                return coercedToken
            }
        }
        throw EOFException("could not parse atomic value: $token")
    }

    fun readForm(reader: TokenReader): KMalType {
        val token = reader.peek()
        return when {
            token.startsWith(";") -> KMalComment()
            token == "(" -> readList(reader)
            token == "[" -> readVector(reader)
            token == "{" -> readMap(reader)
            else -> readAtom(reader)
        }
    }

    fun readAST(str: String): KMalType {
        val tokens = tokenize(str)
        val reader = TokenReader(tokens)
        return readForm(reader)
    }
}