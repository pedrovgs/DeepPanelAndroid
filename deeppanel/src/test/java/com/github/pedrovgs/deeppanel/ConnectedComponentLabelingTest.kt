package com.github.pedrovgs.deeppanel

import org.junit.Assert.*
import org.junit.Test

class ConnectedComponentLabelingTest {
    @Test
    fun test() {
        val input: Array<Array<Int>> = arrayOf(
            arrayOf(0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 1, 1, 1, 0, 0),
            arrayOf(0, 1, 2, 1, 2, 1, 0),
            arrayOf(0, 1, 1, 1, 2, 0, 1),
            arrayOf(0, 1, 2, 1, 2, 1, 0),
            arrayOf(0, 1, 1, 1, 1, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0)
        )

        val result = ConnectedComponentLabeling.findAreas(input)

        printArray(result)
    }

    @Test
    fun test2() {
        val input: Array<Array<Int>> = arrayOf(
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 1),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 1, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 1, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)
        )

        val result = ConnectedComponentLabeling.findAreas(input)

        printArray(result)
        printArray(CCL.twoPass(input))
    }

    @Test
    fun test3() {
        val input: Array<Array<Int>> = arrayOf(
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0),
            arrayOf(0, 0, 1, 2, 2, 2, 1, 1, 1, 2, 1, 0, 0),
            arrayOf(0, 0, 1, 2, 2, 2, 1, 1, 1, 2, 1, 0, 0),
            arrayOf(0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0),
            arrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        )

        val result = ConnectedComponentLabeling.findAreas(input)

        printArray(result)
        printArray(CCL.twoPass(input))
    }

    private fun printArray(result: Array<Array<Int>>) {
        for (i in result.indices) {
            for (j in result.indices) {
                print(result[i][j])
            }
            println()
        }
        println()
    }

}