// SPDX-License-Identifier: MIT

package com.decimal128.math

import com.decimal128.bigint.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class TestFactorial {

    val verbose = false

    @Test
    fun testSmall() {
        assertEquals(BigInt.ONE, factorial(0))
        val accum = MutableBigInt().setOne()
        var f = 1uL
        for (i in 1..20) {
            f *= i.toULong()
            accum *= i
            val hi = factorial(i)
            assertTrue(f EQ hi)
            assertTrue(f EQ accum.toBigInt())
        }
        if (verbose)
            println("Tada!")
    }

    @Test
    fun testMedium() {
        var f = factorial(20)
        val accum = MutableBigInt().set(f)
        for (i in 21..100) {
            f *= i
            accum *= i
            val hi = factorial(i)
            assertEquals(f, hi)
            assertEquals(f, accum.toBigInt())
        }
    }

    @Test
    fun testLarge() {
        val bigNum = 1000// 800000

        var mul = BigInt.ONE
        val mulTime = measureTime {
            for (i in 2..bigNum)
                mul *= i
        }

        val accum = MutableBigInt().setOne()
        val accumTime = measureTime {
            for (i in 2..bigNum)
                accum *= i
        }
        var hi = BigInt.ZERO
        val factorialTime = measureTime { hi = factorial(bigNum)}
        assertEquals(accum.toBigInt(), hi)
        assertEquals(mul, hi)
        if (verbose) {
            println("commonTest: $bigNum! mulTime:$mulTime accumTime:$accumTime factorialTime:$factorialTime")
            val length = hi.toString().length
            println("digitCount:$length")
        }
    }
}
