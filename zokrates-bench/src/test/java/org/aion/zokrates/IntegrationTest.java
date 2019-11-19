package org.aion.zokrates;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.aion.zokrates.TestUtils.*;

@SuppressWarnings("SameParameterValue")
public class IntegrationTest {
    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @SuppressWarnings("FieldCanBeLocal")
    private String mainClassname = "org.oan.tetryon.Verifier";
    private List<ProvingScheme> allSchemes = new ArrayList<>(Arrays.asList(ProvingScheme.G16, ProvingScheme.GM17, ProvingScheme.PGHR13));

    private void testSnarkForSchemes(String testName, String fileName,
                                   List<String[]> positiveArgs, List<String[]> negativeArgs, List<ProvingScheme> schemes) throws Exception {
        String code = readFileFromResources(fileName);

        for (ProvingScheme ps : schemes) {
            File workingDir = folder.newFolder(testName + ps.name());
            ZokratesProgram z = new ZokratesProgram(workingDir, code, ps);

            Map<String, String> contracts = z.compile().setup().exportAvmVerifier();

            Address dapp = deployContract(avmRule, mainClassname, contracts);

            for (String[] args : positiveArgs) {
                VerifyArgs x = z.computeWitness(args).generateProof();
                verifyAndAssertResult(avmRule, dapp, x, true);
            }

            for (String[] args : negativeArgs) {
                VerifyArgs x = z.computeWitness(args).generateProof();
                verifyAndAssertResult(avmRule, dapp, x, false);
            }
        }
    }

    @Test
    public void preimageTest() throws Exception {
        String methodName = testName.getMethodName();
        String fileName = "preimage.zok";

        List<String[]> positiveArgs = new ArrayList<>();
        positiveArgs.add(new String[]{"337", "113569"});

        List<String[]> negativeArgs = new ArrayList<>();
        negativeArgs.add(new String[]{"337", "113570"});

        testSnarkForSchemes(methodName, fileName, positiveArgs, negativeArgs, allSchemes);
    }
}
