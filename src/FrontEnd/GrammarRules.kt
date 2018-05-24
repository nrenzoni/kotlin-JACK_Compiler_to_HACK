package FrontEnd

/**
 * Created by (442377900) on 18-May-18.
 */

// basic lexical elements
const val l_identifier    = "[a-zA-Z0-9]+[_a-zA-Z0-9]*"

// grammar rules (right hand side)
const val r_varName        = l_identifier
const val r_className      = l_identifier
const val r_type           = "int|char|boolean|$r_className"