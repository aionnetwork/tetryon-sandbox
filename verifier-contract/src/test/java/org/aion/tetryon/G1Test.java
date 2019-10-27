package org.aion.tetryon;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class G1Test {

    @SuppressWarnings("unused")
    public static class G1TestWrapper {
        @Callable
        public static byte[] negate(byte[] p) {
            return G1.serialize(G1.negate(G1.deserialize(p)));
        }

        @Callable
        public static byte[] add(byte[] p1, byte[] p2) throws Exception {
            return G1.serialize(G1.add(G1.deserialize(p1), G1.deserialize(p2)));
        }

        @Callable
        public static byte[] mul(byte[] p, BigInteger s) throws Exception {
            return G1.serialize(G1.mul(G1.deserialize(p), s));
        }
    }

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private static Address sender = avmRule.getPreminedAccount();
    private static Address contract;

    @BeforeClass
    public static void deployDapp() {
        byte[] g16DappBytes = avmRule.getDappBytes(G1TestWrapper.class, null, 1,
                Fp.class, Fp2.class, G1.class, G1Point.class, G2.class, G2Point.class, Pairing.class, Util.class);
        contract = avmRule.deploy(sender, BigInteger.ZERO, g16DappBytes).getDappAddress();
    }

    @Test
    public void addTest() {
        Fp ax = new Fp(new BigInteger("222480c9f95409bfa4ac6ae890b9c150bc88542b87b352e92950c340458b0c09", 16));
        Fp ay = new Fp(new BigInteger("2976efd698cf23b414ea622b3f720dd9080d679042482ff3668cb2e32cad8ae2", 16));
        Fp bx = new Fp(new BigInteger("1bd20beca3d8d28e536d2b5bd3bf36d76af68af5e6c96ca6e5519ba9ff8f5332", 16));
        Fp by = new Fp(new BigInteger("2a53edf6b48bcf5cb1c0b4ad1d36dfce06a79dcd6526f1c386a14d8ce4649844", 16));
        Fp cx = new Fp(new BigInteger("16c7c4042e3a725ddbacf197c519c3dcad2bc87dfd9ac7e1e1631154ee0b7d9c", 16));
        Fp cy = new Fp(new BigInteger("19cd640dd28c9811ebaaa095a16b16190d08d6906c4f926fce581985fe35be0e", 16));
        G1Point a = new G1Point(ax, ay);
        G1Point b = new G1Point(bx, by);

        byte[] txData = ABIUtil.encodeMethodArguments("add", G1.serialize(a), G1.serialize(b));
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);
        Assert.assertTrue(r.getReceiptStatus().isSuccess());

        byte[] result = new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneByteArray();

        G1Point c = G1.deserialize(result);
        assertEquals(c.x, cx);
        assertEquals(c.y, cy);
    }

    @Test
    public void mulTest() {
        Fp px = new Fp(new BigInteger("1e462d01d1861f7ee499bf70ab12ade335d98586b52db847ee2ec1e790170e04", 16));
        Fp py = new Fp(new BigInteger("14bd807f4e64904b29e874fd824ff16e465b5798b19aafe0cae60a2dbcf91333", 16));
        Fp qx = new Fp(new BigInteger("15ea829def65cb28c5435094e1b8d06cb021a8671319cdad074ee89ce7c2c0bf", 16));
        Fp qy = new Fp(new BigInteger("0b68b46b86de49221fe4dbdce9b88518812c9d48fb502ada0a2ad9fc28312c89", 16));
        G1Point p = new G1Point(px, py);
        BigInteger s = new BigInteger("30586f85e8fcea91c0db1ed30aacf7350e72efd4cf756b3ce309f2159e275ff9", 16);

        byte[] txData = ABIUtil.encodeMethodArguments("mul", G1.serialize(p), s);
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);
        Assert.assertTrue(r.getReceiptStatus().isSuccess());

        byte[] result = new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneByteArray();

        G1Point q = G1.deserialize(result);

        assertEquals(q.x, qx);
        assertEquals(q.y, qy);
    }
}