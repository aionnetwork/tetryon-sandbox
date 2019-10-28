package org.aion.zokrates;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.CodeAndArguments;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

import static org.aion.avm.embed.AvmRule.ResultWrapper;
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


    private static Address deployContract(String mainClassFullyQualifiedName, Map<String, String> code) throws Exception {
        byte [] optimizedJar = AvmCompiler.generateAvmJar(mainClassFullyQualifiedName, code);
        byte[] dappBytes = new CodeAndArguments(optimizedJar, null).encodeToBytes();

        ResultWrapper w = avmRule.deploy(sender, BigInteger.ZERO, dappBytes);
        assertTrue (w.getTransactionResult().energyUsed < 1_500_000);
        return w.getDappAddress();
    }

    private static void callVerifyAndAssertBoolean(Address dapp, VerifyArgs args, boolean verifyResult) {
        byte[] txData = ABIUtil.encodeMethodArguments("verify", args.getInput(), args.getProof().serialize());
        ResultWrapper w = avmRule.call(sender, dapp, BigInteger.ZERO, txData);

        assertTrue(w.getReceiptStatus().isSuccess());
        assertTrue(w.getTransactionResult().energyUsed < 500_000);
        assertEquals(new ABIDecoder(w.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean(), verifyResult);
    }

    @Test
    public void preimageTest() throws Exception {
        File workingDir = folder.newFolder(testName.getMethodName());
        String code = loadResource("preimage.zok");

        ProvingScheme ps = ProvingScheme.G16;

        ZokratesProgram z = new ZokratesProgram(workingDir, code, ps);

        Map<String, String> contracts = z.compile().setup().exportAvmVerifier();

        Address dapp = deployContract("org.oan.tetryon.Verifier", contracts);

        // positive test case
        VerifyArgs pos = z.computeWitness("337", "113569").generateProof();
        callVerifyAndAssertBoolean(dapp, pos, true);

        // negative test case
        VerifyArgs neg = z.computeWitness("337", "113570").generateProof();
        callVerifyAndAssertBoolean(dapp, neg, false);
    }
}
