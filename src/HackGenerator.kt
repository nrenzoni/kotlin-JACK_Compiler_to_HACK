/**
 * Created by (442377900) on 07-Mar-18.
 */

enum class REGISTER {
    LOCAL, ARGUMENT, THIS, THAT, TEMP, STATIC, POINTER, CONSTANT
}

enum class MATH_OP {
    ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
}

class HACKCodeGen(protected var filename: String, protected val printDebugMsg: Boolean,
                  val generateBootstrapCode: Boolean = false) {
    // classname needed for static push and pop

    protected val registerMapping =
            hashMapOf<REGISTER,Int>(REGISTER.LOCAL to 1, REGISTER.ARGUMENT to 2, REGISTER.THIS to 3, REGISTER.THAT to 4,
                    REGISTER.TEMP to 5, REGISTER.STATIC to 16, REGISTER.POINTER to 3)

    protected var stackIndex: Int = 256
    protected var heapIndex: Int  = 2048
    var code: StringBuilder = StringBuilder()
        private set

    init {
        if (generateBootstrapCode)
            appendBootstrapCode()
    }

    protected var labelCounter = 0

    private fun appendLineToCode(hack_op: String) {
        code.append(hack_op + "\n")
    }
    
    private fun appendBootstrapCode() {
        appendDbgMsg("// initializing stack to 256")

        appendLineToCode("@256")
        appendLineToCode("D = A")
        appendLineToCode("@SP")
        appendLineToCode("M = D")
        callHACK("Sys.init", 0, "call Sys.init 0")
    }

    // sp must be free before using function, since sp points at next free slot on stack
    private fun pushDToAddrsOfSPHelper() {
        appendLineToCode("@SP")
        appendLineToCode("A = M")
        appendLineToCode("M = D")
    }

    private fun appendDbgMsg(debugMsg: String?) {
        if (!printDebugMsg || debugMsg == null) {
            return
        }
        appendLineToCode("\n// $debugMsg")
    }

    fun pushHACK(register: REGISTER, regOffsetORConst: Int, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        val regIndex = registerMapping[register]

        when (register) {

            REGISTER.CONSTANT -> {
                // put constant in D
                appendLineToCode("@${regOffsetORConst}")
                appendLineToCode("D = A")
            }

            REGISTER.LOCAL, REGISTER.ARGUMENT, REGISTER.THIS, REGISTER.THAT -> {
                //D=M=RAM[RAM[register]+regoffset]
                when (register) {
                    REGISTER.LOCAL -> appendLineToCode("@LCL")
                    REGISTER.ARGUMENT -> appendLineToCode("@ARG")
                    REGISTER.THIS -> appendLineToCode("@THIS")
                    REGISTER.THAT -> appendLineToCode("@THAT")
                }
                appendLineToCode("D = M")
                appendLineToCode("@${regOffsetORConst}")
                appendLineToCode("A = D + A")
                appendLineToCode("D = M")
            }

            REGISTER.TEMP -> {
                //D=RAM[TEMP+X]
                val offset = regIndex?.plus(regOffsetORConst)
                appendLineToCode("@${offset}")
                appendLineToCode("D = M")
            }

            REGISTER.STATIC -> {
                //D=RAM[CLASS.X]
                appendLineToCode("@${filename}_${regOffsetORConst}")
                appendLineToCode("D = M")
            }

            REGISTER.POINTER->{
                if(regOffsetORConst != 0 && regOffsetORConst != 1)
                    throw Exception("Only Pointer 0 or 1")
                //D=RAM[regIndex+regoffset]
                val offset = regIndex?.plus(regOffsetORConst)
                appendLineToCode("@${offset}")
                appendLineToCode("D = M")
            }
        }

        pushDToAddrsOfSPHelper()
        // increment stack pointer
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")
        stackIndex++
    }

    fun popHACK(register: REGISTER, regOffset: Int, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        if(stackIndex < 256)
            throw Exception("cannot pop off of stack, already at lowest index")

        val regIndex = registerMapping[register]

        // decrement SP to point at top of stack value (since SP points at next free spot)
        appendLineToCode("@SP")
        appendLineToCode("M = M - 1")
        appendLineToCode("A = M")
        //D=RAM[SP-1]
        appendLineToCode("D = M")


        when (register) {
            // Group 1 (local, argument, this, that)
            REGISTER.LOCAL, REGISTER.ARGUMENT, REGISTER.THIS, REGISTER.THAT -> {
                // A = RAM[regIndex]
                when (register) {
                    REGISTER.LOCAL -> appendLineToCode("@LCL")
                    REGISTER.ARGUMENT -> appendLineToCode("@ARG")
                    REGISTER.THIS -> appendLineToCode("@THIS")
                    REGISTER.THAT -> appendLineToCode("@THAT")
                }
                appendLineToCode("A = M")

                // A = RAM[regIndex] + regOffset
                for(i in 1..regOffset)
                    appendLineToCode("A = A + 1")

                // RAM[ RAM[regIndex] + regOffset ] =RAM[SP-1]
                appendLineToCode("M = D")
            }

            // Group 2 (temp)
            REGISTER.TEMP -> {
                // implement: check that regOffset does not overflow / underflow

                // temp_reg + offset
                val offset = regIndex?.plus(regOffset)
                // *(temp_reg_ base + offset) = top of stack value
                appendLineToCode("@${offset}")
                appendLineToCode("M = D")
            }

            // Group 3 (static)
            REGISTER.STATIC -> {
                //RAM[CLASS_NAME.X]= RAM[SP-1]
                appendLineToCode("@${filename}_${regOffset}") // if X = 0 and filename is the first class it's
                // like to write @16
                appendLineToCode("M = D")
            }

            // Group 4 (pointer 0, pointer 1)
            REGISTER.POINTER -> {
                if(regOffset != 0 && regOffset != 1)
                    throw Exception("Only Pointer 0 or 1 allowed, got ${regOffset}")
                val offset : Int? = registerMapping[REGISTER.POINTER]?.plus(regOffset)
                appendLineToCode("@${offset}")

                //RAM[POINTER 0 OR 1] = RAM[SP-1]
                appendLineToCode("M = D")
            }
        }

        stackIndex--

    }

    fun mathOpHACK(op_type: MATH_OP, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        when (op_type) {
            MATH_OP.ADD, MATH_OP.SUB, MATH_OP.AND, MATH_OP.OR -> binaryMathOpHACK(op_type)
            MATH_OP.NEG, MATH_OP.NOT                          -> unaryMathOpHACK(op_type)
            MATH_OP.EQ, MATH_OP.GT, MATH_OP.LT                -> cmpOpHACK(op_type)
        }
    }

    private fun unaryMathOpHACK(op_type: MATH_OP) {

        appendLineToCode("@SP")
        // A = address of top of stack item
        appendLineToCode("A = M - 1")
        // D = value at top of stack
        appendLineToCode("D = M")

        when(op_type) {
            MATH_OP.NEG -> appendLineToCode("D = -D")
            MATH_OP.NOT -> appendLineToCode("D = !D")
            else -> throw Exception("unaryMathOpHACK() can only process a unary math operation")
        }

        appendLineToCode("M = D")
    }

    private fun binaryMathOpHACK(op_type: MATH_OP) {

        appendLineToCode("@SP")
        // sp-- (to point at value on top of stack address)
        appendLineToCode("M = M - 1")
        // A = stack address
        appendLineToCode("A = M")
        // D = value at top of stack
        appendLineToCode("D = M")
        appendLineToCode("@SP")
        // sp-- (to point at 2nd to top of stack address)
        appendLineToCode("M = M - 1")
        // A = address of 2nd to top of stack
        appendLineToCode("A = M")

        // D holds top of stack value, A holds 2nd to top address

        when(op_type) {
            MATH_OP.ADD -> appendLineToCode("M = D + M")
            MATH_OP.SUB -> appendLineToCode("M = M - D")
            MATH_OP.AND -> appendLineToCode("M = D & M")
            MATH_OP.OR -> appendLineToCode("M = D | M")
            else -> throw Exception("binaryMathOpHACK() can only process a binary math operation")
        }

        // sp++ (sp should point at next free space)
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")

        // stackIndex goes down by 1 after binary math op since 2 pops of operands and 1 push of result
        stackIndex--
    }

    private fun cmpOpHACK(op_type: MATH_OP) {

        val lab_true = labelCounter
        val lab_end = labelCounter++

        appendLineToCode("@SP")
        // sp-- (to point at value on top of stack address)
        appendLineToCode("M = M - 1")
        // A = stack address
        appendLineToCode("A = M")
        // D = value at top of stack
        appendLineToCode("D = M")
        appendLineToCode("@SP")
        // sp-- (to point at 2nd to top of stack address)
        appendLineToCode("M = M - 1")
        // A = address of 2nd to top of stack
        appendLineToCode("A = M")

        // D = ( 2nd to last value on stack ) - ( top of stack value )
        appendLineToCode("D = M - D")

        // A = true condition label; will push 1 to stack. otherwise 0 pushed to stack
        appendLineToCode("@CMP_TRUE_${lab_true}")

        when(op_type) {
            MATH_OP.EQ -> appendLineToCode("D; JEQ")
            MATH_OP.GT -> appendLineToCode("D; JGT")
            MATH_OP.LT -> appendLineToCode("D; JLT")
            else       -> throw Exception("cmpOpHACK() can only process a logical operation")
        }

        // if code flow arrives here during HACK runtime, cmp operation is false; push 0 to stack
        appendLineToCode("@SP")
        appendLineToCode("A = M")
        appendLineToCode("M = 0")
        // jmp to end of routine
        appendLineToCode("@CMP_END_${lab_end}")
        appendLineToCode("0; JMP")

        // true condition branch; push -1 to stack
        appendLineToCode("(CMP_TRUE_${lab_true})")
        appendLineToCode("@SP")
        appendLineToCode("A = M")
        appendLineToCode("M = -1")

        // both branch flows end up here
        appendLineToCode("(CMP_END_${lab_end})")
        // increment sp to point to next free stack slot
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")

        // 2 pops, 1 push
        stackIndex--
    }

    // used by label, goto, and if-goto vm commands, not by call, function, nor return vm commands
    private fun genLabelWithFileBaseStrHelper(label: String) = "${filename}_${label}"

    fun labelHACK(labelName: String, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)
        appendLineToCode( "(" + genLabelWithFileBaseStrHelper(labelName) + ")" )
    }

    fun gotoHACK(labelName: String, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        appendLineToCode( "@" + genLabelWithFileBaseStrHelper(labelName))
        appendLineToCode("0; JMP")
    }

    fun ifGotoHACK(labelName: String, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        //if the head of the stack not equal to 0 jump
        //D=RAM[SP-1]
        appendLineToCode("@SP")
        appendLineToCode("M = M - 1")
        appendLineToCode("A = M")
        appendLineToCode("D = M")
        //jump if not equal to zero
        appendLineToCode( "@" + genLabelWithFileBaseStrHelper(labelName) )
        appendLineToCode("D; JNE")

        //pop 1
        stackIndex--
    }

    fun callHACK(funcName: String, varCount: Int, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        fun pushValueHelper(pointerName: String) {
            when(pointerName) {
                "LCL","ARG","THIS","THAT" -> {
                    appendLineToCode("@${pointerName}")
                    appendLineToCode("D = M")
                }
                else                      -> {
                    appendLineToCode("@${pointerName}")
                    //put the return address in D
                    appendLineToCode("D = A")
                }
            }
            pushDToAddrsOfSPHelper()
            //SP++
            appendLineToCode("@SP")
            appendLineToCode("M = M + 1")
            stackIndex++
        }

        val label = labelCounter++

        //push the return address
        pushValueHelper(funcName + "_ReturnAddress_" + label)
        // save pointers of calling function
        pushValueHelper("LCL")
        pushValueHelper("ARG")
        pushValueHelper("THIS")
        pushValueHelper("THAT")
        //ARG = SP-varCount-5
        // @sp from call to pushValueHelper("THAT")
        appendLineToCode("D = M")
        // only subtract varCount from SP if != 0
        if (varCount > 0) {
            appendLineToCode("@${varCount}")
            appendLineToCode("D = D - A")
        }
        appendLineToCode("@5")
        appendLineToCode("D = D - A")
        appendLineToCode("@ARG")
        appendLineToCode("M = D")
        //LCL = SP
        appendLineToCode("@SP")
        appendLineToCode("D = M")
        appendLineToCode("@LCL")
        appendLineToCode("M = D")
        //run the function
        appendLineToCode("@$funcName")
        appendLineToCode("0; JMP")
        //create label for return address. the label will be recognized even though label is written after the goto
        appendLineToCode("(${funcName}_ReturnAddress_${label})")
    }

    fun functionHACK(funcName: String, localCount: Int, debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        if (localCount < 0)
            throw Exception("localCount must be >= 0, got $localCount")

        appendLineToCode("(${funcName})")

        // only initialize local variables if they exist
        if (localCount > 0) {
            // D = local variable count
            appendLineToCode("@${localCount}")
            appendLineToCode("D = A")
            // jump over initialization of locals if localCount == 0
            appendLineToCode("@${funcName}_loopEnd")
            appendLineToCode("D; JEQ")
            // initialize local variables to 0
            appendLineToCode("(${funcName}_loop)")
            appendLineToCode("@SP")
            appendLineToCode("A = M")
            appendLineToCode("M = 0")
            appendLineToCode("@SP")
            appendLineToCode("M = M + 1")
            appendLineToCode("@${funcName}_loop")
            // keep looping as long as localCount > 0
            appendLineToCode("D = D - 1; JNE")
            // end when localCount == 0
            appendLineToCode("(${funcName}_loopEnd)")
        }
    }

    fun returnHACK(debugMsg: String? = null) {
        appendDbgMsg(debugMsg)

        // frame = LCL
        appendLineToCode("@LCL")
        appendLineToCode("D = M")
        // ret = *(frame - 5). store ret value in ram[13] (a gp register)
        appendLineToCode("@5")
        appendLineToCode("A = D - A")
        appendLineToCode("D = M")
        appendLineToCode("@13")
        appendLineToCode("M = D")
        // &arg = pop()
        appendLineToCode("@SP")
        appendLineToCode("M = M - 1")
        appendLineToCode("A = M")
        appendLineToCode("D = M")
        appendLineToCode("@ARG")
        appendLineToCode("A = M")
        appendLineToCode("M = D")
        // restore sp to before function call
        // sp = arg+1
        appendLineToCode("@ARG")
        appendLineToCode("D = M")
        appendLineToCode("@SP")
        appendLineToCode("M = D + 1")

        // side-affect of function: decrements LCL by 1
        fun assignLCLMinusOneToReg(regName: String) {
            appendLineToCode("@LCL")
            appendLineToCode("M = M - 1")
            appendLineToCode("A = M")
            appendLineToCode("D = M")
            appendLineToCode("@$regName")
            appendLineToCode("M = D")
        }

        // that = *(frame - 1)
        assignLCLMinusOneToReg("THAT")
        // this = *(frame - 2)
        assignLCLMinusOneToReg("THIS")
        // arg = (*frame - 3)
        assignLCLMinusOneToReg("ARG")
        // lcl = *(frame - 4)
        assignLCLMinusOneToReg("LCL")

        // goto ret
        appendLineToCode("@13")
        appendLineToCode("A = M")
        appendLineToCode("0; JMP")
    }
}