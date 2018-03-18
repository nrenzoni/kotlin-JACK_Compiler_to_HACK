/**
 * Created by (442377900) on 07-Mar-18.
 */

enum class REGISTER {
    LOCAL, ARG, THIS, THAT, TEMP, STATIC, POINTER, CONSTANT
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

        // Group 5 (constant)
    }

    fun pushHACK(register: REGISTER, regOffset: Int){

        val regIndex = registerMapping[register]
        when(register){
        // Group 1 (local, argument, this, that)
            REGISTER.LOCAL, REGISTER.ARG, REGISTER.THIS, REGISTER.THAT -> {

                //D=M=RAM[RAM[register]+regoffset]
                appendLineToCode("@${regIndex}")
                appendLineToCode("D=M")
                appendLineToCode("@${regOffset}")
                appendLineToCode("A=D+A")
                appendLineToCode("D=M")

            }
            REGISTER.TEMP->{
                //D=RAM[TEMP+X]
                val offset = regIndex?.plus(regOffset)
                appendLineToCode("@${offset}")
                appendLineToCode("D=M")

            }
            REGISTER.STATIC->{
                //D=RAM[CLASS.X]
                appendLineToCode("@${className}.${regOffset}")
                appendLineToCode("D=M")

            }
            REGISTER.POINTER->{
                if(regOffset != 0 || regOffset != 1)
                    throw Exception("Only Pointer 0 or 1")
                //D=RAM[regIndex+regoffset]
                val offset = regIndex?.plus(regOffset)
                appendLineToCode("@${offset}")
                appendLineToCode("D=M")
            }
            REGISTER.CONSTANT->{
                //D=regOffset
                appendLineToCode("@${regOffset}")
                appendLineToCode("D=A")
            }
        }

        //PUSH D TO STACK
        appendLineToCode("@SP")
        appendLineToCode("A=M")
        appendLineToCode("M=D")
        // increment stack pointer
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")
        stackIndex++

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
                // A = RAM[regIndex]
                appendLineToCode("@${regIndex}")
                appendLineToCode("A = M")
                // A = RAM[regIndex] + regOffset
                for(i in 1..regOffset)
                    appendLineToCode("A=A+1")

                // RAM[ RAM[regIndex] + regOffset ] =RAM[SP-1]
                appendLineToCode("M=D")
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

            MATH_OP.ADD, MATH_OP.SUB -> {
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
                if (op_type==MATH_OP.ADD)
                    appendLineToCode("D = D + M")
                else
                    appendLineToCode("D = D - M")
                // push result to top of stack
                appendLineToCode("@SP")
                appendLineToCode("A=M")
                appendLineToCode("M = D")
                // sp++ (sp should point at next free space)
                appendLineToCode("@SP")
                appendLineToCode("M = M + 1")

                // stackIndex goes down by 1 when adding since 2 pops of operands and 1 push of operator
                stackIndex--
            }


            MATH_OP.NEG -> {
                appendLineToCode("@SP")
                // sp-- (to point at value on top of stack address)
                appendLineToCode("M = M - 1")
                // A = stack address
                appendLineToCode("A = M")
                // D = value at top of stack
                appendLineToCode("M = -M")
                // sp++ (sp should point at next free space)
                appendLineToCode("@SP")
                appendLineToCode("M = M + 1")

            }
            MATH_OP.EQ, MATH_OP.GT, MATH_OP.LT -> {
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
                //ARG1-ARG2
                appendLineToCode("D=D-A")
                //DEFULT M=0 (FALSE)
                appendLineToCode("M=0")
                //IF THE condition return true it will jump to LABEL (IF_TRUE)
                appendLineToCode("@IF_TRUE")

                if(op_type==MATH_OP.EQ)
                    appendLineToCode("D;JEQ")
                else if(op_type==MATH_OP.GT)
                    appendLineToCode("D;JGT")
                else //op_type==MATH_OP.LT
                    appendLineToCode("D;JLT")
                //IF THE CONDITION RETURN FALSE -> JUMP TO END LABEL
                appendLineToCode("@END")
                appendLineToCode("0;JMP")
                //LABEL IF_TRUE
                appendLineToCode("(IF_TRUE)")
                appendLineToCode("M=-1")
                //LABEL END (ALWAYS WILL ARRIVETO HERE)
                appendLineToCode("(END)")
                // stackIndex goes down by 1 when adding since 2 pops of operands and 1 push of operator
                stackIndex--

            }
            MATH_OP.AND,MATH_OP.OR -> {
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
                //arg1+arg2
                appendLineToCode("D=D+A")
                //DEFULT  M=0. IF TRUE IT CHANGE TO -1
                appendLineToCode("M=0")

                appendLineToCode("@IF_FALSE")
                appendLineToCode("D=D+1")
                if(op_type==MATH_OP.OR)
                    /*
                    FALSE AND FALSE -> D = 1
                    FALSE AND TRUE -> D = 0
                    TRUE AND TRUE -> D = -1
                     */
                    appendLineToCode("D;JGT")
                else {
                    /*
                    FALSE AND FALSE -> D = 1
                    FALSE AND TRUE -> D = 0
                    TRUE AND TRUE -> D = -1
                     */
                    appendLineToCode("D;JGE")
                }
                // IF THE CONDITION WILL RETURN FLASE IT JUMP ON THIS RAW AND RAM[SP-2] = 0 (INSERT ABOVE)
                appendLineToCode("M = -1")
                appendLineToCode("(IF_FALSE)")
                // stackIndex goes down by 1 when adding since 2 pops of operands and 1 push of operator
                stackIndex--
            }

            MATH_OP.NOT -> {
                appendLineToCode("@SP")
                // sp-- (to point at value on top of stack address)
                appendLineToCode("M = M - 1")
                // A = stack address
                appendLineToCode("A = M")
                //D=RAM[SP-1]
                appendLineToCode("D=M")
                //CHANGE TO TRUE (DEFULT)
                appendLineToCode("M=-1")
                appendLineToCode("@IF_FALSE")
                //
                appendLineToCode("D;JEQ")
                //if true cahnge to false
                appendLineToCode("M=0")

                appendLineToCode("(IF_FALSE)")
                //SP++
                appendLineToCode("@SP")
                appendLineToCode("M=M+1")

            }
        }
    }
}