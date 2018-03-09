/**
 * Created by (442377900) on 06-Mar-18.
 */

fun countLines(str: String): Int {
    var count: Int = 0
    for(ch in str) {
        if (ch == '\n')
            count++
    }
    return count
}

fun main(args: Array<String>) {
    countLines("1\n2")
}