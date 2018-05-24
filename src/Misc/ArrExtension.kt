package Misc

/**
 * Created by (442377900) on 23-May-18.
 */

// convert ArrayList to Array for using spread operator
inline fun <reified E> ArrayList<E>.toArr(): Array<E?> {
    val arr: Array<E?> = arrayOfNulls(this.size)
    this.toArray(arr)
    return arr
}