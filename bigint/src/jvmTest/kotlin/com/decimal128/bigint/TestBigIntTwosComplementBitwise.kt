package com.decimal128.bigint

import com.decimal128.bigint.BigIntExtensions.toBigInteger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import kotlin.random.Random

/**
 * Validates the two's-complement bitwise ops (and/or/xor/not) against
 * java.math.BigInteger, INCLUDING negative operands — the case the previous
 * magnitude-only implementation got wrong and the existing TestMbiBooleanOps
 * never exercised (it only used non-negative values).
 */
class TestBigIntTwosComplementBitwise {

    private fun randSigned(rng: Random, maxBits: Int): BigInt {
        val bits = rng.nextInt(0, maxBits)
        val v = BigInt.randomWithMaxBitLen(bits)
        return if (rng.nextBoolean()) v.negate() else v
    }

    @Test
    fun andOrXorVsBigInteger() {
        val rng = Random(0xBADC0DEL)
        repeat(5000) {
            val a = randSigned(rng, 300)
            val b = randSigned(rng, 300)
            val ab = a.toBigInteger(); val bb = b.toBigInteger()
            assertEquals(ab.and(bb), (a and b).toBigInteger(), "AND $ab & $bb")
            assertEquals(ab.or(bb),  (a or b).toBigInteger(),  "OR $ab | $bb")
            assertEquals(ab.xor(bb), (a xor b).toBigInteger(), "XOR $ab ^ $bb")
        }
    }

    @Test
    fun notVsBigInteger() {
        val rng = Random(0xFEEDL)
        repeat(2000) {
            val a = randSigned(rng, 300)
            assertEquals(a.toBigInteger().not(), a.not().toBigInteger(), "NOT ${a.toBigInteger()}")
        }
        assertEquals(BigInteger.valueOf(-1), BigInt.ZERO.not().toBigInteger())  // ~0 == -1
    }

    @Test
    fun identities() {
        val rng = Random(0x1234L)
        repeat(1000) {
            val x = randSigned(rng, 200)
            val xb = x.toBigInteger()
            assertEquals(xb, (BigInt.NEG_ONE and x).toBigInteger())              // -1 & x == x
            assertEquals(BigInteger.valueOf(-1), (BigInt.NEG_ONE or x).toBigInteger()) // -1 | x == -1
            assertEquals(x.not().toBigInteger(), (x xor BigInt.NEG_ONE).toBigInteger()) // ~x == x ^ -1
            assertEquals(xb, x.not().not().toBigInteger())                       // ~~x == x
            assertEquals(BigInt.ZERO.toBigInteger(), (x xor x).toBigInteger())   // x ^ x == 0
        }
    }

    @Test
    fun edgeBoundaries() {
        // top-limb sign-bit boundaries that the magnitude-only impl mishandled
        val cases = listOf(
            BigInt.from("2147483648"),    // 2^31  (limb 0x80000000, non-negative)
            BigInt.from("4294967295"),    // 2^32-1
            BigInt.from("4294967296"),    // 2^32
            BigInt.from("-2147483648"),
            BigInt.from("-4294967296"),
            BigInt.ONE, BigInt.NEG_ONE, BigInt.ZERO
        )
        for (a in cases) for (b in cases) {
            val ab = a.toBigInteger(); val bb = b.toBigInteger()
            assertEquals(ab.and(bb), (a and b).toBigInteger(), "AND $ab & $bb")
            assertEquals(ab.or(bb),  (a or b).toBigInteger(),  "OR $ab | $bb")
            assertEquals(ab.xor(bb), (a xor b).toBigInteger(), "XOR $ab ^ $bb")
        }
    }

    @Test
    fun mutableMatchesBigInteger() {
        val rng = Random(0x77777L)
        val m = MutableBigInt()
        repeat(3000) {
            val a = randSigned(rng, 250)
            val b = randSigned(rng, 250)
            val ab = a.toBigInteger(); val bb = b.toBigInteger()
            assertEquals(ab.and(bb), m.setAnd(a, b).toBigInteger())
            assertEquals(ab.or(bb),  m.setOr(a, b).toBigInteger())
            assertEquals(ab.xor(bb), m.setXor(a, b).toBigInteger())
            assertEquals(ab.not(),   m.setNot(a).toBigInteger())
        }
    }

    @Test
    fun mutableAliasingAndInPlace() {
        val rng = Random(0xABCDL)
        repeat(1000) {
            val a = randSigned(rng, 200)
            val b = randSigned(rng, 200)
            val m = MutableBigInt().set(a)
            m.mutAnd(b)
            assertEquals(a.toBigInteger().and(b.toBigInteger()), m.toBigInteger())
            val m2 = MutableBigInt().set(a)
            m2.mutNot()
            assertEquals(a.toBigInteger().not(), m2.toBigInteger())
            val m3 = MutableBigInt().set(a)
            m3.setXor(m3, m3)                       // self-aliasing → 0
            assertEquals(BigInteger.ZERO, m3.toBigInteger())
        }
    }
}
