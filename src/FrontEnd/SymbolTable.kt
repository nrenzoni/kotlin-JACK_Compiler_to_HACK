package FrontEnd

/**
 * Created by (442377900) on 31-May-18.
 */

class Symbol(val name: String, val type: String, val kind: ID_KIND, val index: Int)

class SymbolTable {
    private var staticCounter = 0
    private var fieldCounter = 0
    private var argCounter = 0
    private var varCounter = 0

    private val symbolTableItems = hashMapOf<String, Symbol>()

    fun clear() {
        symbolTableItems.clear()
        staticCounter = 0
        fieldCounter = 0
        argCounter = 0
        varCounter = 0

    }

    fun addIdentifier(name: String, type: String, kind: ID_KIND) {
        val indexVal =
                when (kind) {
                    ID_KIND.STATIC -> staticCounter++
                    ID_KIND.FIELD  -> fieldCounter++
                    ID_KIND.ARG    -> argCounter++
                    ID_KIND.VAR    -> varCounter++
                }
        symbolTableItems[name] = Symbol(name, type, kind, indexVal)
    }

    fun varCount(kind: ID_KIND? = null): Int {
        if (kind == null)
            return symbolTableItems.size
        var counter = 0
        for ((key, sym) in symbolTableItems) {
            if (sym.kind == kind)
                counter++
        }
        return counter
    }

    fun getSymbol(name: String): Symbol? {
        return if (symbolTableItems.contains(name))
            symbolTableItems[name]
        else
            null
    }

    fun indexOf(name: String): Int? = getSymbol(name)?.index ?: throw Exception("$name not in table")
}