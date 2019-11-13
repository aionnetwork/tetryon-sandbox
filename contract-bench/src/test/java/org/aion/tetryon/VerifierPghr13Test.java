package org.aion.tetryon;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.types.Log;
import org.aion.types.TransactionStatus;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class VerifierPghr13Test {

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private static Address sender = avmRule.getPreminedAccount();
    private static Address contract;

    @BeforeClass
    public static void deployDapp() {
        byte[] pghr13DappBytes = avmRule.getDappBytes(VerifierPghr13.class, null, 1,
                Fp.class, Fp2.class, G1.class, G1Point.class, G2.class, G2Point.class, Pairing.class, Util.class);
        AvmRule.ResultWrapper w = avmRule.deploy(sender, BigInteger.ZERO, pghr13DappBytes);
        Assert.assertTrue (w.getTransactionResult().energyUsed < 1_500_000);
        contract = w.getDappAddress();
    }

    static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    // positive test-case for square pre-image verifier: a=337, b=113569 (a^2 == b)
    @Test
    public void pghr13TestVerify() {
        G1Point a = new G1Point("09cba536a0323f53c5525ee1f2415ced788629664ff359f8cde6503ba939113b",
                "14050b5fe8912d7b288cfe6629af86c546a5548fc5f7665aa7133dfc9a10bb47");

        G1Point a_p = new G1Point("1c0281a9eaa3155aa6e25850252872cb8e487b5cee4f13a47d5d277bd7ac269a",
                "0a3d27e61d61b7b629847311c2c713d015af06c8bfcce7086c57b8da05b2a5bc");

        G2Point b = new G2Point("1bf3b08a6b1f2368b8901cd2ad1a08e20ef2a6ad6573b3f059f7bfe41659f4d0",
                            "26670c96b1866fef3240213796d18e75e16377a3d63840b11f4cc7778a1f66ba",
                            "0e42deeaee0637d38d02fd518c29975f5e422ad806d8b8cc4d3896cfcaebc7e1",
                            "155f418ec6f01c83c101a684030b2065166905e8a31c661e6bf55a17cc1f7e46");

        G1Point b_p = new G1Point("1c314693f6db4c29a7ccf6c13a3865a2935825709f5c0b5f18e50e2a003c3860",
                "0a890a6d109f03b5c21132a115c73cb41b773e417072ce5dc0d8517636b2a091");

        G1Point c = new G1Point("1fe0f755499612a188d46ddae0397c97798695e52d6467acfc408b076895fea5",
                "214f71ba3886d322eb13158266d4c191df9b32e9422a7594d748be4de392afa0");

        G1Point c_p = new G1Point("0c03e04ad7241f638aabcd8a9d8e196df6206967166b0b4ad4513c107f3fd226",
                "2a530fc91db219e41a55d21130c1f0b5cc6e7abb78fb859190e30e76d5387e7d");

        G1Point h = new G1Point("19bac844392d6a5dc98733081e13ec356f1595a18c837f1952f70febd1177419",
                "0bc6e207160ef79ad55e73bd2d06f2a40e267a3c86014ab0cc3f59def95396da");

        G1Point k = new G1Point("12fcae98cc80d67b1aaa733ad0f7a540f8986ae32bef476968bc4f5376bb9b68",
                "11327e1921749e4ca9dffd1e1f527b4f09ccc7e8922bd067a649b5a264af21db");

        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba1", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input,
                new VerifierPghr13.Proof(a, a_p, b, b_p, c, c_p, k, h).serialize());
        AvmRule.ResultWrapper w = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        // transaction should succeed
        Assert.assertTrue(w.getReceiptStatus().isSuccess());
        // transaction should not cost too much
        Assert.assertTrue(w.getTransactionResult().energyUsed < 1000_000);
        // verify should return "true"
        Assert.assertTrue(new ABIDecoder(w.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());

        List<Log> logs = w.getLogs();
        Assert.assertEquals(1, logs.size());
        Assert.assertEquals(1, logs.get(0).copyOfTopics().size());
        Assert.assertEquals("VerifySnark", StringUtils.newStringUtf8(trim(logs.get(0).copyOfTopics().get(0))));
        Assert.assertEquals(BigInteger.ONE, new BigInteger(logs.get(0).copyOfData()));

    }

    // negative test-case for square pre-image verifier: a=337, b=113570 (a^2 != b)
    @Test
    public void pghr13TestReject() {
        G1Point a = new G1Point("09cba536a0323f53c5525ee1f2415ced788629664ff359f8cde6503ba939113b",
                "14050b5fe8912d7b288cfe6629af86c546a5548fc5f7665aa7133dfc9a10bb47");

        G1Point a_p = new G1Point("09cba536a0323f53c5525ee1f2415ced788629664ff359f8cde6503ba939113b",
                "14050b5fe8912d7b288cfe6629af86c546a5548fc5f7665aa7133dfc9a10bb47");

        G2Point b = new G2Point("1bf3b08a6b1f2368b8901cd2ad1a08e20ef2a6ad6573b3f059f7bfe41659f4d0",
                "26670c96b1866fef3240213796d18e75e16377a3d63840b11f4cc7778a1f66ba",
                "0e42deeaee0637d38d02fd518c29975f5e422ad806d8b8cc4d3896cfcaebc7e1",
                "155f418ec6f01c83c101a684030b2065166905e8a31c661e6bf55a17cc1f7e46");

        G1Point b_p = new G1Point("1c314693f6db4c29a7ccf6c13a3865a2935825709f5c0b5f18e50e2a003c3860",
                "0a890a6d109f03b5c21132a115c73cb41b773e417072ce5dc0d8517636b2a091");

        G1Point c = new G1Point("1fe0f755499612a188d46ddae0397c97798695e52d6467acfc408b076895fea5",
                "214f71ba3886d322eb13158266d4c191df9b32e9422a7594d748be4de392afa0");

        G1Point c_p = new G1Point("0c03e04ad7241f638aabcd8a9d8e196df6206967166b0b4ad4513c107f3fd226",
                "2a530fc91db219e41a55d21130c1f0b5cc6e7abb78fb859190e30e76d5387e7d");

        G1Point h = new G1Point("19bac844392d6a5dc98733081e13ec356f1595a18c837f1952f70febd1177419",
                "0bc6e207160ef79ad55e73bd2d06f2a40e267a3c86014ab0cc3f59def95396da");

        G1Point k = new G1Point("12fcae98cc80d67b1aaa733ad0f7a540f8986ae32bef476968bc4f5376bb9b68",
                "11327e1921749e4ca9dffd1e1f527b4f09ccc7e8922bd067a649b5a264af21db");

        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba1", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input,
                new VerifierPghr13.Proof(a, a_p, b, b_p, c, c_p, k, h).serialize());
        AvmRule.ResultWrapper w = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        // transaction should succeed
        Assert.assertTrue(w.getReceiptStatus().isSuccess());
        // transaction should not cost too much
        Assert.assertTrue(w.getTransactionResult().energyUsed < 1000_000);
        // verify should return "true"
        Assert.assertFalse(new ABIDecoder(w.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());

        List<Log> logs = w.getLogs();
        Assert.assertEquals(1, logs.size());
        Assert.assertEquals(1, logs.get(0).copyOfTopics().size());
        Assert.assertEquals("VerifySnark", StringUtils.newStringUtf8(trim(logs.get(0).copyOfTopics().get(0))));
        Assert.assertEquals(BigInteger.ZERO, new BigInteger(logs.get(0).copyOfData()));
    }

    @Test
    public void pghr13TestBadInput1() {
        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba2", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input, new byte[4]);
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        TransactionStatus s = r.getReceiptStatus();
        Assert.assertTrue(s.isReverted());
    }

    @Test
    public void pghr13TestBadInput2() {
        G1Point a = new G1Point(
                new Fp(new BigInteger("07f4a1ab12b1211149fa0aed8ade3442b774893dcd1caffb8693ade54999c164", 16)),
                new Fp(new BigInteger("23b7f10c5e1aeaffafa088f1412c0f307969ba3f8f9d5920214a4cb91693fab5", 16)));

        G2Point b = new G2Point(
                new Fp2(new BigInteger("1f6cc814cf1df1ceb663378c496f168bcd21e19bb529e90fcf3721f8df6b4128", 16),
                        new BigInteger("079ee30e2c79e15be67645838a3177f681ab111edacf6f4867e8eed753ed9681", 16)),
                new Fp2(new BigInteger("2779dd0accaa1391e29ad54bf065819cac3129edda4eaf909d6ea2c7495a47f7", 16),
                        new BigInteger("20105b11ae5fbdc7067102d4260c8913cdcb512632680221d7644f9928a7e51d", 16)));

        G1Point c = new G1Point(
                new Fp(new BigInteger("153c3a313679a5c11010c3339ff4f787246ed2e8d736efb615aeb321f5a22432", 16)),
                new Fp(new BigInteger("06691d8441c35768a4ca87a5f5ee7d721bf13115d2a16726c12cda295a19bf09", 16)));

        byte[] txData = ABIUtil.encodeMethodArguments("verify", new BigInteger[]{}, new VerifierG16.Proof(a, b, c).serialize());
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        TransactionStatus s = r.getReceiptStatus();
        Assert.assertTrue(s.isReverted());
    }
}
