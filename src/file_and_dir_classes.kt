
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by (442377900) on 28-Feb-18.
 */

class MyDirectory(val dirName: String): AbstractIterator<MyFile>() {

    init {
        if(!Files.isDirectory(Paths.get(dirName)))
            throw Exception("directory: \"$dirName\" not found!")
    }

    override fun computeNext() {
        for (file in File(dirName).walk()) {
            if(file.isFile)
                super.setNext(ReadFile(file.absolutePath))
            else {
                println("DEBUG: $file is a directory")
            }
        }
        // finish iterating
        super.done()
    }
}

abstract class MyFile(open val filename: String) {
    var fileContent: String = ""
        protected set
    var fileContentLines: MutableList<String> = mutableListOf<String>()
        protected set
    var lineCount: Int = 0
        protected set
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

    // http://kotlination.com/kotlin/read-file-kotlin (method 1.2)
    private fun readInFile() {
        File(filename).bufferedReader().useLines {
            it.forEach {
                fileContentLines.add(it)
                fileContent += it
            }
        }
    }

    fun getLine(lineNumber: Int): String = fileContentLines[lineNumber-1]

}

// write file object for only writing to file, no reading
class WriteFile(override val filename: String) : MyFile(filename) {
    fun AppendToFile(data: String, addNewLine: Boolean = true) {
        fileContent += data + '\n'
        fileContentLines.add(data)
        lineCount += 1
        flushToFile()
    }

    private fun flushToFile() = File(filename).bufferedWriter().use { it.write(fileContent)}
}

fun main(args: Array<String>) {
    val f = ReadFile("input/hello.in")
    println(f.lineCount)
}