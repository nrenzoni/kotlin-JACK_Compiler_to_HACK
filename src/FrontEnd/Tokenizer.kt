package FrontEnd
import FileAndDirectory.ReadFile
import FileAndDirectory.WriteFile

/**
 * Created by (442377900) on 14-May-18.
 */

val KEYWORDS: List<String> = listOf("class", "constructor", "function", "method", "field", "static", "var",
        "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if",
        "else", "while", "return")

val SYMBOLS: List<Char> = listOf('{', '}', '(', ')', '[', ']', '.', ',',
        ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~')

// _ or abc char
val IDCHARS: List<Char> = listOf('a'..'z','A'..'Z').flatten() + '_'

val SKIPCHARS: List<Char> = listOf(' ', '\n', '\t')

class Tokenizer(val file: ReadFile) {
    var tokenizedContent: String = ""
    var currCharIndex: Int = 0
    var currChar: Char = file.fileContent[0]
        get() {
            return file.fileContent[currCharIndex]
        }
    var nextChar: Char? = file.fileContent[1]
        get() {
            // bounds check ok for next char after current char
            if (currCharIndex+1 < file.fileContent.length)
                return file.fileContent[currCharIndex+1]
            else
                return null
        }

    private fun isFinished() = currCharIndex >= file.fileContent.length

    private fun advanceCharIndex(advanceCount: Int = 1) {
        currCharIndex += advanceCount
    }

    private fun appendTagWithBody(tagName: String, body: String) {
        appendToContent(generateXmlTag(tagName), false)
        appendToContent(body, false)
        appendToContent(generateXmlTag(tagName, true))
    }

    private fun appendToContent(inStr: String, addNewLine: Boolean = true) {
        if (addNewLine)
            tokenizedContent += inStr + "\n"
        else
            tokenizedContent += inStr
    }

    fun tokenize() {
        appendToContent(generateXmlTag("tokens"))

        while (!isFinished()) {

            when (currChar) {

                // is _ or abcABC char
                in IDCHARS -> keywordOrId()

                in '0'..'9' -> integerConstant()

                '/' -> {
                    if (nextChar == '/') {
                        advanceCharIndex(2)
                        while (!isFinished()) {
                            if (currChar == '\n') {
                                advanceCharIndex()
                                break
                            }
                            advanceCharIndex()
                        }
                    }
                    else if (nextChar == '*') {
                        advanceCharIndex(2)
                        while (!isFinished()) {
                            if (currChar == '*' && nextChar == '/') {
                                advanceCharIndex(2)
                                break
                            }
                            advanceCharIndex()
                        }
                    }
                    else
                        symbol()
                }

                in SYMBOLS -> symbol()

                '"' -> quotes()

                // whitespace or newline char
                in SKIPCHARS -> advanceCharIndex()

                else -> { throw Exception("syntax in ${file.name} does not match lexicographic rules." +
                        " error starting in string: ${file.fileContent.substring(currCharIndex, currCharIndex+5)}")}
            }

        }

        appendToContent(generateXmlTag("tokens", true), false)
    }

    // advances current parse char index in the process
    // as long as next char is in charsInList, char is added to generated token
    private fun getTokInListHelper(charsInList: List<Char>): String {
        var temp = ""
        while (!isFinished() && currChar in charsInList) {
            temp += currChar
            advanceCharIndex()
        }
        return temp
    }

    private fun keywordOrId() {
        val tempParsedTok = getTokInListHelper(IDCHARS)
        if (tempParsedTok.toLowerCase() in KEYWORDS) {
            appendTagWithBody("keyword", tempParsedTok)
        }
        else
            appendTagWithBody("identifier", tempParsedTok)
    }

    private fun integerConstant() {
        val tempParsedTok = getTokInListHelper(('0'..'9').toList())
        appendTagWithBody("integerConstant", tempParsedTok)
    }

    private fun symbol() {

        val tempParsedTok =
                when (currChar) {
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '&' -> "&amp;"
                    else -> currChar.toString()
                }

        appendTagWithBody("symbol", tempParsedTok)
        advanceCharIndex()
    }

    private fun quotes() {
        var tempParsedTok = ""

        // don't want to include quotes in token
        advanceCharIndex()

        while (!isFinished() && currChar != '"' ) {
            tempParsedTok += currChar
            advanceCharIndex()
        }

        // currChar == '"' or index is after last char
        advanceCharIndex()

        appendTagWithBody("StringConstant", tempParsedTok)
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw Exception("usage: programName filename.jack")
    val jackFile = ReadFile(args[0])
    val xmlOutFile = WriteFile(jackFile.name + "T.xml")

    val tokenizer = Tokenizer(jackFile)
    tokenizer.tokenize()

    xmlOutFile.appendToFile(tokenizer.tokenizedContent)
}