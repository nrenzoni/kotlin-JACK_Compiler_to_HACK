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
    SUBROUTINE_DEC      { override fun toString() = printHelper(name) },
    PARAMETER_LIST      { override fun toString() = printHelper(name) },
    SUBROUTINE_BODY     { override fun toString() = printHelper(name) },
    VAR_DEC             { override fun toString() = printHelper(name) },
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
// composite pattern
interface TokenAST {
    val nodeType : TOKEN_TYPE
}

open class TokenBase(override val nodeType: TOKEN_TYPE) : TokenAST {
    override fun toString(): String = "$nodeType";
}

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

    // no trailing new line char
    override fun toString(): String {
        val tmpStr: StringBuilder = StringBuilder()
        tmpStr.append( "<$headNode>\n" )
        for (child in childNodes) {
            tmpStr.append( "$child\n" )
        }
        tmpStr.append( "</$headNode>" )
        return tmpStr.toString()
    }
}

// recursive print function on Token AST
fun tokenASTPrinter(ast: TokenAST, indentCount: Int = 0): String {

    val tmpStr = StringBuilder()
    val indentStr = " ".repeat(indentCount * 2)


    // skip printing parent node if nodeType is one of the following TOKEN_TYPE
    val skipParentPrint =
            ast.nodeType in arrayOf(TOKEN_TYPE.STATEMENT, TOKEN_TYPE.CLASS_NAME, TOKEN_TYPE.SUBROUTINE_NAME,
                            TOKEN_TYPE.TYPE, TOKEN_TYPE.VAR_NAME, TOKEN_TYPE.SUBROUTINE_CALL)

    // if not printing parent, then don't increment sub-indent by 1
    val subIndent =
        if (skipParentPrint)
            indentCount
        else
            indentCount + 1

    when (ast) {
        is TokenWithChildren -> {
            if (!skipParentPrint)
                tmpStr.append(indentStr + "<${ast.nodeType}>\n")
            for (child in ast.childNodes) {
                val subVal = tokenASTPrinter(child, subIndent)
                tmpStr.append(subVal)
            }
            if (!skipParentPrint)
                tmpStr.append( indentStr + "</${ast.nodeType}>\n" )
        }
        is Token -> {
            tmpStr.append( indentStr + ast + "\n" )
        }
        is TokenBase -> {
            tmpStr.append( indentStr + "<$ast>\n" )
            tmpStr.append( indentStr + "</$ast>\n" )
        }
    }

    return tmpStr.toString()
}