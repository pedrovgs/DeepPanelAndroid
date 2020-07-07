package com.github.pedrovgs.deeppanel

import java.util.*

object CCL {
    fun twoPass(matrix: Array<Array<Int>>): Array<Array<Int>> {
        val firstLabelValue = 2
        val backgroundValue = 1
        var nextLabel = firstLabelValue
        val rowLength = matrix.size
        val columnLength: Int = matrix[0].count()
        val linked = mutableListOf<MutableSet<Int>>()
        val labels =
            Array(rowLength) { Array(columnLength) { 0 } }

        //First step
        for (row in 0 until rowLength) {
            for (column in 0 until columnLength) {
                if (matrix[row][column] > 1) {
                    val neibours =
                        neibours(
                            row,
                            column,
                            labels
                        )
                    if (neibours.isEmpty()) {
                        linked.add(mutableSetOf())
                        linked[nextLabel - firstLabelValue].add(nextLabel)
                        labels[row][column] = nextLabel
                        nextLabel += 1
                    } else {
                        Arrays.sort(neibours)
                        labels[row][column] = neibours[0]
                        for (i in neibours.indices) {
                            for (j in neibours.indices) {
                                linked[neibours[i] - firstLabelValue].add(neibours[j])
                            }
                        }
                    }
                }
            }
        }

        //Second pass
        val vector = Array(nextLabel) { 0 }
        for (i in 0 until nextLabel) {
            if (i < linked.count()) {
                vector[i] = linked[i].min() ?: 0
            } else {
                break
            }
        }
        for (row in 0 until rowLength) {
            for (column in 0 until columnLength) {
                if (matrix[row][column] > 1) {
                    labels[row][column] = vector[labels[row][column] - firstLabelValue]
                }
            }
        }
        return labels
    }

    fun neibours(row: Int, column: Int, matrix: Array<Array<Int>>): IntArray {
        var neibours = intArrayOf()
        val rowLength = matrix.size
        val columnLength: Int = matrix[0].count()
        if (row == 0 && column == 0) {
            return neibours
        } else if (row == 0) {
            neibours = add_element(
                matrix[row][column - 1], neibours
            )
        } else if (column == 0) {
            neibours = add_element(
                matrix[row - 1][column], neibours
            )
        } else if (row > 0 && column > 0 && column < columnLength - 1) {
            neibours = add_element(
                matrix[row][column - 1], neibours
            )
            neibours = add_element(
                matrix[row - 1][column - 1], neibours
            )
            neibours = add_element(
                matrix[row - 1][column], neibours
            )
            neibours = add_element(
                matrix[row - 1][column + 1], neibours
            )
        } else if (row > 0 && column > 0) {
            neibours = add_element(
                matrix[row][column - 1], neibours
            )
            neibours = add_element(
                matrix[row - 1][column - 1], neibours
            )
            neibours = add_element(
                matrix[row - 1][column], neibours
            )
        }
        var neibours2 = intArrayOf()
        for (i in neibours.indices) {
            if (neibours[i] > 1) {
                neibours2 = add_element(
                    neibours[i], neibours2
                )
            }
        }
        return neibours2
    }

    fun add_element(element: Int, neibours: IntArray): IntArray {
        var neibours = neibours
        neibours = Arrays.copyOf(neibours, neibours.size + 1)
        neibours[neibours.size - 1] = element
        return neibours
    }
}