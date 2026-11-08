package com.byteforge.builder;

import com.byteforge.model.Component;
import com.byteforge.rules.CompatibilityRule;
import com.byteforge.rules.CompatibilityRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point the builder uses for hardware validation. The actual rules are a
 * Chain of Responsibility ({@link CompatibilityRules#standardChain()}); this class
 * only starts the chain and offers the power helpers.
 */
public final class CompatibilityChecker {

    public static final double PSU_HEADROOM = CompatibilityRules.PSU_HEADROOM;

    private CompatibilityChecker() {}

    public static int estimatePower(List<Component> parts) {
        return CompatibilityRules.estimatePower(parts);
    }

    public static int recommendedPsuWatts(List<Component> parts) {
        return CompatibilityRules.recommendedPsuWatts(parts);
    }

    static List<String> check(PCBuilder b, CompatibilityRule chain) {
        List<String> issues = new ArrayList<>();
        chain.handle(b, issues);
        return issues;
    }

    static int recommendedFor(PCBuilder b) {
        return recommendedPsuWatts(b.loadParts());
    }
}
