import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by (442377900) on 28-Feb-18.
 */

// MyDirectory and MyFile implement this
interface MyDirFile {
    val name: String
}

class MyDirectory(val dirName: String): MyDirFile, Iterable<MyDirFile> {

    init {
        if(!Files.isDirectory(Paths.get(dirName)))
            throw Exception("directory: \"$dirName\" not found!")
    }

    override val name = dirName

    override fun iterator(): Iterator<MyDirFile> {
        return MyDirectoryIterator(dirName)
    }
}

// custom iterator for returning MyFile objects of files and sub-directories in parent directory
private class MyDirectoryIterator(val dirName: String): AbstractIterator<MyDirFile>() {

    override fun computeNext() {
        for (item in File(dirName).walk()) {
            if ( item.isFile() ) {
                super.setNext(ReadFile(item.absolutePath.toString()))
            }
            else if ( item.isDirectory() )
                super.setNext( MyDirectory(item.absolutePath.toString()) )
            else {
                throw Exception("encountered a type which is not a directory nor file in '$dirName'")
            }
        }
        // finish iterating
        super.done()
    }
}

abstract class MyFile(open val filename: String) : MyDirFile {
    var fileContent: String = ""
        protected set
    var fileContentLines: MutableList<String> = mutableListOf<String>()
        protected set
    var lineCount: Int = 0
        protected set
    override val name = filename
}

// read only file, filename must exist in filesystem already
class ReadFile(override var filename: String) : MyFile(filename) {
    init {
        filename = Paths.get(filename).toAbsolutePath().normalize().toString()
        if(!Files.exists(Paths.get(filename)))
            throw Exception("file: \"$filename\" does not exist in filesystem!")

        readInFile()
        super.lineCount = 1 + fileContentLines.count()
    }

    override val name = filename

    // http://kotlination.com/kotlin/read-file-kotlin (method 1.2)
    private fun readInFile() {
        File(filename).bufferedReader().useLines {
            it.forEach {
                fileContentLines.add(it)
                fileContent += it
            }
        }
    }

    // line counting starts at 1
    fun getLine(lineNumber: Int): String = fileContentLines[lineNumber-1]

}

// for writing to file, no reading
class WriteFile(override val filename: String) : MyFile(filename) {
    init{
        if(!File(filename).exists()) {
            val f: Boolean = File( filename).createNewFile()
            if (!f)
                throw error("can't create the file")
        }
    }

    override val name = filename

    fun appendToFile(data: String, addNewLine: Boolean = true) {
        fileContent += data
        fileContentLines.add(data)
        lineCount += 1
        flushToFile() // performs write
    }


    private fun flushToFile() = File(filename).bufferedWriter().use { it.write(fileContent)}
}

fun main(args: Array<String>) {
    val inF = ReadFile("input/test.in")
    println("lines in read file: " + inF.lineCount)

    val outF = WriteFile("output/test.out")
    outF.appendToFile("hello my name is Avior.")
    outF.appendToFile("and my name is Bob.")
}
