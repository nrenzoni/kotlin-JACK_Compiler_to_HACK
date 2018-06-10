package FrontEnd

/**
 * Created by (442377900) on 18-May-18.
 */

val typeTable = arrayListOf("int", "char", "boolean")

class TokenParser(tokenParser: Tokenizer) {

    private val tokenIter = tokenParser.iterator()

    val parsedAST: ClassToken

    init {
        parsedAST = parse()
    }

    /*override fun toString(): String {
        return tokenASTPrinter(parsedAST)
    }*/

    private fun parse(): ClassToken {
        // all files must start with class declaration
        val ast = classRule()
        if (tokenIter.hasNext())
            throw Exception("code does not conform to grammar: finished parsing with remaining unparsed tokens")
        return ast
    }

    // 'class' className '{' classVarDecList* subroutineDecList* '}'
    private fun classRule(): ClassToken {
        val classTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(classTok, "class")

        val className = classNameRule()

        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, Regex.escape("{"))

        val classVarDecList: ArrayList<ClassVarDecToken> = arrayListOf()
        // keep parsing classVarDecList grammar elements until no more matches
        var savedState = tokenIter.getCurState()
        // classVarDecList*
        try {
            while (true) {
                val curClassVarDec = classVarDecRule()
                classVarDecList.add(curClassVarDec)
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        val subroutineDecList: ArrayList<SubroutineDec> = arrayListOf()
        savedState = tokenIter.getCurState()
        // subroutineDecList*
        try {
            while (true) {
                subroutineDecList.add( subroutineDecRule() )
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, Regex.escape("}"))

        return ClassToken(className, classVarDecList, subroutineDecList)

//        return TokenWithChildren(TokenBase(TOKEN_TYPE.CLASS), classTok, className, leftBracketTok,
//                *classVarDecList.toArr(), *classVarDecList.toArr(), rightBracketTok)
    }

    // ('static'|'field') type varName (',' varName)* ';'
    private fun classVarDecRule(): ClassVarDecToken {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, "(static|field)")
        val idKind = ID_KIND.valueOf(t1.body.toUpperCase())
        val idType = typeRule()

        val varNameList = arrayListOf<String>()

        varNameList.add( varNameRule() )

        // use list to place elements matching (',' varName)*
        var nextTok = tokenIter.getNextTokOrThrowExcp()
        while (grammarMatch(nextTok,",", false)) {
            val varNameTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(varNameTok, r_varName)
            varNameList.add( varNameTok.body )
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }
        grammarMatch(nextTok, ";")

        return ClassVarDecToken(idKind, idType, varNameList)
    }

    // 'int'|'char'|'boolean'|className
    private fun typeRule(): String {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, r_type)
        if (t1.body !in typeTable)
            typeTable.add(t1.body)
        return t1.body
    }

    // ('constructor'|'function'|'method') ('void'|type) subroutineName '(' parameterList ')' subroutineBody
    private fun subroutineDecRule(): SubroutineDec {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, "constructor|function|method")
        val scope = FUNCTION_SCOPE.valueOf(t1.body.toUpperCase())

        val t2 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t2, "void|$r_type")
        val returnType = t2.body

        val subroutineName = subroutineNameRule()

        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")

        val parameterList = parameterListRule()

        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")

        val subroutineBody = subroutineBodyRule()

        return SubroutineDec(scope, returnType, subroutineName, parameterList, subroutineBody)
    }

    // ((type varName) (',' type varName)*)?
    private fun parameterListRule(): ArrayList<Parameter> {

        val parameterList = arrayListOf<Parameter>()
        var savedState = tokenIter.getCurState()
        try {
            val type = typeRule()
            val varName = varNameRule()
            parameterList.add( Parameter(type, varName) )
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
            // empty child list for token AST
            return parameterList // empty
        }

        savedState = tokenIter.getCurState()
        // (',' type varName)*
        try {
            while (true) {
                val commaTok = tokenIter.getNextTokOrThrowExcp()
                grammarMatch(commaTok, ",")
                val curType = typeRule()
                val curVarName = varNameRule()
                parameterList.add(Parameter(curType, curVarName))
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        return parameterList
    }

    // '{' varDec* statements '}'
    private fun subroutineBodyRule(): SubroutineBody {

        val varDecList = arrayListOf<VarDec>()

        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")

        // save which token we're at, since if varDec doesn't match, we'll rollback
        var savedState: Int = tokenIter.getCurState()
        try {
            while (true) {
                varDecList.add( varDecRule() )
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            // roll back token iterator to before varDec
            tokenIter.restoreState(savedState)
        }

        val statementsList = statementsRule()

        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        return SubroutineBody(varDecList, statementsList)
    }

    // 'var' type varName (',' varName)* ';'
    private fun varDecRule(): VarDec {
        val varNameList = arrayListOf<String>()

        val varTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(varTok, "var")

        val idType = typeRule()

        // add first varName
        varNameList.add( varNameRule() )

        // (',' varName)*
        var nextTok = tokenIter.getNextTokOrThrowExcp()

        while (grammarMatch(nextTok,",", false)) {
            varNameList.add( varNameRule() )
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }

        grammarMatch(nextTok, ";")

        return VarDec(idType, varNameList)
    }

    private fun classNameRule(): String      = getIdStringHelper()
    private fun subroutineNameRule(): String = getIdStringHelper()
    private fun varNameRule(): String        = getIdStringHelper()

    // returns string of token if it is an identifier, otherwise throws exception
    private fun getIdStringHelper(): String {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, l_identifier)
        return t1.body
    }

    // statement*
    private fun statementsRule(): ArrayList<Statement> {

        val statementList = arrayListOf<Statement>()

        var savedState = tokenIter.getCurState()
        try {
            // interpret as many statement(s) as possible until exception thrown
            while (true) {
                statementList.add( statementRule() )
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }
        return statementList
    }

    // letStatement | ifStatement | whileStatement | doStatement | returnStatement
    private fun statementRule() : Statement {
        var t1: Statement?

        // if ruleFunc on next token throws exception, roll back iterator, return null; else return tokenAST with
        // returned AST from ruleFunc
        fun <E> tryCatchRollback(ruleFunc: () -> E): E? {
            val savedState = tokenIter.getCurState()
            return try {
                ruleFunc()
            }
            catch (e: Exception) {
                tokenIter.restoreState(savedState)
                null
            }
        }

        t1 = tryCatchRollback(this::letStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(this::ifStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(this::whileStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(this::doStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(this::returnStatementRule)
        if (t1 != null) return t1

        // if no matches, raise exception
        throw Exception("no match found for ${tokenIter.getCurrent()}")
    }

    // 'let' varName  ('[' expression ']')? '=' expression ';'
    private fun letStatementRule(): LetStatement {
        val letTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(letTok, "let")
        val varName = varNameRule()

        var nextTok = tokenIter.getNextTokOrThrowExcp()

        var optionalArrayExpression: ExpressionTree? = null
        if (grammarMatch(nextTok, "\\[", false)) {
            val expression = expressionRule()
            val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(rightBracketTok, "\\]")
            optionalArrayExpression = expression
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }
        grammarMatch(nextTok, "=")
        val rightHandExpression = expressionRule()
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return LetStatement(varName, optionalArrayExpression, rightHandExpression)
    }

    // 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
    private fun ifStatementRule() : IfStatement {
        val ifTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(ifTok, "if")
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val conditionExpression = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")
        val bodyStatementsList = statementsRule()
        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        var optionalElseStatementsList: ArrayList<Statement>? = null
        val savedState = tokenIter.getCurState()
        val nextTok = tokenIter.getNextTokOrThrowExcp()
        if(grammarMatch(nextTok, "else",false)) {
            val t1 = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(t1, "\\{")
            optionalElseStatementsList = statementsRule()
            val t2 = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(t2, "\\}")
        }
        else
            tokenIter.restoreState(savedState)

        return IfStatement(conditionExpression, bodyStatementsList, optionalElseStatementsList)
    }

    // 'while' '(' expression ')' '{' statements '}'
    private fun whileStatementRule() : WhileStatement {
        val whileTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(whileTok, "while")
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val conditionExpression = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")
        val bodyStatementsList = statementsRule()
        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        return WhileStatement(conditionExpression, bodyStatementsList)
    }

    // 'do' subroutineCall ';'
    private fun doStatementRule(): DoStatement {
        val doTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(doTok, "do")
        val subroutineCall = subroutineCallRule()
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return DoStatement(subroutineCall)
    }

    // 'return' expression? ';'
    private fun returnStatementRule() : ReturnStatement {
        val returnTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(returnTok, "return")

        val savedState = tokenIter.getCurState()
        var optionalExpression: ExpressionTree? = null
        try {
            optionalExpression = expressionRule()
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return ReturnStatement(optionalExpression)
    }

    // term (op term)*
    private fun expressionRule(): ExpressionTree {
        val expressionChildList: ArrayList<ExpressionTreeChild> = arrayListOf()

        expressionChildList.add( termRule() )

        // (op term)*
        var savedState = tokenIter.getCurState()
        try {
            while (true) {
                val curOpAST = opRule()
                val curTerm = termRule()
                expressionChildList.add( curOpAST )
                expressionChildList.add(curTerm)
                savedState = tokenIter.getCurState()
            }
        }
        // fix: catch only opRule exception (termRule exception should not cause restore)
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        return ExpressionTree(expressionChildList)
    }

    // integerConstant | string | keywordConstant | varName ('[' expression ']')? | subroutineCall
    // | '(' expression ')' | unaryOp term
    private fun termRule(): Term {
        var savedState = tokenIter.getCurState()
        try { return integerConstantRule() }
        catch (e: Exception) { tokenIter.restoreState(savedState) }

        savedState = tokenIter.getCurState()
        try { return subroutineCallRule() }
        catch (e: Exception) { tokenIter.restoreState(savedState) }

        savedState = tokenIter.getCurState()
        try { return stringConstantRule() }
        catch (e: Exception) { tokenIter.restoreState(savedState) }

        savedState = tokenIter.getCurState()
        try { return keywordConstantRule() }
        catch (e: Exception) { tokenIter.restoreState(savedState) }

        // varName ( '[' expression ']' )?
        savedState = tokenIter.getCurState()
        try {
            val varName = varNameRule() // catch exception from here on grammar mismatch
            if (tokenIter.hasNext()) {
                savedState = tokenIter.getCurState()
                val t2 = tokenIter.next()
                if (grammarMatch(t2,"\\[",false)) {
                    val arrayExpression = expressionRule()
                    val t4 = tokenIter.getNextTokOrThrowExcp()
                    grammarMatch(t4, "\\]")
                    return VarNameWithArray(varName, arrayExpression)
                }
                // disregard token read into t2 ( != "[" )
                else {
                    tokenIter.restoreState(savedState)
                }
            }
            // no opening [ after varName
            return VarName(varName)
        }
        // exception: from firstTerm = varNameRule(); however, exception could also be thrown from other
        // following function calls in try block. Solution = create exception type for each rule
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // unaryOp term
        savedState = tokenIter.getCurState()
        try {
            val unaryOp = unaryOpRule() // only catch exception thrown from here
            val term = termRule()
            return UnaryOpTerm(unaryOp, term)
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // '(' expression ')'
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expression = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        return expression
    }

    // 2 options:
    // option 1:    subroutineName '(' expressionList ')'
    // option 2:    (className | varName) '.' subroutineName '(' expressionList ')'
    private fun subroutineCallRule(): SubroutineCall {
        val savedState = tokenIter.getCurState()
        // option 1:    subroutineName '(' expressionList ')'
        try {
            val subroutineName = subroutineNameRule()
            val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(leftParenthesisTok, "\\(") // exception thrown if nexTok == '.'
            val expressionList = expressionListRule()
            val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(rightParenthesisTok, "\\)")
            return SubroutineCall(null, subroutineName, expressionList)
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // ( className | varName ) '.' subroutineName '(' expressionList ')'
        val classOrVarNameTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(classOrVarNameTok, "$r_className|$r_varName")
        val dotTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(dotTok, "\\.")
        val subroutineName = subroutineNameRule()
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expressionList = expressionListRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")

        return SubroutineCall(classOrVarNameTok.body, subroutineName, expressionList)
    }

    // ( expression (',' expression)* )?
    private fun expressionListRule(): ArrayList<ExpressionTree> {
        val expressionList = arrayListOf<ExpressionTree>()
        var savedState = tokenIter.getCurState()
        try {
            expressionList.add( expressionRule() )
        }
        // expression list is blank
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
            return expressionList
        }

        try {
            savedState = tokenIter.getCurState()
            var nextTok: Token = tokenIter.getNextTokOrThrowExcp()
            while (grammarMatch(nextTok, ",")) {
                expressionList.add( expressionRule() )
                savedState = tokenIter.getCurState()
                nextTok = tokenIter.getNextTokOrThrowExcp()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        return expressionList
    }

    // '+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='
    private fun opRule(): ExpressionTreeChild {
        return when (tokenIter.getNextTokOrThrowExcp().body) {
            "+" -> Op( BINARY_OP.PLUS )
            "-" -> Op( BINARY_OP.MINUS )
            "*" -> Op( BINARY_OP.MULTIPLY )
            "/" -> Op( BINARY_OP.DIVIDE )
            "&" -> Op( BINARY_OP.AND)
            "|" -> Op( BINARY_OP.OR )
            "<" -> Op( BINARY_OP.LESS_THAN )
            ">" -> Op( BINARY_OP.GREATER_THAN )
            "=" -> Op(BINARY_OP.EQUALS)
            else -> throw Exception("mismatch in OpRule")
        }
    }

    // '-' | '~'
    private fun unaryOpRule(): UNARY_OP {
        return when (tokenIter.getNextTokOrThrowExcp().body) {
            "-" -> UNARY_OP.MINUS
            "~" -> UNARY_OP.TILDA
            else -> throw Exception("no match in unaryOpRule")
        }
    }

    // 'true' | 'false' | 'null' | 'this'
    private fun keywordConstantRule(): KeywordConstant {
        val keyword: KEYWORD =
                when (tokenIter.getNextTokOrThrowExcp().body.toLowerCase()) {
                    "true" -> KEYWORD.TRUE
                    "false" -> KEYWORD.FALSE
                    "null" -> KEYWORD.NULL
                    "this" -> KEYWORD.THIS
                    else -> throw Exception("no match in keywordConstantRule()")
        }
        return KeywordConstant(keyword)
    }

    // decimal number between 0 and 32767
    private fun integerConstantRule(): IntegerConstant {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        if (t1.tokenType == TOKEN_TYPE.INTEGER_CONSTANT &&
                t1.body.toInt() in 0..32767)
            return IntegerConstant(t1.body.toInt())
        else
            throw Exception("number not in valid range 0..32767")
    }

    // '"' (a sequence of unicode chars except double-quote and newline) '"'
    private fun stringConstantRule(): StringConstant {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        if (!Regex("[^\n\"]*").matches(t1.body) || t1.tokenType != TOKEN_TYPE.STRING_CONSTANT)
            throw Exception("mismatch in stringConstantRule()")
        return StringConstant(t1.body)
    }

    // if body in token matches rule string, returns true, else throws exception unless told not to raise exception, in
    // which case false is returned
    private fun grammarMatch(tok: Token, rule: String, raiseExceptionOnMismatch: Boolean = true): Boolean {
        if (!Regex(rule).matches(tok.body)) {
            if (raiseExceptionOnMismatch)
                throw Exception("rule: $rule does not match token: $tok")
            return false
        }
        return true
    }
}