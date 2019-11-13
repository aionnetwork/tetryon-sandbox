package org.aion.tetryon;

import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;

import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings({"WeakerAccess", "unused"})
public class VerifierPghr13 {

    protected static class VerifyingKey {

        public final G2Point a;
        public final G1Point b;
        public final G2Point c;
        public final G2Point gamma;
        public final G1Point gamma_beta_1;
        public final G2Point gamma_beta_2;
        public final G2Point z;
        public final G1Point[] ic;

        public VerifyingKey(G2Point a, G1Point b, G2Point c,
                            G2Point gamma, G1Point gamma_beta_1, G2Point gamma_beta_2,
                            G2Point z, G1Point[] ic) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.gamma = gamma;
            this.gamma_beta_1 = gamma_beta_1;
            this.gamma_beta_2 = gamma_beta_2;
            this.z = z;
            this.ic = ic;
        }
    }

    public static class Proof {
        public final G1Point a;
        public final G1Point a_p;
        public final G2Point b;
        public final G1Point b_p;
        public final G1Point c;
        public final G1Point c_p;
        public final G1Point k;
        public final G1Point h;

        public Proof(G1Point a, G1Point a_p,
                     G2Point b, G1Point b_p,
                     G1Point c, G1Point c_p,
                     G1Point k, G1Point h) {
            this.a = a;
            this.a_p = a_p;
            this.b = b;
            this.b_p = b_p;
            this.c = c;
            this.c_p = c_p;
            this.k = k;
            this.h = h;
        }

        // serialized as a | b | c
        public byte[] serialize() {
            byte[] s = new byte[Fp.ELEMENT_SIZE*18];

            byte[] aByte = G1.serialize(this.a);
            byte[] apByte = G1.serialize(this.a_p);
            byte[] bByte = G2.serialize(this.b);
            byte[] bpByte = G1.serialize(this.b_p);
            byte[] cByte = G1.serialize(this.c);
            byte[] cpByte = G1.serialize(this.c_p);
            byte[] kByte = G1.serialize(this.k);
            byte[] hByte = G1.serialize(this.h);

            System.arraycopy(aByte, 0, s, 0, aByte.length);
            System.arraycopy(apByte, 0, s, 2*Fp.ELEMENT_SIZE, apByte.length);
            System.arraycopy(bByte, 0, s, 4*Fp.ELEMENT_SIZE, bByte.length);
            System.arraycopy(bpByte, 0, s, 8*Fp.ELEMENT_SIZE, bpByte.length);
            System.arraycopy(cByte, 0, s, 10*Fp.ELEMENT_SIZE, cByte.length);
            System.arraycopy(cpByte, 0, s, 12*Fp.ELEMENT_SIZE, cpByte.length);
            System.arraycopy(kByte, 0, s, 14*Fp.ELEMENT_SIZE, kByte.length);
            System.arraycopy(hByte, 0, s, 16*Fp.ELEMENT_SIZE, hByte.length);

            return s;
        }

        public static Proof deserialize(byte[] data) {
            Blockchain.require(data.length == 18*Fp.ELEMENT_SIZE);

            G1Point a = G1.deserialize(Arrays.copyOfRange(data, 0, 2*Fp.ELEMENT_SIZE));
            G1Point a_p = G1.deserialize(Arrays.copyOfRange(data, 2*Fp.ELEMENT_SIZE, 4*Fp.ELEMENT_SIZE));
            G2Point b = G2.deserialize(Arrays.copyOfRange(data, 4*Fp.ELEMENT_SIZE, 8*Fp.ELEMENT_SIZE));
            G1Point b_p = G1.deserialize(Arrays.copyOfRange(data, 8*Fp.ELEMENT_SIZE, 10*Fp.ELEMENT_SIZE));
            G1Point c = G1.deserialize(Arrays.copyOfRange(data, 10*Fp.ELEMENT_SIZE, 12*Fp.ELEMENT_SIZE));
            G1Point c_p = G1.deserialize(Arrays.copyOfRange(data, 12*Fp.ELEMENT_SIZE, 14*Fp.ELEMENT_SIZE));
            G1Point k = G1.deserialize(Arrays.copyOfRange(data, 14*Fp.ELEMENT_SIZE, 16*Fp.ELEMENT_SIZE));
            G1Point h = G1.deserialize(Arrays.copyOfRange(data, 16*Fp.ELEMENT_SIZE, 18*Fp.ELEMENT_SIZE));

            return new Proof(a, a_p, b, b_p, c, c_p, k, h);
        }
    }

    protected static VerifyingKey verifyingKey() {
        G2Point a = new G2Point("1bdd41a62cdd8368d7377749f111abffeafe52dff9818f25837e72ba7ec27ee8",
                "036721ca1d05055855c51a768b6d3a26f1dbc90594096fa8185b432c43d78ac2",
                "237352ac06e69e79b2eb97542ce29915de675b1a56b656c37c5653cdf0d99089",
                "218fc788ac769eee9377c9663ea7bc2f4982a755e27483d6c7693109e02db77d");

        G1Point b = new G1Point("02e51ae9e52120c1c65f53a8163b9d1e37d26a5041a90d6a4f6f8e14ba0a7e4d",
                "15cc0cae58ae2bdc22b2e334b7a8bcf01be5532010ced453a0328f3e4820f961");

        G2Point c = new G2Point("133f9b8678cc0cb5f9199dd723c9ad72547ff709cfdda64bc8e6c14996a39913",
                "1d800d56a97d53208659927e06fc21f9040d9a2277b4a656e55f86d9f6819cf3",
                "1bb64382ce7e3f096526e3673906b59c10c9395686502dfee06be7017278cdbf",
                "15d74b4ee3eaea9674d24df8c78f54dbecbea56d5dd716a2e432b880947e3d46");

        G2Point gamma = new G2Point("093cd1b5111f849da2965e173b7ccb827fbb87fa95b0c784d9452f9c2be6bd86",
                "05c38b9d5320f433a32b344c14d17f9e17fea801c0eeb3195de8d69abbd03cdc",
                "2014bbfc222a07ed096ec226192168511d0f79637ea2ff04aae74287cc66c206",
                "111133712de5b70dcd1c6130164291386cac41a44b5fd3f33908970e72168645");

        G1Point gamma_beta_1 = new G1Point("1e5b6fb5745a60f0131e3554e29e3bd5f1eeca5aa47d4394167607206db28281",
                "2370d6f047a3e161343096d8c88b12bf3a1f51db2ef11c4112d53f434a61da68");

        G2Point gamma_beta_2 = new G2Point("0dbaf08cf21a32a6cffe982b3c0399910293d1a0c51d47e38ce51f1587aeca6d",
                "0ea3eb94f2b3e6ebac01942b6de81ee34853e55ec9e3e7621aa26910ca464a9e",
                "28f7512f5e8ed2a43c86e5a0dfb59ef4c539dabe9f745511243e621a02dbf1b4",
                "2c73720693b49fcf2155cf448c0f307cdbefbc1a4100bfdd26e28b69ffecd271");

        G2Point z = new G2Point("2cd20738309a91dd22c728c9ba7f575bf1ead6e3abd941017dc5310ee130398f",
                "2bf748631c8b6d0c134fab7e23ed6b1c02c4cd83020d723b055a4a2b81169ddf",
                "17653d29cee2e0a3cb926a5a05fc61221594c822201d5bfa5e3e20e54736dca8",
                "305c42d0756f39cfe96de066404176dac125ee743d26ae8670f4658188e749c7");

        G1Point[] ic = new G1Point[3];
        ic[0] = new G1Point("09c898126fe60f53367a90dbe1c64ff9f563be4445012fa6935020d4805c55d2", "2cd1e8437acb386f1b3a48f8e349677fdda62e9f9670ed79c6b0aaf889e60111");
        ic[1] = new G1Point("0dd498debb3a81958fa9bf7eb957d620c9db51d4a61fefb064ad135fa9054002", "1d48512b7d890bbfa524e8b02893b3da61e3ad3f967eb92181f03df13dd030ac");
        ic[2] = new G1Point("2982223032cdbecd972eb55c00183e0365799a12417d2a95044bd7fa2d99f2ba", "06757605e0cc68e247b2e795c676edc60369da542e21859757d5503de9cf1766");

        return new VerifyingKey(a, b, c, gamma, gamma_beta_1, gamma_beta_2, z, ic);
    }

    static final BigInteger snarkScalarField = new BigInteger("21888242871839275222246405745257275088548364400416034343698204186575808495617");

    public static boolean verify(BigInteger[] input, Proof proof) throws Exception {
        VerifyingKey vk = verifyingKey();
        Blockchain.require(input.length + 1 == vk.ic.length);
        G1Point X = new G1Point(Fp.zero(), Fp.zero());
        for (int i = 0; i < input.length; i++) {
            Blockchain.require(input[i].compareTo(snarkScalarField) < 0);
            G1Point tmp = G1.mul(vk.ic[i + 1], input[i]);
            if (i == 0)
                X = tmp;
            else
                X = G1.add(X, tmp);
        }
        X = G1.add(X, vk.ic[0]);

        if (!Pairing.pairingProd2(proof.a, vk.a, G1.negate(proof.a_p), G2.G2_P)) {
            return false;
        }

        if (!Pairing.pairingProd2(vk.b, proof.b, G1.negate(proof.b_p), G2.G2_P)) {
            return false;
        }

        if (!Pairing.pairingProd2(proof.c, vk.c, G1.negate(proof.c_p), G2.G2_P)) {
            return false;
        }

        if (!Pairing.pairingProd3(proof.k, vk.gamma,
                G1.negate(G1.add(X, G1.add(proof.a, proof.c))), vk.gamma_beta_2,
                G1.negate(vk.gamma_beta_1), proof.b)) {
            return false;
        }

        return Pairing.pairingProd3(G1.add(X, proof.a), proof.b,
                G1.negate(proof.h), vk.z,
                G1.negate(proof.c), G2.G2_P);
    }

    @Callable
    public static boolean verify(BigInteger[] input, byte[] proof) {
        Blockchain.println("verify() called");

        try {
            if (verify(input, Proof.deserialize(proof))) {
                Blockchain.log("VerifySnark".getBytes(), BigInteger.ONE.toByteArray());
                return true;
            }
        } catch (Exception e) {
            Blockchain.println("verify() failed with exception: " + e.getMessage());
        }

        Blockchain.log("VerifySnark".getBytes(), BigInteger.ZERO.toByteArray());
        return false;
    }
}
