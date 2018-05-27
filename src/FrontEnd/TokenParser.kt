package FrontEnd

import Misc.toArr

/**
 * Created by (442377900) on 18-May-18.
 */

class TokenParser(private val tokenIter: TokenIterator) {

    val parsedAST: TokenAST

    init {
        parsedAST = parse()
    }

    override fun toString(): String {
        return tokenASTPrinter(parsedAST)
    }

    private fun parse(): TokenAST {
        // all files must start with class declaration
        val ast = classRule()
        if (tokenIter.hasNext())
            throw Exception("code does not conform to grammar: finished parsing with remaining unparsed tokens")
        return ast
    }

    private fun classRule(): TokenAST {
        val classTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(classTok, "class")

        val classNameAST = classNameRule()

        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, Regex.escape("{"))

        val variableLenClassVarDecASTs: ArrayList<TokenAST> = arrayListOf()
        // keep parsing classVarDec grammar elements until no more matches
        var savedState = tokenIter.getCurState()
        try {
            while (true) {
                val curClassVarDecAST = classVarDecRule()
                variableLenClassVarDecASTs.add(curClassVarDecAST)
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        val subroutineDecAST = subroutineDecRule()

        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()

        return TokenWithChildren(TokenBase(TOKEN_TYPE.CLASS), classTok, classNameAST, leftBracketTok,
                *variableLenClassVarDecASTs.toArr(), subroutineDecAST, rightBracketTok)
    }

    private fun classVarDecRule(): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, "(static|field)")
        val typeAST = typeRule()
        val varNameAST = varNameRule()

        // use list to place elements matching (',' varName)*
        val variableLenOptionalParams: ArrayList<TokenAST> = ArrayList()
        var nextTok = tokenIter.getNextTokOrThrowExcp()
        while (grammarMatch(nextTok,",")) {
            val varNameTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(varNameTok, r_varName)
            variableLenOptionalParams.add(nextTok)
            variableLenOptionalParams.add(varNameTok)
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }
        grammarMatch(nextTok, ";")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.CLASS_VAR_DEC), t1, typeAST, varNameAST,
                *variableLenOptionalParams.toArr(), nextTok)
    }

    private fun typeRule(): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, r_type)
        return TokenWithChildren(TokenBase(TOKEN_TYPE.TYPE), t1)
    }

    private fun subroutineDecRule(): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, "constructor|function|method")

        val t2 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t2, "void|$r_type")

        val subroutineNameAST = subroutineNameRule()

        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")

        val parameterListAST = parameterListRule()

        val rightParanthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParanthesisTok, "\\)")

        val subroutineBodyAST = subroutineBodyRule()

        return TokenWithChildren(TokenBase(TOKEN_TYPE.SUBROUTINE_DECL), t1, t2,
                subroutineNameAST, leftParenthesisTok, parameterListAST,
                rightParanthesisTok, subroutineBodyAST)
    }

    private fun parameterListRule(): TokenAST {
        val typeAST: TokenAST
        var savedState = tokenIter.getCurState()
        try {
            typeAST = typeRule()
        }
        catch (e: Exception) {
            // rollback iterator to previous Token, since type not matched
            tokenIter.restoreState(savedState)
            // empty child list for token AST
            return TokenBase(TOKEN_TYPE.PARAMETER_LIST)
        }

        val varNameAST = varNameRule()

        // (',' varName)*
        val variableLenOptionalParams: ArrayList<TokenAST> = ArrayList()
        savedState = tokenIter.getCurState()
        var nextTok = tokenIter.getNextTokOrThrowExcp()
        while (grammarMatch(nextTok,",", false)) {
            val curTypeTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(curTypeTok, r_type)
            val curVarNameTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(curVarNameTok, r_varName)
            variableLenOptionalParams.add(nextTok) // comma
            variableLenOptionalParams.add(curTypeTok)
            variableLenOptionalParams.add(curVarNameTok)

            savedState = tokenIter.getCurState()
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }

        // no comma matched
        tokenIter.restoreState(savedState)

        return TokenWithChildren(TokenBase(TOKEN_TYPE.PARAMETER_LIST), typeAST, varNameAST, *variableLenOptionalParams.toArr())
    }

    private fun subroutineBodyRule(): TokenAST {
        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")

        // save which token we're at, since if varDec doesn't match, we'll rollback
        var savedState: Int = tokenIter.getCurState()
        val variableLenVarDecArrList: ArrayList<TokenAST> = ArrayList()
        try {
            while (true) {
                variableLenVarDecArrList.add( varDecRule() )
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            // roll back token iterator to before varDec
            tokenIter.restoreState(savedState)
        }

        val statementsAST = statementsRule()

        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.SUBROUTINE_BODY), leftBracketTok, *variableLenVarDecArrList.toArr(),
                statementsAST, rightBracketTok)
    }

    private fun varDecRule(): TokenAST {
        val varTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(varTok, "var")

        val typeAST = typeRule()
        val varNameAST = varNameRule()

        // use queue to place elements matching (',' varName)*
        val variableLenOptionalParams: ArrayList<TokenAST> = ArrayList()
        var nextTok = tokenIter.getNextTokOrThrowExcp()

        while (grammarMatch(nextTok,",", false)) {
            val varNameTok2AST = varNameRule()
            variableLenOptionalParams.add(nextTok) // adds the comma
            variableLenOptionalParams.add(varNameTok2AST)
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }

        grammarMatch(nextTok, ";")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.VAR_DECL), varTok, typeAST, varNameAST,
                *variableLenOptionalParams.toArr(), nextTok)
    }

    private fun singleRightSideRuleHelper(tokType: TOKEN_TYPE, matchStr: String): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(t1, matchStr)
        return TokenWithChildren(TokenBase(tokType), t1)
    }

    private fun classNameRule(): TokenAST = singleRightSideRuleHelper(TOKEN_TYPE.CLASS_NAME, l_identifier)

    private fun subroutineNameRule(): TokenAST = singleRightSideRuleHelper(TOKEN_TYPE.SUBROUTINE_NAME, l_identifier)

    private fun varNameRule(): TokenAST = singleRightSideRuleHelper(TOKEN_TYPE.VAR_NAME, l_identifier)

    private fun statementsRule(): TokenAST {
        val variableLenStatementParams: ArrayList<TokenAST> = ArrayList()
        var savedState = tokenIter.getCurState()
        try {
            // interpret as many statement(s) as possible until exception thrown
            while (true) {
                variableLenStatementParams.add(statementRule())
                savedState = tokenIter.getCurState()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }
        return TokenWithChildren(TokenBase(TOKEN_TYPE.STATEMENTS), *variableLenStatementParams.toArr())
    }


    private fun statementRule() : TokenAST {
        var t1: TokenAST?

        t1 = tryCatchRollback(TOKEN_TYPE.STATEMENT, this::letStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(TOKEN_TYPE.STATEMENT, this::ifStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(TOKEN_TYPE.STATEMENT, this::whileStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(TOKEN_TYPE.STATEMENT, this::doStatementRule)
        if (t1 != null) return t1

        t1 = tryCatchRollback(TOKEN_TYPE.STATEMENT, this::returnStatementRule)
        if (t1 != null) return t1

        // if no matches, raise exception
        throw Exception("no match found for ${tokenIter.getCurrent()}")
    }

    private fun letStatementRule(): TokenAST {
        val letTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(letTok, "let")
        val varNameAST = varNameRule()

        var nextTok = tokenIter.getNextTokOrThrowExcp()

        var optionalExpressionParamList: Array<TokenAST> = arrayOf()
        if (grammarMatch(nextTok, "\\[", false)) {
            val expressionAST = expressionRule()
            val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(rightBracketTok, "\\]")
            optionalExpressionParamList = arrayOf(nextTok, expressionAST, rightBracketTok)
            nextTok = tokenIter.getNextTokOrThrowExcp()
        }
        grammarMatch(nextTok, "=")
        val expression2AST = expressionRule()
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.LET_STATEMENT), letTok, varNameAST, *optionalExpressionParamList,
                nextTok, expression2AST, semicolonTok)
    }

    private fun ifStatementRule() : TokenAST {
        val ifTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(ifTok, "if")
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expressionAST = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")
        val statementsAST = statementsRule()
        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        var optionalElseParamArr: Array<TokenAST> = arrayOf()
        val savedState = tokenIter.getCurState()
        val nextTok = tokenIter.getNextTokOrThrowExcp()
        if(grammarMatch(nextTok, "else",false)) {
            val t1 = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(t1, "\\{")
            val elseStatementsAST = statementsRule()
            val t2 = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(t2, "\\}")
            optionalElseParamArr = arrayOf(nextTok, t1, elseStatementsAST, t2)
        }
        else
            tokenIter.restoreState(savedState)

        return TokenWithChildren(TokenBase(TOKEN_TYPE.IF_STATEMENT), ifTok, leftParenthesisTok, expressionAST,
                rightParenthesisTok, leftBracketTok, statementsAST, rightBracketTok, *optionalElseParamArr)
    }

    private fun whileStatementRule() : TokenAST {
        val whileTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(whileTok, "while")
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expressionAST = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        val leftBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftBracketTok, "\\{")
        val statementsAST = statementsRule()
        val rightBracketTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightBracketTok, "\\}")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.WHILE_STATEMENT), whileTok, leftParenthesisTok,
                expressionAST, rightParenthesisTok, leftBracketTok, statementsAST, rightBracketTok)
    }

    private fun doStatementRule(): TokenAST {
        val doTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(doTok, "do")
        val subroutineCallAST = subroutineCallRule()
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.DO_STATEMENT), doTok, subroutineCallAST, semicolonTok)
    }

    private fun returnStatementRule() : TokenAST {
        val returnTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(returnTok, "return")

        val savedState = tokenIter.getCurState()
        var optionalExpression: Array<TokenAST> = arrayOf()
        try {
            optionalExpression = arrayOf( expressionRule() )
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }
        val semicolonTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(semicolonTok, ";")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.RETURN_STATEMENT), returnTok, *optionalExpression,
                semicolonTok)
    }
    // term (op term)*
    private fun expressionRule(): TokenAST {
        val termAST = termRule()

        // (op term)*
        val optionalTerms: ArrayList<TokenAST> = arrayListOf()
        var savedState = tokenIter.getCurState()
        try {
            while (true) {
                val curOpAST = opRule()
                val curTermAST = termRule()
                optionalTerms.add(curOpAST)
                optionalTerms.add(curTermAST)
                savedState = tokenIter.getCurState()
            }
        }
        // fix: catch only opRule exception (termRule exception should not cause restore)
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        return TokenWithChildren(TokenBase(TOKEN_TYPE.EXPRESSION), termAST, *optionalTerms.toArr())
    }

    private fun tryCatchRollback(tokenASTHeadName: TOKEN_TYPE, ruleFunc: () -> TokenAST): TokenAST? {
        val savedState = tokenIter.getCurState()
        return try {
            TokenWithChildren(TokenBase(tokenASTHeadName), ruleFunc())
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
            null
        }
    }

    private fun termRule(): TokenAST {
        var t1AST: TokenAST?

        t1AST = tryCatchRollback(TOKEN_TYPE.TERM, this::integerConstantRule)
        if (t1AST != null) return t1AST

        t1AST = tryCatchRollback(TOKEN_TYPE.TERM, this::subroutineCallRule)
        if (t1AST != null) return t1AST

        t1AST = tryCatchRollback(TOKEN_TYPE.TERM, this::stringConstantRule)
        if (t1AST != null) return t1AST

        t1AST = tryCatchRollback(TOKEN_TYPE.TERM, this::keywordConstantRule)
        if (t1AST != null) return t1AST

        // varName ( '[' expression ']' )?
        var savedState = tokenIter.getCurState()
        try {
            t1AST = varNameRule() // catch exception from here on grammar mismatch
            if (tokenIter.hasNext()) {
                savedState = tokenIter.getCurState()
                val t2 = tokenIter.next()
                if (grammarMatch(t2,"\\[",false)) {
                    val t3ExpressionAST = expressionRule()
                    val t4 = tokenIter.getNextTokOrThrowExcp()
                    grammarMatch(t4, "\\]")
                    return TokenWithChildren(TokenBase(TOKEN_TYPE.TERM), t1AST, t2, t3ExpressionAST, t4)
                }
                // disregard token read into t2 ( != "[" )
                else
                    tokenIter.restoreState(savedState)
            }
            // no opening [ after varName
            return TokenWithChildren(TokenBase(TOKEN_TYPE.TERM), t1AST)
        }
        // exception exception: from t1AST = varNameRule(); however, exception could also be thrown from other
        // following function calls in try block. Solution = create exception type for each rule
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // unaryOp term
        savedState = tokenIter.getCurState()
        try {
            t1AST = unaryOpRule() // only catch exception thrown from here
            val t2 = termRule()
            return TokenWithChildren(TokenBase(TOKEN_TYPE.TERM), t1AST, t2)
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // '(' expression ')'
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expressionAST = expressionRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")
        return TokenWithChildren(TokenBase(TOKEN_TYPE.TERM), leftParenthesisTok, expressionAST,
                rightParenthesisTok)
    }

    private fun subroutineCallRule(): TokenAST {
        val savedState = tokenIter.getCurState()
        // subroutineName '(' expressionList ')'
        try {
            val subroutineNameAST = subroutineNameRule()
            val leftParanthesisTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(leftParanthesisTok, "\\(") // catch exception thrown from here if nexTok == '.'
            val expressionListAST = expressionListRule()
            val rightParanthesisTok = tokenIter.getNextTokOrThrowExcp()
            grammarMatch(rightParanthesisTok, "\\(")

            return TokenWithChildren(TokenBase(TOKEN_TYPE.SUBROUTINE_CALL), subroutineNameAST, leftParanthesisTok,
                    expressionListAST, rightParanthesisTok)
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        // ( className | varName ) '.' subroutineName '(' expressionList ')'
        val classOrVarNameTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(classOrVarNameTok, "$r_className|$r_varName")
        val dotTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(dotTok, "\\.")
        val subroutineNameAST = subroutineNameRule()
        val leftParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(leftParenthesisTok, "\\(")
        val expressionListAST = expressionListRule()
        val rightParenthesisTok = tokenIter.getNextTokOrThrowExcp()
        grammarMatch(rightParenthesisTok, "\\)")

        return TokenWithChildren(TokenBase(TOKEN_TYPE.SUBROUTINE_CALL), classOrVarNameTok,
                dotTok, subroutineNameAST, leftParenthesisTok, expressionListAST,
                rightParenthesisTok)
    }

    // ( expression (',' expression)* )?
    private fun expressionListRule(): TokenAST {
        var savedState = tokenIter.getCurState()
        val expressionAST : TokenAST
        try {
            expressionAST = expressionRule()
        }
        // expression list is blank
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
            return TokenWithChildren(TokenBase(TOKEN_TYPE.EXPRESSION_LIST))
        }

        val variableLenOptionalParams: ArrayList<TokenAST> = ArrayList()
        try {
            savedState = tokenIter.getCurState()
            var nextTok: Token = tokenIter.getNextTokOrThrowExcp()
            while (grammarMatch(nextTok, ",")) {
                val curExpressionAST = expressionRule()
                variableLenOptionalParams.add(nextTok)
                variableLenOptionalParams.add(curExpressionAST)

                savedState = tokenIter.getCurState()
                nextTok = tokenIter.getNextTokOrThrowExcp()
            }
        }
        catch (e: Exception) {
            tokenIter.restoreState(savedState)
        }

        return TokenWithChildren(TokenBase(TOKEN_TYPE.EXPRESSION_LIST), expressionAST, *variableLenOptionalParams.toArr())
    }

    private fun opRule(): TokenAST
            = singleRightSideRuleHelper(TOKEN_TYPE.OP, "\\+|-|\\*|/|&|\\||<|>|=")

    private fun unaryOpRule(): TokenAST
            = singleRightSideRuleHelper(TOKEN_TYPE.UNARY_OP, "-|\\+")

    private fun keywordConstantRule(): TokenAST
            = singleRightSideRuleHelper(TOKEN_TYPE.KEYWORD_CONSTANT, "true|false|null|this")

    private fun integerConstantRule(): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        if (t1.tokenType == TOKEN_TYPE.INTEGER_CONSTANT &&
                t1.body.toInt() in 0..32767)
            return t1
        else
            throw Exception("number not in valid range 0..32767")
    }

    private fun stringConstantRule(): TokenAST {
        val t1 = tokenIter.getNextTokOrThrowExcp()
        if (!Regex("[^\n\"]*").matches(t1.body) || t1.tokenType != TOKEN_TYPE.STRING_CONSTANT)
            throw Exception("not a stringConstant")
        return TokenWithChildren(TokenBase(TOKEN_TYPE.STRING_CONSTANT), t1)
    }

    private fun grammarMatch(tok: Token, rule: String, raiseExceptionOnMismatch: Boolean = true): Boolean {
        if (!Regex(rule).matches(tok.body)) {
            if (raiseExceptionOnMismatch)
                throw Exception("rule: $rule does not match token: $tok")
            return false
        }
        return true
    }
}