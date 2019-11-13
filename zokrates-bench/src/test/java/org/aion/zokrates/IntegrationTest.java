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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.aion.avm.embed.AvmRule.ResultWrapper;
import static org.aion.zokrates.Util.trimTrailingZeros;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SameParameterValue")
public class IntegrationTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);
    private static Address sender = avmRule.getPreminedAccount();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String loadResource(String fileName) throws IOException {
        // from src/test/resources folder
        return FileUtils.readFileToString(
                new File(Objects.requireNonNull(IntegrationTest.class.getClassLoader().getResource(fileName)).getFile()),
                (String) null);
    }

    private static String exportContractJar(String mainClassFullyQualifiedName, Map<String, String> code, String fileName, File outputDir) throws Exception {
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

    private static Address deployContract(String mainClassFullyQualifiedName, Map<String, String> code) throws Exception {
        byte [] optimizedJar = AvmCompiler.generateAvmJar(mainClassFullyQualifiedName, code);
        byte[] dappBytes = new CodeAndArguments(optimizedJar, null).encodeToBytes();

        ResultWrapper w = avmRule.deploy(sender, BigInteger.ZERO, dappBytes);
        assertTrue (w.getTransactionResult().energyUsed < 1_500_000);
        return w.getDappAddress();
    }

    private static void verifyAndAssertResult(Address dapp, VerifyArgs args, boolean verifyResult) {
        byte[] txData = ABIUtil.encodeMethodArguments("verify", args.getInput(), args.getProof().serialize());
        System.out.println("Encoded Transaction Data: " + Hex.encodeHexString(txData));
        ResultWrapper w = avmRule.call(sender, dapp, BigInteger.ZERO, txData);

        assertTrue(w.getReceiptStatus().isSuccess());
        assertTrue(w.getTransactionResult().energyUsed < 1000_000);
        assertEquals(new ABIDecoder(w.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean(), verifyResult);

        List<Log> logs = w.getLogs();
        Assert.assertEquals(1, logs.size());
        Assert.assertEquals(1, logs.get(0).copyOfTopics().size());
        Assert.assertEquals("VerifySnark", StringUtils.newStringUtf8(trimTrailingZeros(logs.get(0).copyOfTopics().get(0))));
        Assert.assertEquals(verifyResult ? BigInteger.ONE : BigInteger.ZERO, new BigInteger(logs.get(0).copyOfData()));
    }

    @Test
    public void preimageTestG16() throws Exception {
        File workingDir = folder.newFolder(testName.getMethodName());
        String code = loadResource("preimage.zok");

        ZokratesProgram z = new ZokratesProgram(workingDir, code, ProvingScheme.G16);

        Map<String, String> contracts = z.compile().setup().exportAvmVerifier();

        //String exportedFileName = exportContractJar("org.oan.tetryon.Verifier", contracts, "SquarePreimageVerifier", new File("export"));

        Address dapp = deployContract("org.oan.tetryon.Verifier", contracts);

        // positive test case
        VerifyArgs pos = z.computeWitness("337", "113569").generateProof();
        verifyAndAssertResult(dapp, pos, true);

        // negative test case
        VerifyArgs neg = z.computeWitness("337", "113570").generateProof();
        verifyAndAssertResult(dapp, neg, false);

        System.out.println("QED ...");
    }

    @Test
    public void preimageTestGM17() throws Exception {
        File workingDir = folder.newFolder(testName.getMethodName());
        String code = loadResource("preimage.zok");

        ZokratesProgram z = new ZokratesProgram(workingDir, code, ProvingScheme.GM17);

        Map<String, String> contracts = z.compile().setup().exportAvmVerifier();

        //String exportedFileName = exportContractJar("org.oan.tetryon.Verifier", contracts, "SquarePreimageVerifier", new File("export"));

        Address dapp = deployContract("org.oan.tetryon.Verifier", contracts);

        // positive test case
        VerifyArgs pos = z.computeWitness("337", "113569").generateProof();
        verifyAndAssertResult(dapp, pos, true);

        // negative test case
        VerifyArgs neg = z.computeWitness("337", "113570").generateProof();
        verifyAndAssertResult(dapp, neg, false);

        System.out.println("QED ...");
    }

    @Test
    public void preimageTestPghr13() throws Exception {
        File workingDir = folder.newFolder(testName.getMethodName());
        String code = loadResource("preimage.zok");

        ZokratesProgram z = new ZokratesProgram(workingDir, code, ProvingScheme.PGHR13);

        Map<String, String> contracts = z.compile().setup().exportAvmVerifier();

        //String exportedFileName = exportContractJar("org.oan.tetryon.Verifier", contracts, "SquarePreimageVerifier", new File("export"));

        Address dapp = deployContract("org.oan.tetryon.Verifier", contracts);

        // positive test case
        VerifyArgs pos = z.computeWitness("337", "113569").generateProof();
        verifyAndAssertResult(dapp, pos, true);

        // negative test case
        VerifyArgs neg = z.computeWitness("337", "113570").generateProof();
        verifyAndAssertResult(dapp, neg, false);

        System.out.println("QED ...");
    }
}
