/**
 * Created by (442377900) on 06-Mar-18.
 */

enum class VM_Command_Type {
    C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL, COMMENT
}

class VMParser(val filename: String) {
    init {
        if (hasMoreCommands())
            advanceToNextCommand()
    }

    private val inputfile: ReadFile = ReadFile(filename)
    private var currentLine: Int = 0
    var currentCommand: String = ""
        private set

    fun hasMoreCommands(): Boolean = (currentLine <= inputfile.lineCount)

    fun advanceToNextCommand() {
        currentLine++
        /*currentCommand = inputfile.getLine(currentLine)
        return currentCommand*/
    }

    fun getCurrentCommandType(): VM_Command_Type {
        return when {
            currentCommand.contains(Regex("^(add|sub|neg|eq|gt|lt|and|or|not) ", RegexOption.IGNORE_CASE)) ->
                VM_Command_Type.C_ARITHMETIC
            currentCommand.contains(Regex("^push ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_PUSH
            currentCommand.contains(Regex("^pop", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_POP
            currentCommand.contains(Regex("^// ")) -> VM_Command_Type.COMMENT
            else -> throw Exception("parsing error on ${currentCommand}")
        }
    }

    // example: 'push argument 0' : arg1 = 'argument', arg2 = '0'

    // caller is responsible for not calling if command type is C_RETURN
    fun getArg1() = currentCommand.split(Regex(" "),3)[1]

    // caller is responsible for only calling if command type is C_PUSH, C_POP, C_FUNCTION, or C_CALL.
    fun getArg2AsInt() = currentCommand.split(Regex(" "),4)[2].toInt()

}