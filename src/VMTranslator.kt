
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by (442377900) on 14-Mar-18.
 */

fun usage(filename: String) {
    println("usage: ${filename} <VM file / directory>")
    exitProcess(1)
}

fun main(args: Array<String>) {
    // parse cmd line args, first arg should be .VM filename or directory containing .VM files
    if(args.size < 2) {
       usage(args[0])
    }

    val vm_filename: String = args[1]

    if (File(args[1]).isDirectory() ) {
        val myDir = MyDirectory(args[1])

        // process all *.vm files in directory and generate corresponding .asm files accordingly
        for ( vmFile: MyFile in myDir ) {
            val vm_translater = HACKCodeGen(vmFile.filename)
            val vm_parser = VMParser(vmFile.filename)

            while(vm_parser.hasMoreCommands()) {
                val cur_cmd = vm_parser.currentCommand
                when (vm_parser.getCurrentCommandType()) {

                    VM_Command_Type.C_ARITHMETIC -> {
                        vm_translater.mathOpHACK( extractARITHType(vm_parser.getArg1()) )
                    }

                    VM_Command_Type.C_PUSH -> {
                        vm_translater.pushHACK( extractREGISTER(vm_parser.getArg1()), vm_parser.getArg2().toInt() )
                    }

                    VM_Command_Type.C_POP -> {
                        vm_translater.popHACK( extractREGISTER(vm_parser.getArg1()), vm_parser.getArg2().toInt() )
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

            // implement: complete logic for writing asm file
            val code_writer = CodeWriter(vm_filename)
            code_writer.close()
        }
    }

    else if ( File(args[1]).isFile() ) {

    }

    else {
        usage(args[0])
    }
}