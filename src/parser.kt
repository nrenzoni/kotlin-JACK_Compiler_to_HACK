/**
 * Created by (442377900) on 06-Mar-18.
 */

enum class VM_Command_Type {
    C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
}

class Parser(val filename: String) {
    init {
        if (hasMoreCommands())
            nextCommand()
    }

    private val inputfile: ReadFile = ReadFile(filename)
    private var currentLine: Int = 0
    var currentCommand: String = ""
        private set

    fun hasMoreCommands(): Boolean = (currentLine <= inputfile.lineCount)

    fun nextCommand(): String {
        currentLine++
        currentCommand = inputfile.getLine(currentLine)
        return currentCommand
    }

    fun getCurrentCommandType(): VM_Command_Type {
        return when {
            currentCommand.contains(Regex("^(add|sub|neg|eq|gt|lt|and|or|not) ", RegexOption.IGNORE_CASE)) ->
                VM_Command_Type.C_ARITHMETIC
            currentCommand.contains(Regex("^push ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_PUSH
            currentCommand.contains(Regex("^pop", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_POP
            else -> throw Exception("parsing error on ${currentCommand}")
        }
    }

    // caller is responsible for not calling if command type is C_RETURN
    fun getArg1() = currentCommand.split(Regex(" "),2)[0]

    // caller is responsible for only calling if command type is C_PUSH, C_POP, C_FUNCTION, or C_CALL.
    fun getArg2() = currentCommand.split(Regex(" "),3)[1]

}