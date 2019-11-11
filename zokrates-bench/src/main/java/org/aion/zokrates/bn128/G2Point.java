package org.aion.zokrates.bn128;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Represents a point on the elliptic curve.
 */
public class G2Point {
    public final Fp2 x;
    public final Fp2 y;

    // points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x]. Each coordinate is 32-byte aligned.
    public static int POINT_SIZE = 4 * Fp.ELEMENT_SIZE;

    public byte[] serialize() {
        byte[] data = new byte[POINT_SIZE]; // zero byte array

        byte[] px1 = x.a.toByteArray();
        System.arraycopy(px1, 0, data, Fp.ELEMENT_SIZE*1 - px1.length, px1.length);

        byte[] px2 = x.b.toByteArray();
        System.arraycopy(px2, 0, data, Fp.ELEMENT_SIZE*2 - px2.length, px2.length);

        byte[] py1 = y.a.toByteArray();
        System.arraycopy(py1, 0, data, Fp.ELEMENT_SIZE*3 - py1.length, py1.length);

        byte[] py2 = y.b.toByteArray();
        System.arraycopy(py2, 0, data, Fp.ELEMENT_SIZE*4 - py2.length, py2.length);
        return data;
    }

    public static G2Point deserialize(byte[] data) {

        byte[] px1Data = Arrays.copyOfRange(data, 0, Fp.ELEMENT_SIZE);
        byte[] px2Data = Arrays.copyOfRange(data, 1*Fp.ELEMENT_SIZE, 2*Fp.ELEMENT_SIZE);
        byte[] py1Data = Arrays.copyOfRange(data, 2*Fp.ELEMENT_SIZE, 3*Fp.ELEMENT_SIZE);
        byte[] py2Data = Arrays.copyOfRange(data, 3*Fp.ELEMENT_SIZE, data.length);

        Fp2 x = new Fp2(new BigInteger(px1Data), new BigInteger(px2Data));
        Fp2 y = new Fp2(new BigInteger(py1Data), new BigInteger(py2Data));

        G2Point p = new G2Point(x, y);

        return p;
    }

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
