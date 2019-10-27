package org.aion.tetryon; // should be org.oan.tetryon in zokrates

import java.math.BigInteger;

/**
 * Represents an element of field F_p.
 */
public class Fp {
    // an element in Fp can be encoded in 32 bytes
    public static int ELEMENT_SIZE = 32;

    public static final BigInteger FIELD_MODULUS = new BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208583");

    public final BigInteger c0;

    public Fp(BigInteger c0) {
        this.c0 = c0;
    }

    public Fp(long c0) {
        this(BigInteger.valueOf(c0));
    }

    public static Fp zero() {
        return new Fp(BigInteger.ZERO);
    }

    public boolean isZero() {
        return c0.equals(BigInteger.ZERO);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fp that = (Fp) o;
        return this.c0.equals(that.c0);
    }

    @Override
    public int hashCode() {
        return c0.hashCode();
    }

    @Override
    public String toString() {
        return Util.bytesToHex(c0.toByteArray());
    }
}
