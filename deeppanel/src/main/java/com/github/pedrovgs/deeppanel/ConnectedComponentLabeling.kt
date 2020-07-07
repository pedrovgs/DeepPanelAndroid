package com.github.pedrovgs.deeppanel

import android.util.Log

object ConnectedComponentLabeling {

    private const val firstLabelValue = 2
    private const val backgroundLabel = 1

    fun findAreas(inputMatrix: Array<Array<Int>>): Array<Array<Int>> {
        var currentLabel = firstLabelValue
        val imageSizeRange = inputMatrix.indices
        val parents = mutableMapOf<Int, MutableSet<Int>>()
        val imageSize = imageSizeRange.count()
        val labels = Array(imageSize) { i ->
            Array(imageSize) { j ->
                if (inputMatrix[i][j] == 1) 1 else 0
            }
        }
        // First pass
        for (row in imageSizeRange) {
            for (column in imageSizeRange) {
                val isNotBackgroundOrContent = inputMatrix[row][column] > backgroundLabel
                if (isNotBackgroundOrContent) {
                    val neighbors = findNeighbors(labels, row, column)
                    if (neighbors.isEmpty()) {
                        parents[currentLabel] = mutableSetOf(currentLabel)
                        labels[row][column] = currentLabel
                        Log.d("CCL", "Assigning $currentLabel for position [$row, $column]")
                        currentLabel += 1
                        Log.d(
                            "CCL",
                            "Increment current label with value $currentLabel for position [$row, $column]"
                        )
                    } else {
                        val minNeighborsValue = neighbors.min()!!
                        labels[row][column] = minNeighborsValue
                        Log.d("CCL", "Assigning $minNeighborsValue for position [$row, $column]")
                        if (neighbors.size > 1) {
                            for (neighbor in neighbors) {
                                (parents[neighbor] ?: mutableSetOf()).add(minNeighborsValue)
                                Log.d("CCL", "Updating neighbors parents with $minNeighborsValue")
                                Log.d("CCL",
                                    "The parent list for label $neighbor is now ${parents[neighbor]!!.toIntArray()
                                        .map { it.toString() }.reduce { acc, s -> "$acc,$s" }}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Second pass
        for (row in imageSizeRange) {
            for (column in imageSizeRange) {
                val isNotBackgroundOrContent = inputMatrix[row][column] > backgroundLabel
                if (isNotBackgroundOrContent) {
                    val newAggregatedValue =
                        parents[labels[row][column]]?.min() ?: backgroundLabel
                    labels[row][column] = newAggregatedValue
                }
            }
        }
        val flattenedLabels = labels.flatten()
        val distinctLabels = flattenedLabels.distinct()
        val differentLabelsNumber = distinctLabels.count()
        Log.d(
            "CCL",
            "CCL FINISHED. Found $differentLabelsNumber different panels"
        )
        for (label in distinctLabels) {
            Log.d("CCL", "Number of pixels with label $label == ${flattenedLabels.count { it == label }}")
        }
        return labels
    }

    private fun printArray(result: Array<Array<Int>>) {
        for (i in result.indices) {
            for (j in result.indices) {
                print("${result[i][j]},")
            }
            println()
        }
        println()
    }

    private fun findNeighbors(labeledPrediction: Array<Array<Int>>, i: Int, j: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        val maxValue = labeledPrediction.count()
        if (i > 0) {
            neighbors.add(labeledPrediction[i - 1][j])
            if (j > 0)
                neighbors.add(labeledPrediction[i - 1][j - 1])
            if (j < maxValue)
                neighbors.add(labeledPrediction[i - 1][j + 1])
        }
        if (j > 0) {
            neighbors.add(labeledPrediction[i][j - 1])
        }
        if (i < maxValue) {
            neighbors.add(labeledPrediction[i + 1][j])
            if (j > 0)
                neighbors.add(labeledPrediction[i + 1][j - 1])
            if (j < maxValue)
                neighbors.add(labeledPrediction[i + 1][j + 1])
        }
        if (j < maxValue) {
            neighbors.add(labeledPrediction[i][j + 1])
        }
        return neighbors.filter { it > backgroundLabel }
    }
}
