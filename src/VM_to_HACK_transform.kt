/**
 * Created by (442377900) on 07-Mar-18.
 */

enum class REGISTER {
    LOCAL, ARG, THIS, THAT, TEMP, STATIC, POINTER0, POINTER1
}

class HACKCodeGen() {
    init {
        initializeHACK()
    }

    protected val registerMapping =
            hashMapOf<REGISTER,Int>(REGISTER.LOCAL to 1, REGISTER.ARG to 2, REGISTER.THIS to 3, REGISTER.THAT to 4,
                    REGISTER.TEMP to 5, REGISTER.STATIC to 16, REGISTER.POINTER0 to 3, REGISTER.POINTER1 to 4)

    protected var stackIndex: Int = 256
    protected var heapIndex: Int  = 2048
    var code: String = ""
        private set

    fun initializeHACK() {
        appendLineToCode("@256")
        appendLineToCode("D = A")
        appendLineToCode("@0")
        appendLineToCode("M = D")
    }

    fun pushConstantHACK(number: Int) {
        // put constant in D
        appendLineToCode("@${number}")
        appendLineToCode("D = A")
        // extract current stack index from RAM[0]
        appendLineToCode("@0")
        appendLineToCode("A = M")
        // push constant onto stack
        appendLineToCode("M = D")
        stackIndex++

        // Group 5 (constant)
    }

    private fun appendLineToCode(addition: String) {
        code += addition + "\n"
    }

    fun popHACK(register: REGISTER, popValue: Int) {
        if(stackIndex < 256)
            throw Exception("cannot pop off of stack, already at lowest index")

        val regIndex = registerMapping[register]

        when(register) {
            // Group 1 (local, argument, this, that)
            REGISTER.LOCAL, REGISTER.ARG, REGISTER.THIS, REGISTER.THAT -> {
                // D = RAM[regIndex]
                appendLineToCode("@${regIndex}")
                appendLineToCode("D = M")
                // A = popValue
                appendLineToCode("@${popValue}")
                // A = RAM[regIndex] + popValue
                appendLineToCode("A = D + A")
                // D = RAM[ RAM[regIndex] + popValue ]
                appendLineToCode("D = M")

                // save RAM[ RAM[regIndex] + popValue ] to temp memory so that D and A are free to use

                // temp[0] =  RAM[ RAM[regIndex] + popValue ]
                appendLineToCode("@${REGISTER.TEMP}")
                appendLineToCode("M = D")

                // decrement stack before popping, since top of stack points to next available slot

                // D = RAM[0] (stackIndex)
                appendLineToCode("@0")
                appendLineToCode("D = M")
                // D = stackIndex - 1
                appendLineToCode("D = D - 1")
                // RAM[0] = stackIndex - 1
                appendLineToCode("M = D")

                // D = value on top of stack

                // A = stackIndex - 1
                appendLineToCode("A = D")
                // D = RAM[stackIndex - 1]
                appendLineToCode("D = M")

                // A = RAM[ RAM[regIndex] + popValue ]   ( from RAM[temp0] )
                appendLineToCode("@${REGISTER.TEMP}")
                appendLineToCode("A = M")

                // RAM[ RAM[regIndex] + popValue ] = RAM[stackIndex - 1]
                appendLineToCode("M = D")
            }

            // Group 2 (temp)
            REGISTER.TEMP -> {
                //
                appendLineToCode("@${REGISTER.TEMP}")
                appendLineToCode("D = A")
                appendLineToCode("@${popValue}")
                appendLineToCode("A = D + A")
            }

            // Group 3 (static)
            REGISTER.STATIC -> {
                appendLineToCode("")
            }

            // Group 4 (pointer 0, pointer 1)
            REGISTER.POINTER0, REGISTER.POINTER1 -> {
                appendLineToCode("")
            }

            else ->
                    throw Exception("pop instruction in VM not matched")
        }

        stackIndex--

    }
}