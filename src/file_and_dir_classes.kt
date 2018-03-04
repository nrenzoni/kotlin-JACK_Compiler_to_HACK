import java.io.File
import java.io.InputStream
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
                super.setNext(MyFile(file.absolutePath))
            else {
                println("DEBUG: $file is a directory")
            }
        }
        // finish iterating
        super.done()
    }
}

class MyFile(var filename: String) {
    init {
        filename = Paths.get(filename).toAbsolutePath().normalize().toString()
        println("DEBUG: constructor for $filename called")
//        if(!Files.is .isFile(filename))
//            throw Exception("file: \"$filename\" not found!")
    }

    var fileContent: String = ""
        private set
        get() {
            if (field == "") {
                field = readInFile()
            }
            return field
        }

    var lineCount: Int = -1
        private set
        get() {
            if (field == -1) {
                var newlineCharsCounter = 1
                for(c in fileContent) {
                    if (c == '\n') {
                        newlineCharsCounter++
                    }
                }
                field = newlineCharsCounter
            }
            return field
        }


    // http://kotlination.com/kotlin/read-file-kotlin
    private fun readInFile(): String {
        val fileStream: InputStream = File(filename).inputStream()
        return fileStream.bufferedReader().use { it.readText() }
    }
}

fun main(args: Array<String>) {
    val f = MyFile("input/hello.in")
    println(f.lineCount)
}