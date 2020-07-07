package com.github.pedrovgs.deeppanel

data class Panels(val panelsInfo: List<Panel>)

data class Panel(
    val panelNumberInPage: Int,
    val left: Int,
    val bottom: Int,
    val width: Int,
    val height: Int
) {
    val right = left + width
    val top = bottom + height
}