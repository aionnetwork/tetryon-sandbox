package org.aion.zokrates;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import javax.tools.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class Zokrates {

    public enum Scheme
    {
        G16("g16"),
        GM17("gm17"),
        PGHR13("pghr13");

        private String name;

        Scheme(String envUrl) {
            this.name = envUrl;
        }

        public String getName() {
            return name;
        }
    }

    // hardcoded paths
    File zokratesPath;
    File avmLibPath;

    // provided paths
    File workingDir;
    Scheme scheme;

    // constants
    final String rootCodeFileName = "root.code";
    final String generatedContractDir = "avm-verifier";
    final String contractClasspath = "org.oan.tetryon";

    public static final String AVM_CONTRACT_MAIN_CLASS = "Verifier.java";
    public static final List<String> AVM_CONTRACT_OTHER_CLASSES = Arrays.asList("Util.java", "Fp.java", "Fp2.java", "G1Point.java", "G1.java", "G2Point.java", "G2.java", "Pairing.java");

    // class state
    DefaultExecutor executor;

    /**
     * Generates the root.code file in the zokrates working directory provided. All commands will be executed in this directory.
     */
    public Zokrates(File workingDir, String sourceCode, Scheme scheme) throws IOException {
        // hardcoded paths
        this.zokratesPath = new File("../artifacts/zokrates");
        this.avmLibPath = new File("../artifacts/avm");

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

    public Zokrates compile() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " compile -i " + workingDir.getCanonicalPath() + "/" + rootCodeFileName);
        executor.execute(c);
        return this;
    }

    public Zokrates setup() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " setup --proving-scheme " + this.scheme.getName());
        executor.execute(c);
        return this;
    }

    public Zokrates exportAvmVerifier() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " export-avm-verifier --proving-scheme " + this.scheme.getName());
        executor.execute(c);
        return this;
    }

    public Zokrates computeWitness(String... args) throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " compute-witness -a ");
        c.addArguments(args);
        executor.execute(c);
        return this;
    }

    public Zokrates generateProof() throws IOException {
        CommandLine c = CommandLine.parse(zokratesPath.getCanonicalPath() + " generate-proof --proving-scheme " + this.scheme.getName());
        executor.execute(c);
        return this;
    }

    public static class AvmContract {
        private Class<?> mainClass;
        private ArrayList<Class<?>> otherClasses;

        public void setMainClass(Class<?> mainClass) {
            this.mainClass = mainClass;
        }

        public void setOtherClasses(ArrayList<Class<?>> otherClasses) {
            this.otherClasses = otherClasses;
        }

        public Class<?> getMainClass() {
            return mainClass;
        }

        public ArrayList<Class<?>> getOtherClasses() {
            return otherClasses;
        }
    }

    public AvmContract compileAndLoadContractClasses(File generatedClassfilePath) throws IOException, ClassNotFoundException {
        ArrayList<Class<?>> otherClasses = new ArrayList<>();
        AvmContract contract = new AvmContract();

        List<String> classes = new ArrayList<>();
        classes.addAll(AVM_CONTRACT_OTHER_CLASSES);
        classes.add(AVM_CONTRACT_MAIN_CLASS);

        for (int i=0; i < classes.size(); i++) {
            String c = classes.get(i);

            boolean didCompile = this.compile(new File(workingDir.getCanonicalPath() + "/" + generatedContractDir + "/" + c), generatedClassfilePath);
            if (!didCompile)
                throw new RuntimeException("Failed to compile file: " + c);

            Class<?> cf =  ClassLoader.getSystemClassLoader().loadClass(contractClasspath + "." + c.substring(0, c.lastIndexOf('.')));

            if (i == classes.size()-1)
                contract.setMainClass(cf);
            else
                otherClasses.add(cf);
        }

        contract.setOtherClasses(otherClasses);
        return contract;
    }

    private static String buildClassPath(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            if (path.endsWith("*")) {
                path = path.substring(0, path.length() - 1);
                File pathFile = new File(path);
                for (File file : Objects.requireNonNull(pathFile.listFiles())) {
                    if (file.isFile() && file.getName().endsWith(".jar")) {
                        sb.append(path);
                        sb.append(file.getName());
                        sb.append(System.getProperty("path.separator"));
                    }
                }
            } else {
                sb.append(path);
                sb.append(System.getProperty("path.separator"));
            }
        }
        return sb.toString();
    }

    private boolean compile(File srcFile, File outputPath) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        String cp = buildClassPath(this.avmLibPath.getCanonicalPath() + "/*" );

        List<String> optionList = new ArrayList<>();
        optionList.add("--release");
        optionList.add("10");

        optionList.add("-d");
        optionList.add(outputPath.getCanonicalPath());

        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path") + ";" + cp);

        Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(srcFile));

        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                optionList,
                null,
                compilationUnit);

        boolean r = task.call();

        if (!r) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource().toUri());
            }
        }

        return r;
    }
}
