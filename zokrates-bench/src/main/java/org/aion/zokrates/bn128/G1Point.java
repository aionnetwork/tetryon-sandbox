package org.aion.zokrates.bn128;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Represents a point on G1.
 */
public class G1Point {
    public final Fp x;
    public final Fp y;

    // points in G1 are encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
    public static int POINT_SIZE = 2 * Fp.ELEMENT_SIZE;

    public G1Point(String x, String y) {
        this.x = new Fp(new BigInteger(x, 16));
        this.y = new Fp(new BigInteger(y, 16));
    }

    public G1Point(Fp x, Fp y) {
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
        G1Point that = (G1Point) o;
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

    public byte[] serialize() {
        byte[] data = new byte[POINT_SIZE];

        byte[] px = x.c0.toByteArray();
        System.arraycopy(px, 0, data, Fp.ELEMENT_SIZE - px.length, px.length);

        byte[] py = y.c0.toByteArray();
        System.arraycopy(py, 0, data, Fp.ELEMENT_SIZE*2 - py.length, py.length);

        return data;
    }

    public static G1Point deserialize(byte[] data) {
        byte[] pxData = Arrays.copyOfRange(data, 0, Fp.ELEMENT_SIZE);
        byte[] pyData = Arrays.copyOfRange(data, Fp.ELEMENT_SIZE, data.length);
        Fp p1x = new Fp(new BigInteger(pxData));
        Fp p1y = new Fp(new BigInteger(pyData));
        G1Point p1 = new G1Point(p1x, p1y);
        return p1;
    }
}
