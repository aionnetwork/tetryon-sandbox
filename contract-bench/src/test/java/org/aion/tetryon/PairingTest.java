package org.aion.tetryon;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.tetryon.*;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigInteger;

import static org.aion.tetryon.Util.*;
import static org.junit.Assert.assertTrue;

public class PairingTest {

    @SuppressWarnings("unused")
    public static class PairingTestWrapper {

        @Callable
        public static boolean pairingProd1(byte[] a1, byte[] a2) throws Exception {
            return Pairing.pairingProd1(G1.deserialize(a1), G2.deserialize(a2));
        }

        @Callable
        public static boolean pairingProd2(byte[] a1, byte[] a2, byte[] b1, byte[] b2) throws Exception {
            return Pairing.pairingProd2(G1.deserialize(a1), G2.deserialize(a2), G1.deserialize(b1), G2.deserialize(b2));
        }
    }

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private static Address sender = avmRule.getPreminedAccount();
    private static Address contract;

    @BeforeClass
    public static void deployDapp() {
        byte[] g16DappBytes = avmRule.getDappBytes(PairingTestWrapper.class, null, 1,
                Fp.class, Fp2.class, G1.class, G1Point.class, G2.class, G2Point.class, Pairing.class, Util.class);
        contract = avmRule.deploy(sender, BigInteger.ZERO, g16DappBytes).getDappAddress();
    }

    @Test
    public void pairingProd2Test() {
        Fp g11x = new Fp(new BigInteger("2bcf154b010dedb450cfea4f635526973f39365ec204e4a8b0e3ecc29abb7e4e", 16));
        Fp g11y = new Fp(new BigInteger("23db84b7ae4e35681e833b6a1f6903e28291d154af3ec5ddc787e0e6cb058912", 16));
        G1Point g11 = new G1Point(g11x, g11y);

        Fp g12x = new Fp(new BigInteger("2bcf154b010dedb450cfea4f635526973f39365ec204e4a8b0e3ecc29abb7e4e", 16));
        Fp g12y = new Fp(new BigInteger("0c88c9bb32e36ac199cd0a4c6218547b14ef993cb93304af7498ab300d777435", 16));
        G1Point g12 = new G1Point(g12x, g12y);

        Fp2 g2x = new Fp2(new BigInteger("27d2525616cd883a2e952616138e052125201826d45e179a9ae28655338ca2be", 16),
                new BigInteger("2167ff55d36a2ed92eb480b1b9365382ea2facea90c860d63211827f122fdc29", 16));
        Fp2 g2y = new Fp2(new BigInteger("2c6e8b5d5da9a03f2d6b57bf2338168eca1e43409693b43659fe834149e506a9", 16),
                new BigInteger("020401d78e6fe746fe3d9512f9b4eedcfdd7eb5d08e307f1d6ee5d38f9a253ec", 16));
        G2Point g2 = new G2Point(g2x, g2y);

        byte[] txData = ABIUtil.encodeMethodArguments("pairingProd2", G1.serialize(g11), G2.serialize(g2), G1.serialize(g12), G2.serialize(g2));
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);
        assertTrue(r.getReceiptStatus().isSuccess());

        assertTrue(new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());
    }
}
