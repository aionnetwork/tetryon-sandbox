package org.aion.tetryon; // should be org.oan.tetryon in zokrates

import avm.AltBn128;

import java.math.BigInteger;


/**
 * A library of pairing operations.
 */
public class Pairing {

    /**
     * Returns the generator of G1
     */
    public static G1Point P1() {
        return new G1Point(new Fp(1), new Fp(2));
    }

    /**
     * Returns the generator of G2
     */
    public static G2Point P2() {
        return new G2Point(
                new Fp2(new BigInteger("11559732032986387107991004021392285783925812861821192530917403151452391805634"),
                        new BigInteger("10857046999023057135944570762232829481370756359578518086990519993285655852781")),
                new Fp2(new BigInteger("4082367875863433681332203403145435568316851327593401208105741076214120093531"),
                        new BigInteger("8495653923123431417604973247489272438418190587263600148770280649306958101930"))
        );
    }

    /**
     * Bilinear pairing check.
     *
     * @param p1
     * @param p2
     * @return
     */
    public static boolean pairing(G1Point[] p1, G2Point[] p2) throws Exception {
        if (p1.length != p2.length) {
            throw new IllegalArgumentException("Points are not in pair");
        }

        byte[] g1ListData = new byte[p1.length * G1.POINT_SIZE];
        byte[] g2ListData = new byte[p1.length * G2.POINT_SIZE];

        for (int i = 0; i < p1.length; i++) {
            System.arraycopy(G1.serialize(p1[i]), 0, g1ListData, i*G1.POINT_SIZE, G1.POINT_SIZE);
            System.arraycopy(G2.serialize(p2[i]), 0, g2ListData, i*G2.POINT_SIZE, G2.POINT_SIZE);
        }

        return AltBn128.isPairingProdEqualToOne(g1ListData, g2ListData);
    }

    public static boolean pairingProd1(G1Point a1, G2Point a2) throws Exception {
        return pairing(new G1Point[]{a1}, new G2Point[]{a2});
    }

    public static boolean pairingProd2(G1Point a1, G2Point a2, G1Point b1, G2Point b2) throws Exception {
        return pairing(new G1Point[]{a1, b1}, new G2Point[]{a2, b2});
    }

    @SuppressWarnings("unused")
    public static boolean pairingProd3(G1Point a1, G2Point a2, G1Point b1, G2Point b2, G1Point c1, G2Point c2) throws Exception {
        return pairing(new G1Point[]{a1, b1, c1}, new G2Point[]{a2, b2, c2});
    }

    public static boolean pairingProd4(G1Point a1, G2Point a2, G1Point b1, G2Point b2, G1Point c1, G2Point c2, G1Point d1, G2Point d2) throws Exception {
        return pairing(new G1Point[]{a1, b1, c1, d1}, new G2Point[]{a2, b2, c2, d2});
    }
}
