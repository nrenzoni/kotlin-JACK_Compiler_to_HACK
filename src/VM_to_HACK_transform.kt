/**
 * Created by (442377900) on 07-Mar-18.
 */

enum class REGISTER {
    LOCAL, ARG, THIS, THAT, TEMP, STATIC, POINTER
}

enum class MATH_OP {
    ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
}


class HACKCodeGen(className: String) {

    protected lateinit var className : String  // needed for static push and pop

    init {
        this.className=className
        initializeHACK()
    }

    protected val registerMapping =
            hashMapOf<REGISTER,Int>(REGISTER.LOCAL to 1, REGISTER.ARG to 2, REGISTER.THIS to 3, REGISTER.THAT to 4,
                    REGISTER.TEMP to 5, REGISTER.STATIC to 16, REGISTER.POINTER to 3)

    protected var stackIndex: Int = 256
    protected var heapIndex: Int  = 2048
    var code: String = ""
        private set

    fun initializeHACK() {
        // load initial stack Index value in SP
        appendLineToCode("@${stackIndex}")
        appendLineToCode("D = A")
        appendLineToCode("@SP")
        appendLineToCode("M = D")
    }

    fun pushConstantHACK(number: Int) {
        // put constant in D
        appendLineToCode("@${number}")
        appendLineToCode("D = A")
        // extract current stack index
        appendLineToCode("@SP")
        appendLineToCode("A = M")
        // push constant onto stack
        appendLineToCode("M = D")
        // increment stack pointer
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")
        stackIndex++

        // Group 5 (constant)
    }

    private fun appendLineToCode(hack_op: String) {
        code += hack_op + "\n"
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
            REGISTER.LOCAL, REGISTER.ARG, REGISTER.THIS, REGISTER.THAT -> {
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
                appendLineToCode("@${className}.${popValue}") // if X =0 and className is the first class it's like to write @16
                appendLineToCode("M=D")
            }

            // Group 4 (pointer 0, pointer 1)
            REGISTER.POINTER -> {
                if(regOffset != 0 || regOffset != 1)
                    throw Exception("Only Pointer 0 or 1")
                val offset : Int? = registerMapping[REGISTER.POINTER] + regOffset
                appendLineToCode("@${offset}")

                //RAM[POINTER 0 OR 1] = RAM[SP-1]
                appendLineToCode("M=D")
            }
        }

        stackIndex--

    }

    fun mathOpHACK(op_type: MATH_OP, arg1: Int, arg2: Int) {

        when (op_type) {

            MATH_OP.ADD -> {
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
                // add two top of stack values
                appendLineToCode("D = D + M")
                // push result to top of stack
                appendLineToCode("@SP")
                appendLineToCode("M = D")
                // sp++ (sp should point at next free space)
                appendLineToCode("M = M + 1")

                // stackIndex goes down by 1 when adding since 2 pops of operands and 1 push of operator
                stackIndex--
            }

            MATH_OP.SUB -> TODO()
            MATH_OP.NEG -> TODO()
            MATH_OP.EQ -> TODO()
            MATH_OP.GT -> TODO()
            MATH_OP.LT -> TODO()
            MATH_OP.AND -> TODO()
            MATH_OP.OR -> TODO()
            MATH_OP.NOT -> TODO()
        }
    }
}