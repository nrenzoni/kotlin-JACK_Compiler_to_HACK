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
        if ( cmd_line.contains(Regex("^${op}", RegexOption.IGNORE_CASE)) ) {
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

    protected val registerMapping =
            hashMapOf<REGISTER,Int>(REGISTER.LOCAL to 1, REGISTER.ARGUMENT to 2, REGISTER.THIS to 3, REGISTER.THAT to 4,
                    REGISTER.TEMP to 5, REGISTER.STATIC to 16, REGISTER.POINTER to 3)

    protected var stackIndex: Int = 256
    protected var heapIndex: Int  = 2048
    var code: String = ""
        private set

    init {
        initializeHACK()
    }

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
        appendLineToCode("A = M")
        appendLineToCode("M = D")
    }

    fun pushHACK(register: REGISTER, regOffsetORConst: Int) {

        val regIndex = registerMapping[register]

        when (register) {

            REGISTER.CONSTANT -> {
                // put constant in D
                appendLineToCode("@${regOffsetORConst}")
                appendLineToCode("D = A")
            }

            REGISTER.LOCAL, REGISTER.ARGUMENT, REGISTER.THIS, REGISTER.THAT -> {
                //D=M=RAM[RAM[register]+regoffset]
                appendLineToCode("@${regIndex}")
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
                appendLineToCode("@${className}_${regOffsetORConst}")
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

        pushDToAddrsOfSP_helper()
        // increment stack pointer
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")
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
        appendLineToCode("D = M")


        when (register) {
            // Group 1 (local, argument, this, that)
            REGISTER.LOCAL, REGISTER.ARGUMENT, REGISTER.THIS, REGISTER.THAT -> {
                // A = RAM[regIndex]
                appendLineToCode("@${regIndex}")
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
                appendLineToCode("@${className}_${regOffset}") // if X = 0 and className is the first class it's
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

    fun mathOpHACK(op_type: MATH_OP) {
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
            MATH_OP.ADD -> appendLineToCode("D = D + M")
            MATH_OP.SUB -> appendLineToCode("D = M - D")
            MATH_OP.AND -> appendLineToCode("D = D & M")
            MATH_OP.OR -> appendLineToCode("D = D | M")
            else -> throw Exception("binaryMathOpHACK() can only process a binary math operation")
        }

        // push result to top of stack
        pushDToAddrsOfSP_helper()
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
        appendLineToCode("D = 0")
        pushDToAddrsOfSP_helper()
        // jmp to end of routine
        appendLineToCode("@CMP_END_${lab_end}")
        appendLineToCode("0; JMP")

        // true condition branch; push -1 to stack
        appendLineToCode("(CMP_TRUE_${lab_true})")
        appendLineToCode("D = -1")
        pushDToAddrsOfSP_helper()


        // both branch flows end up here
        appendLineToCode("(CMP_END_${lab_end})")
        // increment sp to point to next free stack slot
        appendLineToCode("@SP")
        appendLineToCode("M = M + 1")

        // 2 pops, 1 push
        stackIndex--
    }
    private fun CreateLabel(labelName: String){
        appendLineToCode("{${className}.${labelName})")
    }
    private fun GotoF(labelName: String){
        appendLineToCode("@${className}.${labelName}")
        appendLineToCode("0; JMP")
    }
    private fun IfGotoF(labalName:String){
        //if the head of the stack not equal to 0 jump
        //D=RAM[SP-1]
        appendLineToCode("@SP")
        appendLineToCode("M=M-1")
        appendLineToCode("A=M")
        appendLineToCode("D=M")
        //jump if not equal to zero
        appendLineToCode("@${className}.${labalName}")
        appendLineToCode("D; JNE")

        //pop 1
        stackIndex--
    }
    private fun pushPointerValue(pointerName: String){
        when(pointerName){
            "LCL","ARG","THIS","THAT" -> appendLineToCode("@${pointerName}")
            else                      -> appendLineToCode("@${pointerName}.ReturnAddress")
        }
        //put the return address in D
        appendLineToCode("D=A")
        pushDToAddrsOfSP_helper()
        //SP++
        appendLineToCode("@SP")
        appendLineToCode("M=M+1")
        stackIndex++
    }
    private fun Call(funcName:String, varCount: Int){
        //push the return address
        pushPointerValue("${className}.${funcName}")
        // push the value of the pointers
        pushPointerValue("LCL")
        pushPointerValue("ARG")
        pushPointerValue("THIS")
        pushPointerValue("THAT")
        //ARG=SP-varCount-5
        appendLineToCode("@SP")
        appendLineToCode("D=A")
        appendLineToCode("@${varCount}")
        appendLineToCode("D=D-A")
        appendLineToCode("@5")
        appendLineToCode("D=D-A")
        appendLineToCode("@ARG")
        appendLineToCode("M=D")
        //LCL=SP
        appendLineToCode("@SP")
        appendLineToCode("D=M")
        appendLineToCode("@LCL")
        appendLineToCode("M=D")
        //run the function
        GotoF(funcName)
        //create the label return address. don't worry it will recognize the label even that the label write after the goto
        CreateLabel("${funcName}.ReturnAddress")

    }


}