package ru.kpfu.itis.paramonov.roomhelper.util

import java.util.Locale.getDefault

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
    getDefault()
) else it.toString() }