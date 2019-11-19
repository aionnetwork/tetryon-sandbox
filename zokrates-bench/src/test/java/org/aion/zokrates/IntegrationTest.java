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

    // Prove solution for a 4x4 sudoku board, indexed like so:
    // (with public parameters: a21, b11, b22, c11, c22, d21)
    //
    // | a11 | a12 || b11 | b12 |
    // --------------------------
    // | a21 | a22 || b21 | b22 |
    // ==========================
    // | c11 | c12 || d11 | d12 |
    // --------------------------
    // | c21 | c22 || d21 | d22 |

    // def main(a21, b11, b22, c11, c22, d21, a11, a12, a22, b12, b21, c12, c21, d11, d12, d22)
    @Test
    public void sudoku4x4Test() throws Exception {
        class SudokuBoard {
            private final String a11, a12, a21, a22;
            private final String b11, b12, b21, b22;
            private final String c11, c12, c21, c22;
            private final String d11, d12, d21, d22;

            private SudokuBoard(String[] row1, String[] row2, String[] row3, String[] row4) {
                assert row1.length == 4 && row2.length == 4 && row3.length == 4 && row4.length == 4;
                // | a11 | a12 || b11 | b12 |
                a11 = row1[0];
                a12 = row1[1];
                b11 = row1[2];
                b12 = row1[3];
                // | a21 | a22 || b21 | b22 |
                a21 = row2[0];
                a22 = row2[1];
                b21 = row2[2];
                b22 = row2[3];
                // | c11 | c12 || d11 | d12 |
                c11 = row3[0];
                c12 = row3[1];
                d11 = row3[2];
                d12 = row3[3];
                // | c21 | c22 || d21 | d22 |
                c21 = row4[0];
                c22 = row4[1];
                d21 = row4[2];
                d22 = row4[3];
            }

            private String[] serialize() {
                return new String[]{a21, b11, b22, c11, c22, d21, a11, a12, a22, b12, b21, c12, c21, d11, d12, d22};
            }
        }
        String methodName = testName.getMethodName();
        String fileName = "sudoku4x4.zok";

        SudokuBoard correct = new SudokuBoard(
                new String[]{"1", "3", "4", "2"},
                new String[]{"4", "2", "3", "1"},
                new String[]{"3", "1", "2", "4"},
                new String[]{"2", "4", "1", "3"});

        SudokuBoard wrong = new SudokuBoard(
                new String[]{"1", "3", "4", "2"},
                new String[]{"4", "2", "3", "1"},
                new String[]{"3", "1", "2", "4"},
                new String[]{"2", "4", "1", "4"});

        List<String[]> positiveArgs = new ArrayList<>();
        positiveArgs.add(correct.serialize());

        List<String[]> negativeArgs = new ArrayList<>();
        negativeArgs.add(wrong.serialize());

        testSnarkForSchemes(methodName, fileName, positiveArgs, negativeArgs, allSchemes);
    }
}
