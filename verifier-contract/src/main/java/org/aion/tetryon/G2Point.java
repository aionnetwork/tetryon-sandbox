package org.aion.tetryon; // should be org.oan.tetryon in zokrates

import java.math.BigInteger;

/**
 * Represents a point on the elliptic curve.
 */
public class G2Point {
    public final Fp2 x;
    public final Fp2 y;

    public G2Point(String x_a, String x_b, String y_a, String y_b) {
        this.x = new Fp2(new BigInteger(x_a, 16), new BigInteger(x_b, 16));
        this.y = new Fp2(new BigInteger(y_a, 16), new BigInteger(y_b, 16));
    }

    public G2Point(Fp2 x, Fp2 y) {
        this.x = x;
        this.y = y;
    }

    public boolean isZero() {
        return x.isZero() && y.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        G2Point that = (G2Point) o;
        return this.x.equals(that.x) && this.y.equals(that.y);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.x.hashCode();
        result = 31 * result + this.y.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
