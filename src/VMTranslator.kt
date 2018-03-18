import java.io.File
import kotlin.system.exitProcess

/**
 * Created by (442377900) on 14-Mar-18.
 */

class VMToHACKTranslator(vmFile: ReadFile) {
    protected val vm_code_gen = HACKCodeGen(vmFile.filename)
    protected val vm_parser = VMParser(vmFile.filename)

    // file name without extension
    protected val filename_base: String

    init {
        val filename_split = vmFile.filename.split(".", limit = 2)
        if (! filename_split[1].contains(Regex("vm", RegexOption.IGNORE_CASE))) {
            throw Exception("cannot parse a non vm file")
        }
        filename_base = filename_split[0]
    }

    fun translate() {

        while(vm_parser.hasMoreCommands()) {
            val cur_cmd = vm_parser.currentCommand
            when (vm_parser.getCurrentCommandType()) {

                VM_Command_Type.C_ARITHMETIC -> {
                    vm_code_gen.mathOpHACK( extractARITHType( cur_cmd ) )
                }

                VM_Command_Type.C_PUSH -> {
                    vm_code_gen.pushHACK( extractREGISTER( cur_cmd ), vm_parser.getArg2AsInt() )
                }

                VM_Command_Type.C_POP -> {
                    vm_code_gen.popHACK( extractREGISTER( cur_cmd ), vm_parser.getArg2AsInt() )
                }

                VM_Command_Type.C_LABEL -> TODO()
                VM_Command_Type.C_GOTO -> TODO()
                VM_Command_Type.C_IF -> TODO()
                VM_Command_Type.C_FUNCTION -> TODO()
                VM_Command_Type.C_RETURN -> TODO()
                VM_Command_Type.C_CALL -> TODO()
            }

            vm_parser.advanceToNextCommand()
        }
    }

    fun saveASM() {
        val outputFile = WriteFile("${filename_base}.asm")
        outputFile.appendToFile(vm_code_gen.code)
    }
}

fun usage(filename: String) {
    println("usage: ${filename} <VM file / directory>")
    exitProcess(1)
}

fun main(args: Array<String>) {
    // parse cmd line args, first arg should be either .VM file name or directory name containing .VM files

    if(args.size < 2) {
       usage(args[0])
    }

    when {
        File(args[1]).isDirectory() -> {
            val myDir = MyDirectory(args[1])

            // process all *.vm files in directory and generate corresponding .asm files accordingly
            for ( vmFile: MyFile in myDir ) {
                if (vmFile is ReadFile) {
                    val translator = VMToHACKTranslator(vmFile)
                    translator.translate()
                    translator.saveASM()
                }
            }
        }

        // arg1 is file
        File(args[1]).isFile() -> {

        }

        // arg1 isn't file  or directory
        else -> usage(args[0])
    }
}