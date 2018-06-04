package FrontEnd
import Misc.ReadFile
import Misc.generateXmlTag

/**
 * Created by (442377900) on 14-May-18.
 */

val KEYWORDS: List<String> = listOf("class", "constructor", "function", "method", "field", "static", "var",
        "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if",
        "else", "while", "return")

val SYMBOLS: List<Char> = listOf('{', '}', '(', ')', '[', ']', '.', ',',
        ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~')

// _ or abc char
val IDCHARS: List<Char> = listOf('a'..'z','A'..'Z', '0'..'9').flatten() + '_'

val SKIPCHARS: List<Char> = listOf(' ', '\n', '\t')


class Tokenizer(private val file: ReadFile) : Iterable<Token> {
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

    private val _tokens: ArrayList<Token> = arrayListOf()
    val tokens: ArrayList<Token>
        get() {
            if (_tokens.isEmpty())
                parseTokens()
            return _tokens
        }

    var tokenizedContentForFileOut: String = ""
        private set
        get() {
            if (field.isEmpty()) {
                field = generateXmlTag("tokens") + "\n"
                for (tok in this) {
                    field += "$tok\n"
                }
                field += generateXmlTag("tokens", true) + "\n"
            }
            return field
        }

    private fun advanceCharIndex(advanceCount: Int = 1) {
        currCharIndex += advanceCount
    }

    private fun appendToken(tokType: TOKEN_TYPE, body: String) {
        _tokens.add( Token(tokType, body) )
    }

    private fun parseTokens() {

        while (!isFinished()) {

            when (currChar) {

                in '0'..'9' -> integerConstant()

                // is _, abcABC char, or 0..9
                in IDCHARS -> keywordOrId()

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
            appendToken(TOKEN_TYPE.KEYWORD, tempParsedTok)
        }
        else
            appendToken(TOKEN_TYPE.IDENTIFIER, tempParsedTok)
    }

    private fun integerConstant() {
        val tempParsedTok = getTokInListHelper(('0'..'9').toList())
        appendToken(TOKEN_TYPE.INTEGER_CONSTANT, tempParsedTok)
    }

    private fun symbol() {
        appendToken(TOKEN_TYPE.SYMBOL, currChar.toString())
        advanceCharIndex()
    }

    private fun quotes() {
        var tempParsedTok = ""

        while (true) {
            advanceCharIndex()

            if (isFinished())
                break
            if (currChar == '"') {
                advanceCharIndex()
                break
            }

            tempParsedTok += currChar
        }

        appendToken(TOKEN_TYPE.STRING_CONSTANT, tempParsedTok)
    }

    override fun iterator(): TokenIterator {
        return TokenIterator(tokens)
    }
}

class TokenIterator(private val tokens: ArrayList<Token>) : Iterator<Token> {
    private var curTokI = 0;
    private val tokLen = tokens.size

    override fun hasNext(): Boolean = curTokI < tokLen

    override fun next(): Token = tokens.elementAt(curTokI++)

    fun getCurrent(): Token = tokens.elementAt(curTokI)

    fun getCurState(): Int {
        return curTokI
    }

    fun restoreState(savedState: Int) {
        curTokI = savedState
    }

    fun getNextTokOrThrowExcp(): Token {
        if (hasNext())
            return next()
        throw Exception("no more tokens to return")
    }

}