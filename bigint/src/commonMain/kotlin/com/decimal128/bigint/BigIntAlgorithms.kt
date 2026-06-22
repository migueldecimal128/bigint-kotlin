// SPDX-License-Identifier: MIT

@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.bigint

import com.decimal128.bigint.BigInt.Companion.NEG_ONE
import com.decimal128.bigint.BigInt.Companion.ONE
import com.decimal128.bigint.BigInt.Companion.ZERO

object BigIntAlgorithms {

    /**
     * Raises [base] to the integer power [exp] using binary exponentiation.
     *
     * Special cases are handled efficiently:
     *  - `exp == 0` → returns [ONE]
     *  - `exp == 1` → returns [base]
     *  - `base == ZERO` → returns [ZERO]
     *  - `base == ±1` → returns ±1 depending on the parity of [exp]
     *  - `base == ±2` → uses bit-setting for fast 2ⁿ
     *  - `exp == 2` → uses `sqr()` for efficiency
     *
     * Uses a non-modular square-and-multiply loop with preallocated buffers sized
     * to the maximum possible bit length of the result.
     *
     * @param base the BigInt base value
     * @param exp  the non-negative exponent
     * @return `base^exp` with the mathematically correct sign
     * @throws IllegalArgumentException if [exp] is negative
     */
    fun pow(base: BigInt, exp: Int): BigInt {
        val resultSign = base.isNegative() && ((exp and 1) != 0)
        val baseAbs = base.abs()
        return when {
            exp < 0 -> throw IllegalArgumentException("cannot raise BigInt to negative power:$exp")
            exp == 0 -> ONE
            exp == 1 -> base
            exp == 2 -> baseAbs.sqr()
            base.isZero() -> ZERO
            baseAbs EQ 1 -> if (resultSign) NEG_ONE else ONE
            baseAbs EQ 2 -> BigInt.withSetBit(exp).withSign(resultSign)
            else -> {
                val maxBitLen = base.magnitudeBitLen() * exp
                val r = MutableBigInt.withBitCapacityHint(maxBitLen).setOne()
                powLeftToRight(base, exp, r)
                r.toBigInt()
            }
        }
    }

    fun tryPowFastPath(base: BigIntNumber, exp: Int, mbi: MutableBigInt): Boolean {
        val resultSign = base.isNegative() && ((exp and 1) != 0)
        when {
            exp < 0 -> throw IllegalArgumentException("cannot raise BigInt to negative power:$exp")
            exp == 0 -> mbi.setOne()
            exp == 1 -> mbi.set(base)
            exp == 2 -> mbi.setSqr(base)
            base.isZero() -> mbi.setZero()
            base magEQ 1 -> mbi.set(if (resultSign) -1 else 1)
            base magEQ 2 -> mbi.setBit(exp).mutWithSign(resultSign)
            else -> return false
        }
        return true
    }

    /**
     * Computes [base]^[exp] using left-to-right binary exponentiation.
     *
     * Scans the exponent from MSB to LSB, building the result through repeated
     * squaring and conditional multiplication. Sign handling is implicit: negative
     * bases with odd exponents naturally produce negative results through the
     * multiplication operations.
     *
     * This algorithm uses one working buffer instead of two (as required by
     * right-to-left), reducing memory allocation and pressure. The original base
     * value remains small and cache-resident throughout the computation.
     *
     * @param base the base value (may be negative)
     * @param exp the non-negative exponent (must be > 2)
     * @param ret destination for the result; must not alias [base]
     * @throws IllegalStateException if exp ≤ 2 (caller should use [tryPowFastPath])
     */
    fun powLeftToRight(base: BigIntNumber, exp: Int, ret: MutableBigInt) {
        if (exp <= 2)
            throw IllegalStateException() // should have been fast-path
        val maxBitLen = base.magnitudeBitLen() * exp
        ret.hintBitCapacity(maxBitLen).set(base)
        var bitIndex = 31 - exp.countLeadingZeroBits() - 1
        while (bitIndex >= 0) {
            ret.setSqr(ret)
            if ((exp shr bitIndex) and 1 != 0)
                ret *= base
            --bitIndex
        }
    }

    /**
     * Montgomery REDC (CIOS form) using 32-bit limbs.
     *
     * Computes  t = t * R⁻¹ mod N,  where R = 2^(32*nLen).
     *
     * Input layout:
     *  - t[0 .. tLen-1] contains the low limbs of T, with 0 ≤ tLen ≤ 2*nLen+1
     *  - n[0 .. nLen-1] contains modulus N (must be odd)
     *  - np = −N⁻¹ mod 2³² (32-bit Montgomery factor)
     *
     * Buffer requirements:
     *  - t must provide capacity for **at least 2*nLen + 1 limbs**
     *  - any limbs in t[tLen .. 2*nLen] must not contain live data
     *    (this implementation clears them before use)
     *
     * Mathematical preconditions:
     *  - N is odd
     *  - 0 ≤ T < N * R   so that REDC produces a value < 2N
     *
     * Postconditions:
     *  - t[0 .. k] holds a k-limb candidate, where k ≤ nLen+1
     *  - if that value ≥ N, one subtraction is applied
     *  - the final result satisfies 0 ≤ t < N and occupies ≤ nLen limbs
     *
     * Returns:
     *  - the normalized limb length of the reduced residue
     *
     * Notes:
     *  - This is the classic Coarsely Integrated Operand Scanning (CIOS) form.
     *  - One extra limb of temporary space is used to hold carry propagation
     *    during the elimination phase.
     */
    fun montgomeryRedc(t: Magia, tLen: Int, n: Magia, k: Int, np: UInt): Int {
        require(k > 0 && k <= n.size)
        require(tLen <= t.size)
        require(t.size >= 2*k + 1)
        require((n[0] and 1) != 0)

        // clear garbage limbs up to t[2*k + 1]
        t.fill(0, tLen, 2*k + 1)

        // --- Phase 1: eliminate low limbs one at a time ---
        for (i in 0..<k) {
            // m = (t[i] * np) mod 2^32
            val m = (t[i].toUInt() * np).toULong()

            var carry = 0uL
            var j = 0

            // t[i+j] += m * n[j] + carry
            while (j < k) {
                // 64-bit accumulator
                val acc =
                    t[i + j].toUInt().toULong() +
                            m * n[j].toUInt().toULong() +
                            carry

                t[i + j] = acc.toInt()
                carry   = acc shr 32
                j++
            }
            carry += t[i + k].toUInt().toULong()
            t[i + k] = carry.toInt()
            // propagate the remaining carry through the high limbs (a single-limb
            // push loses a carry when t[i+k+1] is already 0xFFFFFFFF — observable
            // for sparse moduli just above a power of two)
            var c = carry shr 32
            var idx = i + k + 1
            while (c != 0uL) {
                val acc = t[idx].toUInt().toULong() + c
                t[idx] = acc.toInt()
                c = acc shr 32
                idx++
            }
        }

        // --- Phase 2: shift down the upper half ---
        // result initially resides in t[k .. 2k-1]
        t.copyInto(t, 0, k, k + k + 1)

        // --- Phase 3: conditional subtract modulus ---
        // if T >= N, subtract once

        val tNormLen = magia_normLen(t, k + 1)
        if (magia_compare(t, tNormLen, n, k) < 0)
            return tNormLen
        return magia_setSub(t, t, tNormLen, n, k)
    }


}

