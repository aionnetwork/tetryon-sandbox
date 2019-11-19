package org.aion.zokrates;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.CodeAndArguments;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.types.Log;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.aion.zokrates.Util.trimTrailingZeros;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class TestUtils {
    static String readFileFromResources(String fileName) throws IOException {
        // from src/test/resources folder
        return FileUtils.readFileToString(
                new File(Objects.requireNonNull(IntegrationTest.class.getClassLoader().getResource(fileName)).getFile()),
                (String) null);
    }

    private static String exportContractJar(String mainClassFullyQualifiedName, Map<String, String> code, String fileName, File outputDir) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        outputDir.mkdirs();
        if (!outputDir.isDirectory()) throw new UnsupportedOperationException();

        byte [] optimizedJar = AvmCompiler.generateAvmJar(mainClassFullyQualifiedName, code);

        // append top 5 bytes of md5 hash of the jar bytes, to avoid overwriting any previous jars
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(optimizedJar);
        if (digest.length != 16) throw new RuntimeException();
        String hash = Hex.encodeHexString(digest).toLowerCase().substring(0, 8);

        String outputFileName = fileName + "-" + hash + ".jar";
        File outputPath = new File(outputDir, outputFileName);
        FileUtils.writeByteArrayToFile(outputPath, optimizedJar);

        return outputFileName;
    }

    @SuppressWarnings("SameParameterValue")
    static Address deployContract(AvmRule avmRule, String mainClassFullyQualifiedName, Map<String, String> code) throws Exception {
        byte [] optimizedJar = AvmCompiler.generateAvmJar(mainClassFullyQualifiedName, code);
        byte[] dappBytes = new CodeAndArguments(optimizedJar, null).encodeToBytes();

        AvmRule.ResultWrapper w = avmRule.deploy(avmRule.getPreminedAccount(), BigInteger.ZERO, dappBytes);
        assertTrue (w.getTransactionResult().energyUsed < 1_500_000);
        return w.getDappAddress();
    }

    static void verifyAndAssertResult(AvmRule avmRule, Address dapp, VerifyArgs args, boolean verifyResult) {
        byte[] txData = ABIUtil.encodeMethodArguments("verify", args.getInput(), args.getProof().serialize());
        System.out.println("Encoded Transaction Data: " + Hex.encodeHexString(txData));

        long t0 = System.nanoTime();
        AvmRule.ResultWrapper w = avmRule.call(avmRule.getPreminedAccount(), dapp, BigInteger.ZERO, txData);
        long t1 = System.nanoTime();

        long durationNanoSec = (t1 - t0);
        double durationMilliSec = durationNanoSec / 1000000.0;
        double durationSec = durationMilliSec / 1000.0;

        System.out.println("AVM call() took " +
                (durationSec > 1 ? String.format("%.6f seconds", durationSec) : String.format("%.6f ms", durationMilliSec)));

        assertTrue(w.getReceiptStatus().isSuccess());
        System.out.println("AVM call() used NRG: " + String.format("%,d", w.getTransactionResult().energyUsed));
        assertEquals(new ABIDecoder(w.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean(), verifyResult);

        List<Log> logs = w.getLogs();
        Assert.assertEquals(1, logs.size());
        Assert.assertEquals(1, logs.get(0).copyOfTopics().size());
        Assert.assertEquals("VerifySnark", StringUtils.newStringUtf8(trimTrailingZeros(logs.get(0).copyOfTopics().get(0))));
        Assert.assertEquals(verifyResult ? BigInteger.ONE : BigInteger.ZERO, new BigInteger(logs.get(0).copyOfData()));
    }
}
