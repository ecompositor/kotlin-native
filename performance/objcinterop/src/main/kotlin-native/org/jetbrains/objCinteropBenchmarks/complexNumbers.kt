/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.complexNumbers
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import platform.Foundation.*
import platform.darwin.*

actual class ComplexNumbersBenchmark actual constructor() {
    val complexNumbersSequence = generateNumbersSequence()

    fun randomNumber() = Random.nextDouble(0.0, benchmarkSize.toDouble())

    actual fun generateNumbersSequence(): List<Any> {
        val result = mutableListOf<Complex>()
        for (i in 1..benchmarkSize) {
            result.add(Complex(randomNumber(), randomNumber()))
        }
        return result
    }

    actual fun sumComplex() {
        complexNumbersSequence.map { (it as Complex).add(it) }.reduce { acc, it -> (acc as Complex).add(it) }
    }

    actual fun subComplex() {
        complexNumbersSequence.map { (it as Complex).sub(it) }.reduce { acc, it -> (acc as Complex).sub(it) }
    }

    actual fun classInheritance() {
         class InvertedNumber(val value: Double) : CustomNumberProtocol, NSObject() {
            override fun add(other: CustomNumberProtocol?) : CustomNumberProtocol? =
                    other?.let {
                        if (it is InvertedNumber)
                            InvertedNumber(-value + sqrt(it.value))
                        else
                            error("Expected object of InvertedNumber class")
                    }

            override fun sub(other: CustomNumberProtocol?) : CustomNumberProtocol? =
                    other?.let {
                        if (it is InvertedNumber)
                            InvertedNumber(-value - sqrt(it.value))
                        else
                            error("Expected object of InvertedNumber class")
                    }
        }

        val result = InvertedNumber(0.0)

        for (i in 1..benchmarkSize) {
            result.add(InvertedNumber(randomNumber()))
            result.sub(InvertedNumber(randomNumber()))
        }
    }

    actual fun categoryMethods() {
        complexNumbersSequence.map { (it as Complex).mul(it) }.reduce { acc, it -> acc?.mul(it) }
        complexNumbersSequence.map { (it as Complex).div(it) }.reduce { acc, it -> acc?.mul(it) }
    }

    actual fun stringToObjC() {
        complexNumbersSequence.forEach {
            (it as Complex).setFormat("%.1lf|%.1lf")
        }
    }

    actual fun stringFromObjC() {
        complexNumbersSequence.forEach {
            (it as Complex).description()?.split(" ")
        }
    }

    private fun revert(number: Int, lg: Int): Int {
        var result = 0
        for (i in 0 until lg) {
            if (number and (1 shl i) != 0) {
                result = result or 1 shl (lg - i - 1)
            }
        }
        return result
    }

    inline private fun fftRoutine(invert:Boolean = false): Array<Any> {
        var lg = 0
        while ((1 shl lg) < complexNumbersSequence.size) {
            lg++
        }
        val sequence = complexNumbersSequence.toTypedArray()

        sequence.forEachIndexed { index, number ->
            if (index < revert(index, lg)) {
                sequence[index] = sequence[revert(index, lg)].also { sequence[revert(index, lg)] = sequence[index] }
            }
        }

        var length = 2
        while (length <= complexNumbersSequence.size) {
            val angle = 2 * PI / length * if (invert) -1 else 1
            val base = Complex(cos(angle), sin(angle))
            for (i in 0 until complexNumbersSequence.size step length) {
                var value = Complex(1.0, 1.0)
                for (j in 0 until length/2) {
                    val first = sequence[i + j] as Complex
                    val second = (sequence[i + j + length/2] as Complex).mul(value)
                    sequence[i + j] = first.add(second)!!
                    sequence[i + j + length/2] = first.sub(second)!!
                    value = value.mul(base)!!
                }
            }
        }
        return sequence
    }

    actual fun fft() {
        fftRoutine()
    }

    actual fun invertFft() {
        val sequence = fftRoutine(true)

        sequence.forEachIndexed { index, _ ->
            sequence[index] = (sequence[index] as Complex).div(Complex(sequence.size.toDouble(), 0.0))!!
        }
    }
}