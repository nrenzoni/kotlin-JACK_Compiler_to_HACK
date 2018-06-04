package FrontEnd

import Misc.*
import kotlin.system.exitProcess

/**
 * Created by (442377900) on 24-May-18.
 */

fun jackToVm(dirName: String) {
    val dir = MyDirectory(dirName)
    for (file in dir) {
        if (file is ReadFile && file.checkFilenameExtension("jack")) {
            try {
                val outputFilename = file.nameWithoutExtension + "_KotlinVersion.vm"
                val tokenizer = Tokenizer(file)
                val tokenParser = TokenParser(tokenizer)
                val currCodeGenObj = CodeGeneration(tokenParser)
                WriteFile(outputFilename).appendToFile(currCodeGenObj.toString())
                println("successfully output \"" + shortenPathName(file.name) + "\" to \""
                        + shortenPathName(outputFilename) + "\"")
            }
            catch (e: Exception) {
                println("exception while parsing " + shortenPathName(file.name))
                println(e.message)
                for (i in e.stackTrace) {
                    println(i)
                }
                exitProcess(-1)
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw Exception("usage: programName <directory containing .jack files>")
    jackToVm(args[0])
}