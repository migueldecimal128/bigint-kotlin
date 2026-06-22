// SPDX-License-Identifier: MIT

package com.decimal128.math

import com.decimal128.bigint.*

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGcd {

    val verbose = false

    @Test
    fun testGcd() {
        repeat(10000) {
            test1(Random.nextInt(8))
        }
    }

    fun test1(bitLen: Int) {
        val x = BigInt.randomWithMaxBitLen(bitLen, withRandomSign = true)
        val y = BigInt.randomWithMaxBitLen(bitLen, withRandomSign = true)

        testSymmetry(x, y)
        testIdempotence(x)
        testZero(x)
        testSigns(x, y)

        val k = BigInt.randomWithMaxBitLen(Random.nextInt(29))
        val a = x * k
        val b = y * k

        val gcdAB = gcd(a, b)
        val gcdXY = gcd(x, y)
        assertEquals(gcdAB, k * gcdXY)
    }

    fun testSymmetry(x: BigInt, y: BigInt) {
        val gcdXY = gcd(x, y)
        val gcdYX = gcd(y, x)
        assertEquals(gcdXY, gcdYX)
    }

    fun testIdempotence(x: BigInt) {
        val gcdXX = gcd(x, x)
        assertEquals(gcdXX, x.abs())
    }

    fun testZero(x: BigInt) {
        val gcdXZero = gcd(x, BigInt.ZERO)
        val gcdZeroX = gcd(0.toBigInt(), x)
        assertEquals(gcdXZero, x.abs())
        assertEquals(gcdZeroX, x.abs())
    }

    fun testSigns(x: BigInt, y: BigInt) {
        val gcd0 = gcd(x, y)
        val gcd1 = gcd(x, y.negate())
        val gcd2 = gcd(x.negate(), y)
        val gcd3 = gcd(x.negate(), y.negate())

        assertEquals(gcd0, gcd1)
        assertEquals(gcd0, gcd2)
        assertEquals(gcd0, gcd3)
    }


    @Test
    fun testProblemChild() {
        val x = BigInt.from("75739105468096430")
        val y = BigInt.from("112746730774794142")
        testSymmetry(x, y)
    }

    @Test
    fun testProblemChild2() {
        val x = BigInt.from("4")
        val y = BigInt.from("1")
        testSymmetry(x, y)

    }

    @Test
    fun testProblemChild3() {
        val x = BigInt.from("-39")
        val y = BigInt.from("-39")
        val k = BigInt.from("115399892")
        val a = k * x
        val b = k * y
        val gcdAB = gcd(a, b)
        val gcdXY = gcd(x, y)
        val gcdXY_k = k * gcdXY
        if (verbose) {
            println("x:$x y:$y k:$k a:$a b:$b")
            println("gcdAB:$gcdAB gcdXY:$gcdXY gcdXY_k:$gcdXY_k")
            println("==> gcdAB:$gcdAB gcdXY_k:$gcdXY_k")
            val eq = gcdAB EQ gcdXY_k
            println("eq:$eq")
        }
        assertEquals(gcdAB, gcdXY_k)
    }

}