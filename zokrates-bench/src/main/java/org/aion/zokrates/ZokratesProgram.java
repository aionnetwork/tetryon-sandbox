package org.aion.zokrates;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class ZokratesProgram {
    // hardcoded paths
    private final File zokratesPath = new File("../artifacts/zokrates");

    // provided paths
    File workingDir;
    ProvingScheme scheme;

    // constants
    final String rootCodeFileName = "root.code";

    // class state
    DefaultExecutor executor;

    /**
     * Generates the root.code file in the zokrates working directory provided. All commands will be executed in this directory.
     */
    public ZokratesProgram(File workingDir, String sourceCode, ProvingScheme scheme) throws IOException {
        // provided paths
        this.workingDir = workingDir;
        this.scheme = scheme;

        String rootCodePath = workingDir.getCanonicalPath() + "/" + rootCodeFileName;

        // write source code to the working directory
        FileUtils.writeStringToFile(new File(rootCodePath), sourceCode, (String) null, false);

        // setup the executor
        executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDir);
    }

    public ZokratesProgram compile() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " compile -i " + workingDir.getCanonicalPath() + "/" + rootCodeFileName);
        executor.execute(c);
        return this;
    }

    public ZokratesProgram setup() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " setup --proving-scheme " + this.scheme.toString());
        executor.execute(c);
        return this;
    }

    public Map<String, String> exportAvmVerifier() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " export-avm-verifier --proving-scheme " + this.scheme.toString());
        executor.execute(c);

        List<File> contractFiles = (List<File>) FileUtils.listFiles(new File(workingDir, "avm-verifier"), new String[]{"java"}, true);

        HashMap<String, String> contracts = new HashMap<>();
        for (File f : contractFiles) {
            String v = FileUtils.readFileToString(f, (String) null);
            String k = getPackageName(v) + "." + FilenameUtils.removeExtension(f.getName());
            contracts.put(k, v);
        }

        return contracts;
    }

    public ZokratesProgram computeWitness(String... args) throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " compute-witness -a ");
        c.addArguments(args);
        executor.execute(c);
        return this;
    }

    public VerifyArgs generateProof() throws Exception {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " generate-proof --proving-scheme " + this.scheme.toString());
        executor.execute(c);

        final String generatedProof = FileUtils.readFileToString(new File(workingDir, "proof.json"), (String) null);

        return VerifyArgs.parseJson(generatedProof, this.scheme);
    }

    // utility
    private String getPackageName(String code) {
        Pattern regex = Pattern.compile("package\\s+([a-zA_Z][.\\w]*);");
        Matcher m = regex.matcher(code);
        if (m.find()) {
            return m.group(1);
        }

        throw new RuntimeException("malformed java file");
    }
}
