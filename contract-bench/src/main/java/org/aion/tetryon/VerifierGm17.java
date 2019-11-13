package org.aion.tetryon;

import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;

import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings({"WeakerAccess", "unused"})
public class VerifierGm17 {

    protected static class VerifyingKey {

        public final G2Point h;
        public final G1Point g_alpha;
        public final G2Point h_beta;
        public final G1Point g_gamma;
        public final G2Point h_gamma;
        public final G1Point[] query;

        public VerifyingKey(G2Point h, G1Point g_alpha, G2Point h_beta, G1Point g_gamma, G2Point h_gamma, G1Point[] query) {
            this.h = h;
            this.g_alpha = g_alpha;
            this.h_beta = h_beta;
            this.g_gamma = g_gamma;
            this.h_gamma = h_gamma;
            this.query = query;
        }
    }

    public static class Proof {
        public final G1Point a;
        public final G2Point b;
        public final G1Point c;

        public Proof(G1Point a, G2Point b, G1Point c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        // serialized as a | b | c
        public byte[] serialize() {
            byte[] s = new byte[Fp.ELEMENT_SIZE*8];

            byte[] a = G1.serialize(this.a);
            byte[] b = G2.serialize(this.b);
            byte[] c = G1.serialize(this.c);

            System.arraycopy(a, 0, s, 0, a.length);
            System.arraycopy(b, 0, s, 6*Fp.ELEMENT_SIZE - b.length, b.length);
            System.arraycopy(c, 0, s, 8*Fp.ELEMENT_SIZE - c.length, c.length);

            return s;
        }

        public static Proof deserialize(byte[] data) {
            Blockchain.require(data.length == 8*Fp.ELEMENT_SIZE);

            G1Point a = G1.deserialize(Arrays.copyOfRange(data, 0, 2*Fp.ELEMENT_SIZE));
            G2Point b = G2.deserialize(Arrays.copyOfRange(data, 2*Fp.ELEMENT_SIZE, 6*Fp.ELEMENT_SIZE));
            G1Point c = G1.deserialize(Arrays.copyOfRange(data, 6*Fp.ELEMENT_SIZE, 8*Fp.ELEMENT_SIZE));

            return new Proof(a, b, c);
        }
    }

    protected static VerifyingKey verifyingKey() {
        G2Point h = new G2Point("1c4d9327ced199d84fe23af0cd2098163ae12ee719fa12cba67a23d4fd3d7d2b",
                "2c57726f5ef22c875588742041f8ab372ca7b07cd83fab4a660fce33c48951d8",
                "1f5009a451cb92c26cd91b9a054cc0638ad154c544edbe9f40512868e824ce4f",
                "277f92b1ef2ceace1351cbd7a2456633379d614c53638d048c0093aebc3f3edf");

        G1Point g_alpha = new G1Point("2d9cba74f882b848bd9c4e79cbfc8c85f4210b7019cc277763b9ca570f2f6065",
                "2a45529741c396be7ad74259aec507b38ea3b0c93e1baf123eae72c1b0437f84");

        G2Point h_beta = new G2Point("2f95640c133957596274a795a57a2dbd826ba652e4109038e4d5d7bddf441ccf",
                "11d76b37341d672549c1bebc202d8f45e022b83ec285d7bf29a1b22f39771cb8",
                "1dbc4c564340b4cc8fe765d58527f860b0acbc1bfb9a5aeb4ed0bc732cbd44b8",
                "0667bd4309ada274784194b9c3148c4c20c9a70a3aa8efcb675899c0d2985390");

        G1Point g_gamma = new G1Point("049ef26ee8260096726031598359a5a263aa22b041824f6aa20a19776b07c566",
                "077e441a822f7e267f92566ee78ad0c2629ebb335174c44741d89d8449ef6d63");

        G2Point h_gamma = new G2Point("05afff0728c0b3b54b8da83d9b27f4b49318ba7e944f8647ecc82e0192e67ba3",
                "1ab226d81bd1a8a5090c4b86b92aee1d4f1914812b129d52370ae099e8cad744",
                "25aacc75fa00214aca96904b55c5c3b265314b03fa200e15debea20bf056992a",
                "10a67d0361cb089d42d4f1accb5eab94a12df0bb3bf667b03dd9fa0ce3c888d2");

        G1Point[] query = new G1Point[]{
                new G1Point("1cda32f0e268d82f9a01c48783ca82eb43d1b92eed20eec331815351dc66fd77",
                        "02fcbbe249ca654010a7c399d3576b393fb7c95234b464ddf53fa808bc30c7d5"),
                new G1Point("1e21815b62d65931b9ed0030473cab6412a0b2669c8a057887f3f59037059f0e",
                        "09bbba83ecc31715abbdefc63364d877ac233e02ecadc22f18492ddc97c02bba"),
                new G1Point("0230e4604e0cd203ee62ecf72b11ade8555e807f12521e4305186ad72f11e643",
                        "1e43685f35ea07980e950f8ca0151a5336a3a35cc7fea721575d5edb10394998")
        };

        return new VerifyingKey(h, g_alpha, h_beta, g_gamma, h_gamma, query);
    }

    static final BigInteger snarkScalarField = new BigInteger("21888242871839275222246405745257275088548364400416034343698204186575808495617");

    public static boolean verify(BigInteger[] input, Proof proof) throws Exception {
        VerifyingKey vk = verifyingKey();
        Blockchain.require(input.length + 1 == vk.query.length);
        G1Point X = new G1Point(Fp.zero(), Fp.zero());
        for (int i = 0; i < input.length; i++) {
            Blockchain.require(input[i].compareTo(snarkScalarField) < 0);
            G1Point tmp = G1.mul(vk.query[i + 1], input[i]);
            if (i == 0)
                X = tmp;
            else
                X = G1.add(X, tmp);
        }
        X = G1.add(X, vk.query[0]);

        if(!Pairing.pairingProd4(vk.g_alpha, vk.h_beta, X, vk.h_gamma, proof.c, vk.h, G1.negate(G1.add(proof.a, vk.g_alpha)), G2.ECTwistAdd(proof.b, vk.h_beta))) {
            return false;
        }

        return Pairing.pairingProd2(proof.a, vk.h_gamma, G1.negate(vk.g_gamma), proof.b);
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
