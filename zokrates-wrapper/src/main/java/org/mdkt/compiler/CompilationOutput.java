package org.mdkt.compiler;

import java.util.List;
import java.util.Map;

public class CompilationOutput {
    private Map<String, Class<?>> classMap;
    private List<CompiledCode> compiledCode;

    public CompilationOutput(Map<String, Class<?>> classMap, List<CompiledCode> compiledCode) {
        this.classMap = classMap;
        this.compiledCode = compiledCode;
    }

    public Map<String, Class<?>> getClassMap() {
        return classMap;
    }

    public List<CompiledCode> getCompiledCode() {
        return compiledCode;
    }
}
