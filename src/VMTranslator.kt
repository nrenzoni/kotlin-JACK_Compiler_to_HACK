import java.io.File
import kotlin.system.exitProcess

/**
 * Created by (442377900) on 14-Mar-18.
 */

// module accepts a .vm file and translate to .asm (in HACK) by internally using VMParser and HACKCodeGen modules
class VMToHACKTranslator(vmFile: ReadFile) {
    protected val vm_parser = VMParser(vmFile.filename)

    // file name with no extension nor directory is used in hack code generator for compiling labels
    protected val filename_no_ext: String
    protected val filename_no_ext_no_dir: String
    protected val vm_code_gen: HACKCodeGen

    init {
        val filename_split = vmFile.filename.split(".", limit = 2)
        if (! filename_split[1].contains(Regex("vm", RegexOption.IGNORE_CASE))) {
            throw Exception("cannot parse a non vm file")
        }
        filename_no_ext = filename_split[0]

        val tmp = filename_no_ext.split("\\")
        filename_no_ext_no_dir = tmp[tmp.size-1]

        vm_code_gen = HACKCodeGen(filename_no_ext_no_dir)

        translate()
    }

    protected fun translate() {

        while(vm_parser.hasMoreCommands()) {
            when (vm_parser.getCurrentCommandType()) {

                VM_Command_Type.C_ARITHMETIC -> {
                    vm_code_gen.mathOpHACK( vm_parser.getArithType() )
                }

                VM_Command_Type.C_PUSH -> {
                    vm_code_gen.pushHACK( vm_parser.getRegisterFromArg1(), vm_parser.getArg2AsInt() )
                }

                VM_Command_Type.C_POP -> {
                    vm_code_gen.popHACK( vm_parser.getRegisterFromArg1(), vm_parser.getArg2AsInt() )
                }

                VM_Command_Type.C_LABEL -> {
                    vm_code_gen.labelHACK( vm_parser.getArg1() )
                }
                VM_Command_Type.C_GOTO -> {
                    vm_code_gen.gotoHACK( vm_parser.getArg1() )
                }
                VM_Command_Type.C_IF_GOTO -> {
                    vm_code_gen.ifGotoHACK( vm_parser.getArg1() )
                }
                VM_Command_Type.C_FUNCTION -> {
                    vm_code_gen.functionHACK( vm_parser.getArg1(), vm_parser.getArg2AsInt() )
                }
                VM_Command_Type.C_RETURN -> {
                    vm_code_gen.returnHACK()
                }
                VM_Command_Type.C_CALL -> {
                    vm_code_gen.callHACK( vm_parser.getArg1(), vm_parser.getArg2AsInt() )
                }
                // comments are skipped
                VM_Command_Type.COMMENT -> {}
            }

            vm_parser.advanceToNextCommand()
        }
    }

    // saveASM() static function
    companion object {
        fun saveASM(baseFilename: String, asmCode: String) {
            val outputFile = WriteFile("${baseFilename}.asm")
            outputFile.appendToFile(asmCode)
        }
    }

    fun saveASM() {
        VMToHACKTranslator.saveASM("${filename_no_ext}", vm_code_gen.code)
    }

    fun getHACKCode(): String = vm_code_gen.code
}

fun printUsage() {
    println("usage: filename <VM file / directory>")
    exitProcess(1)
}

fun translateVMFile(filename: String) {
    val vmFile = ReadFile(filename)
    val translator = VMToHACKTranslator(vmFile)
    translator.saveASM()
}

// compiles vm code to HAcK, and places compiled code in a single asm file derived from directory name
fun translateVMFilesInDir(dirName: String) {
    val myDir = MyDirectory(dirName)
    var compiledHACKCode: String = ""

    // process all *.vm files in directory and generate corresponding .asm files accordingly
    for ( vmFile in myDir ) {
        when ( vmFile ) {
            is ReadFile -> {
                if (vmFile.filename.contains(Regex(".asm&", RegexOption.IGNORE_CASE))) {
                    compiledHACKCode += VMToHACKTranslator(vmFile).getHACKCode()
                }
                else
                    println("skipping over ${vmFile.name} in ${myDir.dirName}")
            }
            /*is MyDirectory -> {
                // recursion to process sub-directories
                translateVMFilesInDir(vmFile.dirName)
            }*/
            else -> println("skipping over ${vmFile.name} in ${myDir.dirName}")
        }
    }

    VMToHACKTranslator.saveASM("${dirName}.asm", compiledHACKCode)
}

// args starts at 0 (no program filename in arg[0] like in C)
fun main(args: Array<String>) {
    // parse cmd line args, first arg should be either .VM file name or directory name containing .VM files

    if(args.isEmpty()) {
       printUsage()
    }

    when {
        File(args[0]).isDirectory() -> translateVMFilesInDir(args[0])
        File(args[0]).isFile() -> translateVMFile(args[0])
        // arg1 isn't file  or directory
        else -> printUsage()
    }
}