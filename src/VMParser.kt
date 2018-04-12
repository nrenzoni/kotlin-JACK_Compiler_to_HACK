/**
 * Created by (442377900) on 06-Mar-18.
 */

enum class VM_Command_Type {
    C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF_GOTO, C_FUNCTION, C_RETURN, C_CALL, COMMENT
}

// module for parsing file line by line. does not run by itself, line advancement is called from translator module
class VMParser(val filename: String) {

    private val inputfile: ReadFile = ReadFile(filename)
    private var currentLine: Int = 1
    var currentCommand: String = inputfile.getLine(1)
        get() {
            return inputfile.getLine(currentLine)
        }
        private set

    fun hasMoreCommands(): Boolean = (currentLine < inputfile.lineCount)

    fun advanceToNextCommand() {
        currentLine++
        /*currentCommand = inputfile.getLine(currentLine)
        return currentCommand*/
    }

    fun getCurrentCommandType(): VM_Command_Type {
        return when {
            currentCommand.contains(Regex("^(add|sub|neg|eq|gt|lt|and|or|not)", RegexOption.IGNORE_CASE)) ->
                VM_Command_Type.C_ARITHMETIC
            currentCommand.contains(Regex("^push ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_PUSH
            currentCommand.contains(Regex("^pop ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_POP
            currentCommand.contains(Regex("^label ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_LABEL
            currentCommand.contains(Regex("^goto ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_GOTO
            currentCommand.contains(Regex("^if-goto ", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_IF_GOTO
            currentCommand.contains(Regex("^function [a-z._]+ [1-9][0-9]*&", RegexOption.IGNORE_CASE)) ->
                VM_Command_Type.C_FUNCTION
            currentCommand.contains(Regex("^return&", RegexOption.IGNORE_CASE)) -> VM_Command_Type.C_RETURN
            currentCommand.contains(Regex("^call [a-z._]+ [1-9][0-9]*&", RegexOption.IGNORE_CASE)) ->
                VM_Command_Type
                    .C_CALL
            currentCommand.contains(Regex("^//")) || currentCommand.isBlank()
            -> VM_Command_Type.COMMENT
            else -> throw Exception("parsing error on '${currentCommand}'")
        }
    }

    // example: 'push argument 0' : arg1 = 'argument', arg2 = '0'

    // caller is responsible for not calling if command type is C_RETURN
    fun getArg1() = currentCommand.split(Regex(" "),3)[1]

    // caller is responsible for only calling if command type is C_PUSH, C_POP, C_FUNCTION, or C_CALL.
    fun getArg2AsInt() = currentCommand.split(Regex(" |\t"),4)[2].toInt()

    fun getArithType(): MATH_OP {
        for ( op in MATH_OP.values() ) {
            if ( currentCommand.contains(Regex("^${op}", RegexOption.IGNORE_CASE)) ) {
                return op
            }
        }
        throw Exception("no matching enum found in '${currentCommand}'")
    }

    fun getRegisterFromArg1(): REGISTER {
        val arg1 = getArg1()
        for ( reg in REGISTER.values() ) {
            if ( arg1.contains(Regex("${reg}", RegexOption.IGNORE_CASE)) ) {
                return reg
            }
        }
        throw Exception("no matching register found in arg1 of '${currentCommand}'")
    }
}