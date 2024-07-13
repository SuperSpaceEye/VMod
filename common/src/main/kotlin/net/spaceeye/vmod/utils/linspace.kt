package net.spaceeye.vmod.utils

fun linspace(start: Double, end: Double, num: Int): ArrayList<Double> {
    var _start = start
    var _end = end

    val linspaced = ArrayList<Double>(num)
    if (num == 0) { return linspaced }
    if (num == 1) { linspaced.add(start); return linspaced }

    val delta = (_end - _start) / (num - 1)

    for (i in 0 until num) {
        linspaced.add((start+delta*i))
    }

    return linspaced
}