/**
 * Created by (442377900) on 14-Mar-18.
 */

fun main(args: Array<String>) {
    // parse cmd line args, first arg should be .VM filename
    if(args.size < 2) {
        println("usage: ${args[0]} <VM file>")
        return
    }

    // implement:
    // check if arg[1] is directory, and if so, process all *.vm files in directory and generate corresponding .asm
    // files accordingly

    val vm_filename: String = args[1]

    val vm_parser = VMParser(vm_filename)
    val code_writer = CodeWriter(vm_filename)

    while (vm_parser.hasMoreCommands()) {
        var cmd_type = vm_parser.getCurrentCommandType()

        // complete logic for writing HACK command to/in code_writer

        if(cmd_type != VM_Command_Type.C_RETURN) {

        }

        vm_parser.advanceToNextCommand()
    }

    code_writer.close()
}