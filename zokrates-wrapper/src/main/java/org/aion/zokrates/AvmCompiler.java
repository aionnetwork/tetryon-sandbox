package org.aion.zokrates;

import org.aion.avm.core.dappreading.UserlibJarBuilder;
import org.aion.avm.tooling.deploy.OptimizedJarBuilder;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.util.*;

public class AvmCompiler {

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
        List<String> optionList = new ArrayList<>();
        optionList.add("--release");
        optionList.add("10");

        InMemoryJavaCompiler javac = InMemoryJavaCompiler.newInstance();

        javac.useOptions(optionList.toArray(new String[0]));

        for (Map.Entry<String, String> s : sources.entrySet()) {
            javac.addSource(s.getKey(), s.getValue());
        }

        List<CompiledCode> compiled = javac.compileAll().getCompiledCode();

        HashMap<String, byte[]> classBytes = new HashMap<>();
        for (CompiledCode c : compiled) {
            classBytes.put(c.getClassName(), c.getByteCode());
        }

        return classBytes;
    }
}
