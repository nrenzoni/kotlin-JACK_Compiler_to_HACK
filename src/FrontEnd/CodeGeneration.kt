package FrontEnd

/**
 * Created by (442377900) on 31-May-18.
 */

// output to vm language
// assume classToken is well-formed
// one CodeGeneration object per file (each TokenParser contains one TokenAST)
class CodeGeneration(tokenParser: TokenParser) {

    private val classToken = tokenParser.parsedAST
    private val className = classToken.className

    private val outputVMCode = StringBuilder()

    override fun toString(): String {
        return outputVMCode.toString()
    }

    private val staticSymbolTable       = SymbolTable()
    private val classVarSymbolTable     = SymbolTable()
    private var methodSymbolTable       = SymbolTable()
    private var ifCounter = 0
    private var whileCounter = 0

    init {
        generateClassCode()
    }

    private fun appendCode(code: String) = outputVMCode.append("$code\n")

    private fun getSymbol(name: String): Symbol? {
        return methodSymbolTable.getSymbol(name) ?:
            classVarSymbolTable.getSymbol(name)  ?:
            staticSymbolTable.getSymbol(name)
    }

    private fun generateClassCode() {
        for (classVarDec in classToken.classVarDecList) {
            for (id in classVarDec.varNameList) {
                when (classVarDec.idKind) {
                    ID_KIND.STATIC ->
                        staticSymbolTable.addIdentifier(id, classVarDec.idType, classVarDec.idKind)
                    ID_KIND.FIELD ->
                        classVarSymbolTable.addIdentifier(id, classVarDec.idType, classVarDec.idKind)
                    else -> throw Exception("invalid ID kind in classVarDec()")
                }
            }
        }
        for (subroutineDec in classToken.subroutineDecList) {
            methodSymbolTable.clear()
            ifCounter = 0
            whileCounter = 0
            functionRule(subroutineDec, classToken.className)
        }
    }

    private fun functionRule(subroutineDec: SubroutineDec, className: String) {
        if (subroutineDec.functionScope == FUNCTION_SCOPE.METHOD) {
            methodSymbolTable.addIdentifier("this", className, ID_KIND.ARG)
        }

        for (localParameter in subroutineDec.parameterList) {
            methodSymbolTable.addIdentifier(localParameter.name, localParameter.type, ID_KIND.ARG)
        }

        val localVarCount = getVarCount(subroutineDec.subroutineBody.varDecList)
        appendCode("function ${className}.${subroutineDec.name} $localVarCount")

        when (subroutineDec.functionScope) {
            FUNCTION_SCOPE.METHOD -> {
                // this pointer passed as first arg for methods
                appendCode("push argument 0")
                appendCode("pop pointer 0")
            }

            FUNCTION_SCOPE.CONSTRUCTOR -> {
                // number of field class variables (not including static)
                appendCode("push constant " + classVarSymbolTable.varCount())
                appendCode("call Memory.alloc 1")
                // place returned value from Memory.alloc in this pointer
                appendCode("pop pointer 0")
            }

            FUNCTION_SCOPE.FUNCTION -> {}
        }

        subroutineBodyRule(subroutineDec.subroutineBody)
    }

    private fun getVarCount(varDecList: ArrayList<VarDec>): Int {
        var counter = 0
        for (elem in varDecList) {
            counter += elem.nameList.size
        }
        return counter
    }

    private fun subroutineBodyRule(subroutineBody: SubroutineBody) {
        for (varDec in subroutineBody.varDecList) {
            for (id in varDec.nameList) {
                methodSymbolTable.addIdentifier(id, varDec.type, ID_KIND.VAR)
            }
        }
        for (statement in subroutineBody.statementList) {
            statementRule(statement)
        }
    }

    private fun statementRule(statement: Statement) {
        when (statement) {
            is LetStatement    -> letStatementRule(statement)
            is IfStatement     -> ifStatementRule(statement)
            is WhileStatement  -> whileStatementRule(statement)
            is DoStatement     -> doStatementRule(statement)
            is ReturnStatement -> returnStatementRule(statement)
            else -> throw Exception("when() on statement not fully defined")
        }
    }

    private fun letStatementRule(letStatement: LetStatement) {
        if (letStatement.arrayExpression != null) {
            expressionRule(letStatement.arrayExpression)
            pushNamedSymbol(letStatement.varName)
            appendCode("add")
            expressionRule(letStatement.rightSideExpression)
            // store results of right side expr in temp 0
            appendCode("pop temp 0")
            // pop address of left hand array element into that pointer
            appendCode("pop pointer 1")
            // store right hand calculated result in address where that points to
            appendCode("push temp 0")
            appendCode("pop that 0")
        }
        else {
            expressionRule(letStatement.rightSideExpression)
            popNamedSymbol(letStatement.varName)
        }
    }

    private fun ifStatementRule(ifStatement: IfStatement) {
        val curLabel = ifCounter
        val endLabel = ifCounter++
        expressionRule(ifStatement.conditionExpression)
        appendCode("if-goto IF_TRUE$curLabel")
        appendCode("goto IF_FALSE$curLabel")
        appendCode("label IF_TRUE$curLabel")
        for (statement in ifStatement.bodyStatementsList) {
            statementRule(statement)
        }
        if (ifStatement.elseStatementsList != null) {
            // only want to increment ifCounter if endLabel is actually used
            appendCode("goto IF_END$endLabel")
        }
        appendCode("label IF_FALSE$curLabel")
        if (ifStatement.elseStatementsList != null) {
            for (statement in ifStatement.elseStatementsList) {
                statementRule(statement)
            }
        }
        if (ifStatement.elseStatementsList != null)
            appendCode("label IF_END$endLabel")
    }

    private fun whileStatementRule(whileStatement: WhileStatement) {
        val label = whileCounter++

        appendCode("label WHILE_EXP$label")

        // compute ~(conditionExpression)
        expressionRule(whileStatement.conditionExpression)
        appendCode("not")

        appendCode("if-goto WHILE_END$label")
        for (statement in whileStatement.bodyStatementsList) {
            statementRule(statement)
        }
        appendCode("goto WHILE_EXP$label")
        appendCode("label WHILE_END$label")
    }

    private fun doStatementRule(doStatement: DoStatement) {
        subroutineCallRule(doStatement.subroutineCall)
        // pop returned val off stack to clear it
        appendCode("pop temp 0")
    }

    private fun returnStatementRule(returnStatement: ReturnStatement) {
        val optionalExpression = returnStatement.optionalExpression
        // if no return value, return 0
        if (optionalExpression == null) {
            appendCode("push constant 0")
        }
        else {
            expressionRule(optionalExpression)
        }
        appendCode("return")
    }

    private fun subroutineCallRule(subroutineCall: SubroutineCall) {
        var pushedPointerArg = false

        // push this for method call
        if (subroutineCall.classNameVarName == null) {
            appendCode("push pointer 0")
            pushedPointerArg = true
        }
        // if call on object, push address of object; if call on function of class, don't push any address
        else {
            val objName = subroutineCall.classNameVarName
            // if symbol exists in symbol table, push variable address
            if (getSymbol(objName) != null) {
                pushNamedSymbol(objName)
                pushedPointerArg = true
            }
            // otherwise, call is on class function (not method); no push needed
        }

        for (expression in subroutineCall.expressionList) {
            expressionRule(expression)
        }

        val argSize: Int =
                if (pushedPointerArg)
                    subroutineCall.expressionList.size + 1
                else
                    subroutineCall.expressionList.size

        val classOutputName: String =
            if (subroutineCall.classNameVarName != null)
                // first check if is variable by checking if in symbol table, in which case its type is the className,
                // otherwise it's a className already
                getSymbol(subroutineCall.classNameVarName)?.type ?: subroutineCall.classNameVarName
            else
                className

        appendCode("call $classOutputName.${subroutineCall.subroutineName} $argSize")
    }

    // function calling expressionRule() responsible for performing pop afterwards
    private fun expressionRule(expression: ExpressionTree) {
        val expressionTreeChildList = expression.expressionTreeChildList
        termRule(expressionTreeChildList[0] as Term)
        for (i in 2 until expressionTreeChildList.size step 2) {
            val curOp  = expressionTreeChildList[i-1]
            val curTerm = expressionTreeChildList[i]
            if (curTerm !is Term || curOp !is Op) { throw Exception("expressionTreeChild not built correctly") }
            termRule(curTerm)
            operatorRule(curOp.operator)
        }
    }

    private fun operatorRule(op: BINARY_OP) {
        val opStr =
                when (op) {
                    BINARY_OP.PLUS ->         "add"
                    BINARY_OP.MINUS ->        "sub"
                    BINARY_OP.MULTIPLY ->     "call Math.multiply 2"
                    BINARY_OP.DIVIDE ->       "call Math.divide 2"
                    BINARY_OP.AND ->          "and"
                    BINARY_OP.OR ->           "or"
                    BINARY_OP.LESS_THAN ->    "lt"
                    BINARY_OP.GREATER_THAN -> "gt"
                    BINARY_OP.EQUALS ->       "eq"
                }
        appendCode(opStr)
    }

    private fun pushPopHelper(name: String, isPush: Boolean) {
        val symbol = getSymbol(name) ?: throw Exception("name not found in symbol table")
        val symKind = symbol.kind
        val symIndex =
                when (symKind) {
                    ID_KIND.STATIC           -> staticSymbolTable.indexOf(name)
                    ID_KIND.FIELD            -> classVarSymbolTable.indexOf(name)
                    ID_KIND.ARG, ID_KIND.VAR -> methodSymbolTable.indexOf(name)
                } ?: throw Exception("table mismatch lookup in pushNamedSymbol()")

        when {
            // toString() of symKind used
            isPush -> appendCode("push $symKind $symIndex")
            else   -> appendCode("pop $symKind $symIndex")
        }
    }

    private fun pushNamedSymbol(name: String) = pushPopHelper(name, true)
    private fun popNamedSymbol(name: String) = pushPopHelper(name, false)

    // pushes term; function calling termRule() responsible to perform pop afterwards
    private fun termRule(term: Term) {
        when (term) {
            is IntegerConstant -> {
                appendCode("push constant ${term.integerConstant}")
            }

            is StringConstant -> {
                val strLen = term.string.length
                appendCode("push constant $strLen")
                appendCode("call String.new 1")
                for (char in term.string) {
                    // toInt() converts to ascii
                    appendCode("push constant ${char.toInt()}")
                    appendCode("call String.appendChar 2")
                }
            }

            is KeywordConstant -> {
                    when (term.keywordConstant) {
                        KEYWORD.TRUE -> {
                            appendCode("push constant 0")
                            appendCode("not")
                        }

                        KEYWORD.FALSE, KEYWORD.NULL -> appendCode("push constant 0")


                        KEYWORD.THIS -> appendCode("push pointer 0")
                    }
            }

            is VarNameWithArray-> {
                if (term.optionalArrayExpression != null) {
                    expressionRule(term.optionalArrayExpression)
                }
                pushNamedSymbol(term.varName)
                appendCode("add")
                // pointer 1 = that
                appendCode("pop pointer 1")
                // read from array
                appendCode("push that 0")
            }

            is UnaryOpTerm -> {
                termRule(term.term)
                when (term.unaryOp) {
                    UNARY_OP.MINUS -> appendCode("neg")
                    UNARY_OP.TILDA -> appendCode("not")
                }
            }

            is VarName -> pushNamedSymbol(term.varName)

            is SubroutineCall -> subroutineCallRule(term)

            is ExpressionTree -> expressionRule(term)
        }
    }
}