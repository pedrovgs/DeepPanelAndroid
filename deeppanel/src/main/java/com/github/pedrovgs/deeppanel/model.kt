package com.github.pedrovgs.deeppanel

import android.graphics.Bitmap

typealias Prediction = Array<IntArray>

data class PredictionResult(
    val connectedAreas: Prediction,
    val panels: Panels
)

data class DetailedPredictionResult(
    val imageInput: Bitmap,
    val resizedImage: Bitmap,
    val labeledAreasBitmap: Bitmap,
    val panelsBitmap: Bitmap,
    val predictionResult: PredictionResult
)

data class Panels(val panelsInfo: List<Panel>)

data class Panel(
    val panelNumberInPage: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int = right - left
    val height: Int = bottom - top
}

// Mutable code in data classes is really handy when initializing instances
// from the JNI code.
data class RawPanelsInfo(
    var connectedAreas: Prediction = emptyArray(),
    var panels: Array<RawPanel> = emptyArray()
)

data class RawPanel(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
)
