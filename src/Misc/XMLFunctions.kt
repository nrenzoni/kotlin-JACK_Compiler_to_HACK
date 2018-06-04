package Misc

/**
 * Created by (442377900) on 14-May-18.
 */

fun generateXmlTag(name: String, closingTag: Boolean = false): String {
    if (!closingTag)
        return "<$name>"
    else
        return "</$name>"
}