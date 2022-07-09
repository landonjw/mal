package mal

import mal.MalTypes.KMalSymbol

class KMalEnvironment(val parent: KMalEnvironment?) {
    private val mappings: MutableMap<KMalSymbol, KMalType> = mutableMapOf()

    fun set(symbol: KMalSymbol, value: KMalType) {
        mappings[symbol] = value
    }

    fun set(vararg pair: Pair<KMalSymbol, KMalType>) = pair.forEach { set(it.first, it.second) }

    fun find(symbol: KMalSymbol): KMalType? {
        return when (val value = mappings[symbol]) {
            null -> parent?.find(symbol)
            else -> value
        }
    }

    fun get(symbol: KMalSymbol): KMalType = find(symbol) ?: throw IllegalStateException("$symbol not found.")
}