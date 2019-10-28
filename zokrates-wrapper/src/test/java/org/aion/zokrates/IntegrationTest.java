package org.aion.zokrates;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.CodeAndArguments;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import static org.aion.tetryon.Verifier.Proof;

public class IntegrationTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);
    private static Address sender = avmRule.getPreminedAccount();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static final File compilePath = new File("out/test/classes");
    static final String packagePath = "/org/oan/tetryon";

    static String loadResource(String fileName) throws IOException {
        // from src/test/resources folder
        return FileUtils.readFileToString(
                new File(Objects.requireNonNull(IntegrationTest.class.getClassLoader().getResource(fileName)).getFile()),
                (String) null);
    }


    static Address deployContract(File workingDir) throws IOException, ClassNotFoundException {
        byte [] optimizedJar = AvmCompiler.generateAvmJar(workingDir);
        byte[] dappBytes = new CodeAndArguments(optimizedJar, null).encodeToBytes();

        AvmRule.ResultWrapper w = avmRule.deploy(sender, BigInteger.ZERO, dappBytes);
        Assert.assertTrue (w.getTransactionResult().energyUsed < 1_500_000);
        return w.getDappAddress();
    }

    @Test
    public void preimageTest() throws IOException, ParseException, ClassNotFoundException {
        // cleanup
        //FileUtils.deleteDirectory(new File(compilePath.getCanonicalPath() + packagePath));

        File workingDir = folder.newFolder(testName.getMethodName());
        String code = loadResource("preimage.zok");

        ZokratesProgram z = new ZokratesProgram(workingDir, code, ProvingScheme.G16);

        z.compile().setup().exportAvmVerifier();

        Address dapp = deployContract(workingDir);

        z.computeWitness("337", "113569").generateProof();

        final String generatedProof = FileUtils.readFileToString(new File(workingDir.getCanonicalPath() + "/proof.json"), (String) null);
        Proof proof = Util.parseProof(generatedProof);
        BigInteger[] input = Util.parseInput(generatedProof);

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input, proof.serialize());
        AvmRule.ResultWrapper r = avmRule.call(sender, dapp, BigInteger.ZERO, txData);

        Assert.assertTrue(r.getReceiptStatus().isSuccess());
        Assert.assertTrue(r.getTransactionResult().energyUsed < 500_000);
        Assert.assertTrue(new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());
    }
}
