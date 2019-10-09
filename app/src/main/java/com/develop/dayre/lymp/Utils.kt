package com.develop.dayre.lymp

//Необходима для текста-ссылок
fun getSpaceIndices(s: String, c: Char): Array<Int> {
    var pos = s.indexOf(c, 0)
    val indices = ArrayList<Int>()
    while (pos != -1) {
        indices.add(pos)
        pos = s.indexOf(c, pos + 1)
    }
    return indices.toTypedArray()
}