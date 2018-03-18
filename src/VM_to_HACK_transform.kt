/**
 * Created by (442377900) on 07-Mar-18.
 */

enum class REGISTER {
    LOCAL, ARGUMENT, THIS, THAT, TEMP, STATIC, POINTER, CONSTANT
}

enum class MATH_OP {
    ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
}

fun extractARITHType(cmd_line: String): MATH_OP {
    for ( op in MATH_OP.values() ) {
        if ( cmd_line.contains(Regex("^${op} ", RegexOption.IGNORE_CASE)) ) {
            return op
        }
    }
    throw Exception("no matching enum found in '${cmd_line}'")
}

fun extractREGISTER(cmd_line: String): REGISTER {
    for ( reg in REGISTER.values() ) {
        if ( cmd_line.contains(Regex("${reg} ", RegexOption.IGNORE_CASE)) ) {
            return reg
        }
    }
    throw Exception("no matching register found in '${cmd_line}'")
}

class HACKCodeGen(protected var className: String) {
    // classname needed for static push and pop

    init {
        initializeHACK()
    }

    protected val registerMapping =
            hashMapOf<REGISTER,Int>(REGISTER.LOCAL to 1, REGISTER.ARGUMENT to 2, REGISTER.THIS to 3, REGISTER.THAT to 4,
                    REGISTER.TEMP to 5, REGISTER.STATIC to 16, REGISTER.POINTER to 3)

    protected var stackIndex: Int = 256
    protected var heapIndex: Int  = 2048
    var code: String = ""
        private set

    protected var labelCounter = 0

    private fun appendLineToCode(hack_op: String) {
        code += hack_op + "\n"
    }

    fun initializeHACK() {
        // load initial stack Index value in SP
        appendLineToCode("@${stackIndex}")
        appendLineToCode("D = A")
        appendLineToCode("@SP")
        appendLineToCode("M = D")
    }

    // sp must be subtracted before using function, since sp points at next free slot on stack
    private fun pushDToAddrsOfSP_helper() {
        // sp points at 2nd to top item; we will push D here
        appendLineToCode("@SP")
        appendLineToCode("A=M")
        appendLineToCode("M=D")
    }

    fun pushHACK(register: REGISTER, regOffsetORConst: Int) {

        when (register) {

            REGISTER.CONSTANT -> {
                // put constant in D
                appendLineToCode("@${regOffsetORConst}")
                appendLineToCode("D = A")
                // extract current stack index
                appendLineToCode("@SP")
                appendLineToCode("A = M")
                // push constant onto stack
                appendLineToCode("M = D")
                // increment stack pointer
                appendLineToCode("@SP")
                appendLineToCode("M = M + 1")
            }

            REGISTER.LOCAL -> TODO()
            REGISTER.ARGUMENT -> TODO()
            REGISTER.THIS -> TODO()
            REGISTER.THAT -> TODO()
            REGISTER.TEMP -> TODO()
            REGISTER.STATIC -> TODO()
            REGISTER.POINTER -> TODO()
        }

        stackIndex++
    }

    fun popHACK(register: REGISTER, regOffset: Int) {
        if(stackIndex < 256)
            throw Exception("cannot pop off of stack, already at lowest index")

        val regIndex = registerMapping[register]

        // decrement SP to point at top of stack value (since SP points at next free spot)
        appendLineToCode("@SP")
        appendLineToCode("M = M - 1")
        appendLineToCode("A = M")
        //D=RAM[SP-1]
        appendLineToCode("D=M")


        when(register) {
            // Group 1 (local, argument, this, that)
            REGISTER.LOCAL, REGISTER.ARGUMENT, REGISTER.THIS, REGISTER.THAT -> {
                // D = RAM[regIndex]
                appendLineToCode("@${regIndex}")
                appendLineToCode("D = M")
                // A = regOffset
                appendLineToCode("@${regOffset}")
                // A = RAM[regIndex] + regOffset
                appendLineToCode("A = D + A")
                // D = RAM[ RAM[regIndex] + regOffset ]
                appendLineToCode("D = M")

                // save RAM[ RAM[regIndex] + regOffset ] to temp memory so that D and A are free to use

                // temp[0] =  RAM[ RAM[regIndex] + regOffset ]
                val tempReg: Int? = registerMapping[REGISTER.TEMP] ?: throw Exception("temp reg is null")
                appendLineToCode("@${tempReg}")
                appendLineToCode("M = D")

                // decrement stack before popping, since top of stack points to next available slot

                // D = RAM[0] (stackIndex)
                appendLineToCode("@SP")

                /* I think that unneeded because it's already done in lines 68-70
                appendLineToCode("D = M")
                // D = stackIndex - 1
                appendLineToCode("D = D - 1")
                // RAM[0] = stackIndex - 1
                appendLineToCode("M = D")

                // D = value on top of stack

                // A = stackIndex - 1
                appendLineToCode("A = D")
                */

                // D = RAM[stackIndex - 1]
                appendLineToCode("D = M")

                // A = RAM[ RAM[regIndex] + regOffset ]   ( from RAM[temp0] )
                appendLineToCode("@${REGISTER.TEMP}")
                appendLineToCode("A = M")

                // RAM[ RAM[regIndex] + regOffset ] = RAM[stackIndex - 1]
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
                appendLineToCode("@${className}.${regOffset}") // if X =0 and className is the first class it's like to write @16
                appendLineToCode("M=D")
            }

            // Group 4 (pointer 0, pointer 1)
            REGISTER.POINTER -> {
                if(regOffset != 0 || regOffset != 1)
                    throw Exception("Only Pointer 0 or 1")
                val offset : Int? = registerMapping[REGISTER.POINTER]?.plus(regOffset)
                appendLineToCode("@${offset}")

                //RAM[POINTER 0 OR 1] = RAM[SP-1]
                appendLineToCode("M=D")
            }
        }

        stackIndex--

    }

    fun mathOpHACK(op_type: MATH_OP) {
        when (op_type) {
            MATH_OP.ADD, MATH_OP.SUB, MATH_OP.AND, MATH_OP.OR -> binaryMathOpHACK(op_type)
            MATH_OP.NEG, MATH_OP.NOT                          -> unaryMathOpHACK(op_type)
            MATH_OP.EQ, MATH_OP.GT, MATH_OP.LT                -> logicOpHACK(op_type)
        }
    }

    private fun unaryMathOpHACK(op_type: MATH_OP) {

        appendLineToCode("@SP")
        // A = address of top of stack item
        appendLineToCode("A=M-1")
        // D = value at top of stack
        appendLineToCode("D=M")

        when(op_type) {
            MATH_OP.NEG -> appendLineToCode("D=-D")
            MATH_OP.NOT -> appendLineToCode("D=!D")
            else -> throw Exception("unaryMathOpHACK() can only process a unary math operation")
        }

        appendLineToCode("M=D")
    }

    private fun binaryMathOpHACK(op_type: MATH_OP) {

        appendLineToCode("@SP")
        // sp-- (to point at value on top of stack address)
        appendLineToCode("M=M-1")
        // A = stack address
        appendLineToCode("A=M")
        // D = value at top of stack
        appendLineToCode("D=M")
        appendLineToCode("@SP")
        // sp-- (to point at 2nd to top of stack address)
        appendLineToCode("M=M-1")
        // A = address of 2nd to top of stack
        appendLineToCode("A=M")

        // D holds top of stack value, A holds 2nd to top address

        when(op_type) {
            MATH_OP.ADD -> appendLineToCode("D=D+M")
            MATH_OP.SUB -> appendLineToCode("D=D-M")
            MATH_OP.AND -> appendLineToCode("D=D&M")
            MATH_OP.OR -> appendLineToCode("D=D|M")
            else -> throw Exception("binaryMathOpHACK() can only process a binary math operation")
        }

        // push result to top of stack
        pushDToAddrsOfSP_helper()
        // sp++ (sp should point at next free space)
        appendLineToCode("@SP")
        appendLineToCode("M=M+1")

        // stackIndex goes down by 1 after binary math op since 2 pops of operands and 1 push of result
        stackIndex--
    }

    private fun logicOpHACK(op_type: MATH_OP) {

        val lab_true = labelCounter++
        val lab_end = labelCounter++

        appendLineToCode("@SP")
        // sp-- (to point at value on top of stack address)
        appendLineToCode("M=M-1")
        // A = stack address
        appendLineToCode("A=M")
        // D = value at top of stack
        appendLineToCode("D=M")
        appendLineToCode("@SP")
        // sp-- (to point at 2nd to top of stack address)
        appendLineToCode("M=M-1")
        // A = address of 2nd to top of stack
        appendLineToCode("A=M")

        // D = ( top of stack value ) - ( 2nd to last value on stack )
        appendLineToCode("D=D-M")

        // A = true condition label; will push 1 to stack. otherwise 0 pushed to stack
        appendLineToCode("@CMP_TRUE.${lab_true}")

        when(op_type) {
            MATH_OP.EQ -> appendLineToCode("D;JMP")
            MATH_OP.GT -> appendLineToCode("D;JGT")
            MATH_OP.LT -> appendLineToCode("D;JLT")
            else       -> throw Exception("logicOpHACK() can only process a logical operation")
        }

        // if code flow arrives here, cmp operation is false; push 0 to stack
        appendLineToCode("D=0")
        pushDToAddrsOfSP_helper()
        // jmp to end of routine
        appendLineToCode("@CMP_END.${lab_end}")
        appendLineToCode("0;JMP")

        // true condition branch; push 1 to stack
        appendLineToCode("(CMP_TRUE.${lab_true})")
        appendLineToCode("D=1")
        pushDToAddrsOfSP_helper()


        // both branch flows end up here
        appendLineToCode("(CMP_END.${lab_end})")
        // increment sp to point to next free stack slot
        appendLineToCode("@SP")
        appendLineToCode("M=M+1")

        // 2 pops, 1 push
        stackIndex--
    }
}