package mal

object Step0REPL {
    fun READ(input: String) = input

    fun EVAL(input: String) = input

    fun PRINT(input: String) = input

    fun REP(input: String) = PRINT(EVAL(READ(input)))
}

fun main() {
    while (true) {
        print("user> ")
        val input = readLine() ?: break
        println(Step0REPL.REP(input))
    }
}