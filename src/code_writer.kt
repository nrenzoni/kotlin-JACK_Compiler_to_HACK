import java.io.File
import java.util.*
import javax.swing.text.Segment


class CodeWriter(val fileName : String){
    init{
        setFileName(fileName)

    }
   // private var mem:Array<Stack<Int>> = arrayOfNulls<Stack>(5)
    //private var mem:List<Stack<Int>> = ArrayList<Stack<Int>>()
   // private var memory: Array<Stack<Int>> = Array<Stack<Int>>(5, (Int)->Stack<Int>)
    private lateinit var outputFile: WriteFile
    private var currentLine: Int = 0
    var currentCommand: String = ""
        private set

    fun setFileName(fileName: String){
        outputFile=WriteFile(fileName)

    }
    fun writeArithmetic(command: String){
    //    when(command.toLowerCase()){
      //      "add" ->
        //    "sub" ->
        }

    }

    fun writePushPop(command: VM_Command_Type, segment: String, index: Int){
        if (command == VM_Command_Type.C_PUSH){

        }
        else if (command == VM_Command_Type.C_POP){
            return when(segment){
                "local"-> mem[0].pop()
                "argument"->mem[1].pop()
                "this"-> mem[2].pop()
                "that"-> mem[3].pop()
                "temp"-> mem[4].pop()
                "this"-> mem[2].pop()
            }

        }
        else
            throw error("Only pop and push for this function")
    }
    fun close(){

    }

}