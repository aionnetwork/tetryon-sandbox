package org.aion.tetryon;

import avm.AltBn128;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * A collection of Elliptic Curve operations on G1 for alt_bn128. This implementation is
 * heavily based on the EC API exposed by the AVM.
 *
 * <p>
 * Curve definition: y^2 = x^3 + b
 * <p>
 */
public class G1 {

    // points in G1 are encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
    public static int POINT_SIZE = 2 * Fp.ELEMENT_SIZE;

    public static byte[] serialize(G1Point p) {
        byte[] data = new byte[POINT_SIZE];

        byte[] px = p.x.c0.toByteArray();
        System.arraycopy(px, 0, data, Fp.ELEMENT_SIZE - px.length, px.length);

        byte[] py = p.y.c0.toByteArray();
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


    // The prime q in the base field F_q for G1
    private static final BigInteger q = new BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208583");

    public static G1Point negate(G1Point p) {
        if (p.isZero()) {
            return new G1Point(Fp.zero(), Fp.zero());
        }
        return new G1Point(p.x, new Fp(q.subtract(p.y.c0.mod(q))));
    }

    public static G1Point add(G1Point p1, G1Point p2) throws Exception {
        byte[] p1data = serialize(p1);
        byte[] p2data = serialize(p2);
        byte[] resultData = AltBn128.g1EcAdd(p1data, p2data);
        G1Point result = deserialize(resultData);
        return result;
    }

    public static G1Point mul(G1Point p, BigInteger s) throws Exception {
        byte[] pdata = serialize(p);

        byte[] resultData = AltBn128.g1EcMul(pdata, s);
        G1Point result = deserialize(resultData);
        return result;
    }
}
