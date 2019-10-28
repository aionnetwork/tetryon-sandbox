package org.aion.zokrates;

import org.aion.avm.core.dappreading.UserlibJarBuilder;
import org.aion.avm.tooling.deploy.OptimizedJarBuilder;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.util.*;

public class AvmCompiler {

    // hardcoded paths
    private static final File avmLibPath = new File("../artifacts/avm");

    public static byte[] generateAvmJar(String mainClassFullyQualifiedName, Map<String, String> code) throws Exception {
        HashMap<String, byte[]> allClasses = compileInMemory(code);
        byte[] mainClass = allClasses.remove(mainClassFullyQualifiedName);

        byte[] unoptimizedJar = UserlibJarBuilder.buildJarForExplicitClassNamesAndBytecode(
                mainClassFullyQualifiedName, mainClass, allClasses);

        return new OptimizedJarBuilder(true, unoptimizedJar, 2)
                .withUnreachableMethodRemover()
                .withRenamer()
                .withConstantRemover()
                .getOptimizedBytes();
    }

    private static HashMap<String, byte[]> compileInMemory(Map<String, String> sources) throws Exception {
        String cp = buildClassPath(avmLibPath.getCanonicalPath() + "/*" );

        List<String> optionList = new ArrayList<>();
        optionList.add("--release");
        optionList.add("10");
        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path") + ";" + cp);

        InMemoryJavaCompiler javac = InMemoryJavaCompiler.newInstance();

        javac.useOptions(optionList.toArray(new String[0]));

        for (Map.Entry<String, String> s : sources.entrySet()) {
            javac.addSource(s.getKey(), s.getValue());
        }

        List<CompiledCode> compiled = javac.compileAll();

        HashMap<String, byte[]> classBytes = new HashMap<>();
        for (CompiledCode c : compiled) {
            classBytes.put(c.getClassName(), c.getByteCode());
        }

        return classBytes;
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
