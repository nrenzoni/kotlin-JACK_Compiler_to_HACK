package Misc

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by (442377900) on 28-Feb-18.
 */

// Misc.MyDirectory and Misc.MyFile implement this
interface MyDirFile {
    val name: String
}

class MyDirectory(val dirName: String): MyDirFile, Iterable<MyDirFile> {

    init {
        if(!Files.isDirectory(Paths.get(dirName)))
            throw Exception("not a directory: \"$dirName\"")
    }

    override val name = dirName

    override fun iterator(): Iterator<MyDirFile> {
        return MyDirectoryIterator(dirName)
    }
}

// custom iterator for returning Misc.MyFile objects of files and sub-directories in parent directory
private class MyDirectoryIterator(val dirName: String): Iterator<MyDirFile> {
    val dirContent = File(dirName).walk()
    // start curIndex at 1 since index 0 is directory itself
    var curIndex = 1;
    val maxIndex = dirContent.count() - 1

    override fun next(): MyDirFile {
        val item = dirContent.elementAt(curIndex++)
        return when {
            item.isFile() -> ReadFile(item.absolutePath)
            item.isDirectory() -> MyDirectory(item.absolutePath)
            else -> throw Exception("encountered a type which is not a directory nor file in '$dirName'")
        }
    }

    override fun hasNext(): Boolean =  curIndex <= maxIndex
}

abstract class MyFile(override val name: String) : MyDirFile {
    var fileContent: String = ""
        protected set
    // array of strings for each line in file
    var fileContentLines: MutableList<String> = mutableListOf()
        protected set
    var lineCount: Int = 0
        protected set
}

// read-only file, name must exist in filesystem already
class ReadFile(override var name: String) : MyFile(name) {
    init {
        name = Paths.get(name).toAbsolutePath().normalize().toString()
        if(!Files.exists(Paths.get(name)))
            throw Exception("file: \"$name\" does not exist in filesystem!")

        readInFile()
        super.lineCount = 1 + fileContentLines.count()
    }

    // http://kotlination.com/kotlin/read-file-kotlin (method 1.2)
    private fun readInFile() {
        File(name).bufferedReader().useLines {
            it.forEach {
                fileContentLines.add(it)
                fileContent += it + "\n"
            }
        }
    }

    // line counting starts at 1
    fun getLine(lineNumber: Int): String = fileContentLines[lineNumber-1]

}

// for writing to file, no reading
class WriteFile(override val name: String) : MyFile(name) {
    init{
        if(!File(name).exists()) {
            val f: Boolean = File( name).createNewFile()
            if (!f)
                throw error("error creating: $name")
        }
    }

    // fix: data appended to fileContentLines should be split on newline char. lineCount should increase according to
    // how many lines of data input
    fun appendToFile(data: String, addNewLine: Boolean = true) {
        fileContent += data
        fileContentLines.add(data)
        lineCount += 1
        flushToFile() // performs write
    }

    private fun flushToFile() = File(name).bufferedWriter().use { it.write(fileContent)}
}

fun shortenPathName(inPath: String, maxPrintDepth: Int = 3): String {
    val splitName = inPath.split("\\")
    return if (splitName.size > maxPrintDepth) {
        var printName = "...\\"
        for (i in splitName.size - maxPrintDepth until splitName.size - 1) {
            printName += splitName[i] + "\\"
        }
        printName += splitName[splitName.size-1]
        printName
    } else
        inPath
}

fun checkFilenameExtension(filename: String, extension: String): Boolean {
    val filename_split = filename.split("")
    return filename_split[filename_split.size-1].contains(Regex(extension, RegexOption.IGNORE_CASE))
}

// returns name without base directory nor extension
fun getFilenameOnly(filename: String): String {
    val filename_no_dir = filename.split("\\").last()
    return filename_no_dir.split("", limit = 2).first()
}

fun main(args: Array<String>) {
    val inF = ReadFile("input/test.in")
    println("lines in read file: " + inF.lineCount)

    val outF = WriteFile("output/test.out")
    outF.appendToFile("hello my name is Avior.")
    outF.appendToFile("and my name is Bob.")
}
