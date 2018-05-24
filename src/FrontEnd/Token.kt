package FrontEnd

/**
 * Created by (442377900) on 24-May-18.
 */


enum class TOKEN_TYPE {
    KEYWORD             { override fun toString() = printHelper(name) },
    IDENTIFIER          { override fun toString() = printHelper(name) },
    SYMBOL              { override fun toString() = printHelper(name) },
    STRING_CONSTANT     { override fun toString() = printHelper(name) },
    INTEGER_CONSTANT    { override fun toString() = printHelper(name) },

    CLASS               { override fun toString() = printHelper(name) },
    CLASS_VAR_DEC       { override fun toString() = printHelper(name) },
    TYPE                { override fun toString() = printHelper(name) },
    SUBROUTINE_DECL     { override fun toString() = printHelper(name) },
    PARAMETER_LIST      { override fun toString() = printHelper(name) },
    SUBROUTINE_BODY     { override fun toString() = printHelper(name) },
    VAR_DECL            { override fun toString() = printHelper(name) },
    CLASS_NAME          { override fun toString() = printHelper(name) },
    SUBROUTINE_NAME     { override fun toString() = printHelper(name) },
    VAR_NAME            { override fun toString() = printHelper(name) },
    STATEMENTS          { override fun toString() = printHelper(name) },
    STATEMENT           { override fun toString() = printHelper(name) },
    LET_STATEMENT       { override fun toString() = printHelper(name) },
    IF_STATEMENT        { override fun toString() = printHelper(name) },
    WHILE_STATEMENT     { override fun toString() = printHelper(name) },
    DO_STATEMENT        { override fun toString() = printHelper(name) },
    RETURN_STATEMENT    { override fun toString() = printHelper(name) },

    EXPRESSION          { override fun toString() = printHelper(name) },
    EXPRESSION_LIST     { override fun toString() = printHelper(name) },
    TERM                { override fun toString() = printHelper(name) },
    SUBROUTINE_CALL     { override fun toString() = printHelper(name) },
    OP                  { override fun toString() = printHelper(name) },
    UNARY_OP            { override fun toString() = printHelper(name) },
    KEYWORD_CONSTANT    { override fun toString() = printHelper(name) };

    fun printHelper(str: String): String {
        val splitArr = str.split('_')
        var tmpStr = splitArr[0].toLowerCase()
        for (s in splitArr.slice(1 until splitArr.size)) {
            tmpStr += s.toLowerCase().capitalize()
        }
        return tmpStr
    }
}

// abstract syntax tree of tokens
interface TokenAST {
    val nodeType : TOKEN_TYPE
}

open class TokenBase(override val nodeType: TOKEN_TYPE) : TokenAST

class Token(val tokenType: TOKEN_TYPE, val body: String): TokenBase(tokenType) {
    override fun toString(): String {
        val xmlEscapedBody =
                when (body) {
                    "<"  -> "&lt;"
                    ">"  -> "&gt;"
                    "\"" -> "&quot;"
                    "&"  -> "&amp;"
                    else -> body
                }
        return "<$tokenType> $xmlEscapedBody </$tokenType>"
    }
}

class TokenWithChildren(val headNode: TokenBase, vararg childrenNodes: TokenAST?) : TokenAST {
    override val nodeType = headNode.nodeType

    val childNodes = ArrayList<TokenAST>()
    val childCount: Int
        get() = childNodes.size

    init {
        for (n in childrenNodes) {
            if (n != null) {
                childNodes.add(n)
            }
        }
    }
}

