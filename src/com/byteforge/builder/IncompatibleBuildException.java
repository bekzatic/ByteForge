package com.byteforge.builder;

import java.util.ArrayList;
import java.util.List;

/** Thrown by {@link PCBuilder#build()} when the selected parts do not work together. */
public class IncompatibleBuildException extends RuntimeException {
    private final List<String> issues;

    public IncompatibleBuildException(List<String> issues) {
        super("Incompatible build: " + String.join(" ", issues));
        this.issues = new ArrayList<>(issues);
    }

    public List<String> getIssues() {
        return issues;
    }
}
