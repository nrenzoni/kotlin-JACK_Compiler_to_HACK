package FrontEnd

import Misc.ReadFile
import Misc.WriteFile

/**
 * Created by (442377900) on 24-May-18.
 */

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw Exception("usage: programName filename.jack")
    val jackFile = ReadFile(args[0])
    val xmlOutFile = WriteFile(jackFile.nameWithoutExtension + "T.xml")

    val tokenizer = Tokenizer(jackFile)
    val tp = TokenParser(tokenizer.iterator())

//    xmlOutFile.appendToFile(tokenizer.tokenizedContentForFileOut)
    xmlOutFile.appendToFile( tp.toString() )
}