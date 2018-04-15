
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by (442377900) on 14-Mar-18.
 */

// module accepts a .vm file and translate to .asm (in HACK) by internally using VMParser and HACKCodeGen modules
class VMToHACKTranslator(protected val vmFiles: ArrayList<ReadFile>, protected val exportBaseFilename: String) {

    var generated_vm_code: StringBuilder = StringBuilder()

    fun translateFiles() {
        HACKCodeGen.generateBootstrapCode(generated_vm_code, true)
        for(vmFile in vmFiles) {
            val vm_parser = VMParser(vmFile.filename)
            val temp_vm_code = HACKCodeGen(getFilenameOnly(vmFile.filename), true)
            translate(vm_parser, temp_vm_code)
            generated_vm_code.append(temp_vm_code.code)
        }
    }

    protected fun translate(vm_parser: VMParser, vm_code_out: HACKCodeGen) {
        while(vm_parser.hasMoreCommands()) {
            when (vm_parser.getCurrentCommandType()) {

                VM_Command_Type.C_ARITHMETIC -> {
                    vm_code_out.mathOpHACK( vm_parser.getArithType(), vm_parser.currentCommand )
                }

                VM_Command_Type.C_PUSH -> {
                    vm_code_out.pushHACK( vm_parser.getRegisterFromArg1(), vm_parser.getArg2AsInt(), vm_parser.currentCommand )
                }

                VM_Command_Type.C_POP -> {
                    vm_code_out.popHACK( vm_parser.getRegisterFromArg1(), vm_parser.getArg2AsInt(), vm_parser.currentCommand )
                }

                VM_Command_Type.C_LABEL -> {
                    vm_code_out.labelHACK( vm_parser.getArg1(), vm_parser.currentCommand )
                }
                VM_Command_Type.C_GOTO -> {
                    vm_code_out.gotoHACK( vm_parser.getArg1(), vm_parser.currentCommand )
                }
                VM_Command_Type.C_IF_GOTO -> {
                    vm_code_out.ifGotoHACK( vm_parser.getArg1(), vm_parser.currentCommand )
                }
                VM_Command_Type.C_FUNCTION -> {
                    vm_code_out.functionHACK( vm_parser.getArg1(), vm_parser.getArg2AsInt(), vm_parser.currentCommand )
                }
                VM_Command_Type.C_RETURN -> {
                    vm_code_out.returnHACK(vm_parser.currentCommand)
                }
                VM_Command_Type.C_CALL -> {
                    vm_code_out.callHACK( vm_parser.getArg1(), vm_parser.getArg2AsInt(), vm_parser.currentCommand )
                }
                // comments are skipped
                VM_Command_Type.COMMENT -> {}
            }

            vm_parser.advanceToNextCommand()
        }
    }

    fun saveASM() {
        val outputFileName = "$exportBaseFilename.asm"
        val outputFile = WriteFile(outputFileName)
        outputFile.appendToFile(generated_vm_code.toString())
        println("writing to file: " + shortenPathName(outputFileName))
    }

    fun getHACKCode(): String = generated_vm_code.toString()

    fun translateAndSaveASM() {
        translateFiles()
        saveASM()
    }
}

fun printUsage() {
    println("usage: filename <VM file / directory> (make sure to quote path with spaces in path name")
    exitProcess(1)
}

fun translateVMFile(filename: String) {
    val vmFile = ReadFile(filename)
    val translator = VMToHACKTranslator(arrayListOf(vmFile), filename)
    translator.translateAndSaveASM()
}

// compiles vm code to HAcK, and places compiled code in a single asm file derived from directory name
fun translateVMFilesInDir(dirPath: String) {
    val myDir = MyDirectory(dirPath)
    val myVMFiles = ArrayList<ReadFile>()
    // add all *.vm files in dirPath to myVMFiles list
    for ( vmFile in myDir ) {
        when ( vmFile ) {
            is ReadFile -> {
                if (checkFilenameExtension(vmFile.filename, "vm")) {
                    myVMFiles.add(vmFile)
                    println("parsing " + shortenPathName(vmFile.name))
                }
                // file not .asm file
                else {
                    println("skipping over file " + shortenPathName(vmFile.name))
                }
            }
            is MyDirectory -> {
                println("skipping over directory " + shortenPathName(vmFile.name))
            }
        }
    }
    val splitName = dirPath.split("\\")
    // dir base name
    val newFileName = splitName[splitName.size - 1]
    VMToHACKTranslator(myVMFiles, dirPath + "\\" + newFileName).translateAndSaveASM()
}

// first cmd arg at args[0] (no program filename in argv[0] like in C)
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