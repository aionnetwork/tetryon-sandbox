package org.aion.zokrates;

import org.aion.avm.core.dappreading.UserlibJarBuilder;
import org.aion.avm.tooling.deploy.OptimizedJarBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AvmCompiler {

    // hardcoded paths
    private static final File avmLibPath = new File("../artifacts/avm");
    private static final String generatedContractDir = "avm-verifier";
    private static final String classPrefix = "org.oan.tetryon";

    private static final String AVM_CONTRACT_MAIN_CLASS = "Verifier";
    private static final List<String> AVM_CONTRACT_OTHER_CLASSES = Arrays.asList("Util", "Fp", "Fp2", "G1Point", "G1", "G2Point", "G2", "Pairing");

    public static byte[] generateAvmJar(File workingDir) throws IOException {
        List<String> classNames = new ArrayList<>(AVM_CONTRACT_OTHER_CLASSES);
        classNames.add(AVM_CONTRACT_MAIN_CLASS);

        List<File> classPaths = new ArrayList<>();
        for (String c : classNames) {
            classPaths.add(new File(workingDir,generatedContractDir+"/"+c+".java"));
        }

        boolean didCompile = compile(classPaths, workingDir);
        if (!didCompile)
            throw new RuntimeException("could not compile java files");

        List<File> compiledClassFiles = (List<File>) FileUtils.listFiles(workingDir, new String[]{"class"}, true);

        HashMap<String, byte[]> classBytes = new HashMap<>();
        for (File f : compiledClassFiles) {
            String k = classPrefix + "." + FilenameUtils.removeExtension(f.getName());
            byte[] v = FileUtils.readFileToByteArray(f);
            classBytes.put(k, v);
        }

        String mainClassFqn = classPrefix + "." + AVM_CONTRACT_MAIN_CLASS;
        byte[] mainClassBytes = classBytes.remove(mainClassFqn);

        byte[] unoptimizedJar = UserlibJarBuilder.buildJarForExplicitClassNamesAndBytecode(
                classPrefix + "." + AVM_CONTRACT_MAIN_CLASS, mainClassBytes,
                classBytes);

        return new OptimizedJarBuilder(true, unoptimizedJar, 2)
                .withUnreachableMethodRemover()
                .withRenamer()
                .withConstantRemover()
                .getOptimizedBytes();
    }

    private static boolean compile(List<File> srcFiles, File outputPath) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        String cp = buildClassPath(avmLibPath.getCanonicalPath() + "/*" );

        List<String> optionList = new ArrayList<>();
        optionList.add("--release");
        optionList.add("10");

        optionList.add("-d");
        optionList.add(outputPath.getCanonicalPath());

        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path") + ";" + cp);

        Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(srcFiles);

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

}
