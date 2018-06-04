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

enum class ID_KIND {
    STATIC { override fun toString(): String = "static" },
    FIELD  { override fun toString(): String = "this" },
    ARG    { override fun toString(): String = "argument" },
    VAR    { override fun toString(): String = "local" }
}

enum class FUNCTION_SCOPE {
    CONSTRUCTOR,
    FUNCTION,
    METHOD
}

enum class UNARY_OP {
    MINUS,
    TILDA
}

enum class BINARY_OP {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    AND,
    OR,
    LESS_THAN,
    GREATER_THAN,
    EQUALS
}

enum class KEYWORD {
    TRUE,
    FALSE,
    NULL,
    THIS
}

// abstract syntax tree of tokens
// using composite pattern
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

/*
    // TokenWithChildren printFunction

    override fun toString(): String {
        val tmpStr: StringBuilder = StringBuilder()
        tmpStr.append( "<$headNode>\n" )
        for (child in childNodes) {
            tmpStr.append( "$child\n" )
        }
        tmpStr.append( "</$headNode>" )
        return tmpStr.toString()
    }
*/

class ClassToken(val className: String, val classVarDecList: ArrayList<ClassVarDecToken>,
                 val subroutineDecList: ArrayList<SubroutineDec>)

class ClassVarDecToken(val idKind: ID_KIND, val idType: String,
                       val varNameList: ArrayList<String>)

class SubroutineDec(val functionScope: FUNCTION_SCOPE, val returnType: String, val name: String,
                    val parameterList: ArrayList<Parameter>, val subroutineBody: SubroutineBody)

class Parameter(val type: String, val name: String)

class SubroutineBody(val varDecList: ArrayList<VarDec>, val statementList: ArrayList<Statement>)

class VarDec(val type: String, val nameList: ArrayList<String>)

interface Statement

class LetStatement(val varName: String, val arrayExpression: ExpressionTree?,
                   val rightSideExpression: ExpressionTree): Statement

class IfStatement(val conditionExpression: ExpressionTree, val bodyStatementsList: ArrayList<Statement>,
                  val elseStatementsList: ArrayList<Statement>?) : Statement

class WhileStatement(val conditionExpression: ExpressionTree, val bodyStatementsList: ArrayList<Statement>): Statement

class DoStatement(val subroutineCall: SubroutineCall): Statement

class ReturnStatement(val optionalExpression: ExpressionTree?): Statement

class SubroutineCall(val classNameVarName: String?, val subroutineName: String,
                     val expressionList: ArrayList<ExpressionTree>) : Term

// first element in expressionTreeChildList is always a Term
class ExpressionTree(val expressionTreeChildList: ArrayList<ExpressionTreeChild>): Term

interface ExpressionTreeChild
class Op(val operator: BINARY_OP) : ExpressionTreeChild
interface Term: ExpressionTreeChild

class IntegerConstant(val integerConstant: Int): Term
class StringConstant(val string: String): Term
class KeywordConstant(val keywordConstant: KEYWORD): Term
class VarNameWithArray(val varName: String, val optionalArrayExpression: ExpressionTree?): Term
class UnaryOpTerm(val unaryOp: UNARY_OP, val term: Term): Term
class VarName(val varName: String): Term

/*
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
*/
