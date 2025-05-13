package net.spaceeye.vmod.utils

import java.util.Locale

fun separateTypeName(typeName: String): String {
    val separated = typeName.split(Regex("(?=[a-z][A-Z])")).toMutableList()
    separated[0] = separated[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return separated.reduce { acc, s -> acc + s[0] + " " + s.subSequence(1, s.length).toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}
}