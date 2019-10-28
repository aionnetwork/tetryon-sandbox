package org.aion.zokrates;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("WeakerAccess")
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

    public ZokratesProgram exportAvmVerifier() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " export-avm-verifier --proving-scheme " + this.scheme.toString());
        executor.execute(c);
        return this;
    }

    public ZokratesProgram computeWitness(String... args) throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " compute-witness -a ");
        c.addArguments(args);
        executor.execute(c);
        return this;
    }

    public ZokratesProgram generateProof() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " generate-proof --proving-scheme " + this.scheme.toString());
        executor.execute(c);
        return this;
    }
}
