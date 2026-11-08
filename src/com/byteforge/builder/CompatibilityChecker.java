package com.byteforge.builder;

import com.byteforge.model.Component;
import com.byteforge.model.Parts;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware rules used by the builder before it allows a {@link Computer} to exist.
 */
public final class CompatibilityChecker {

    /** Recommended PSU headroom over the estimated peak draw. */
    public static final double PSU_HEADROOM = 1.25;

    private CompatibilityChecker() {}

    public static int estimatePower(List<Component> parts) {
        int sum = 0;
        for (Component c : parts) {
            sum += c.getPowerDraw();
        }
        return sum;
    }

    public static int recommendedPsuWatts(List<Component> parts) {
        return (int) Math.ceil(estimatePower(parts) * PSU_HEADROOM);
    }

    /** Ranking of form factors: a case accepts boards of its size or smaller. */
    private static int formFactorSize(String ff) {
        switch (ff) {
            case "ITX":  return 1;
            case "mATX": return 2;
            case "ATX":  return 3;
            default:     return 99;
        }
    }

    static List<String> check(PCBuilder b) {
        List<String> issues = new ArrayList<>();
        if (b.cpu == null)         issues.add("A processor is required.");
        if (b.motherboard == null) issues.add("A motherboard is required.");
        if (b.ram == null)         issues.add("Memory (RAM) is required.");
        if (b.drives.isEmpty())    issues.add("At least one storage drive is required.");
        if (b.psu == null)         issues.add("A power supply is required.");
        if (b.pcCase == null)      issues.add("A case is required.");

        if (b.cpu != null && b.motherboard != null
                && !b.cpu.getSocket().equals(b.motherboard.getSocket())) {
            issues.add("CPU socket " + b.cpu.getSocket() + " does not fit motherboard socket "
                    + b.motherboard.getSocket() + ".");
        }
        if (b.ram != null && b.motherboard != null
                && !b.ram.getRamType().equals(b.motherboard.getRamType())) {
            issues.add(b.ram.getRamType() + " memory is not supported by a "
                    + b.motherboard.getRamType() + " motherboard.");
        }
        if (b.ramModules < 1 || b.ramModules > 4) {
            issues.add("Between 1 and 4 RAM modules are supported.");
        }
        if (b.motherboard != null && b.pcCase != null
                && formFactorSize(b.motherboard.getFormFactor()) > formFactorSize(b.pcCase.getFormFactor())) {
            issues.add(b.motherboard.getFormFactor() + " motherboard does not fit in the "
                    + b.pcCase.getFormFactor() + " case.");
        }
        if (b.gpu != null && b.pcCase != null && b.gpu.getLengthMm() > b.pcCase.getMaxGpuLengthMm()) {
            issues.add("Graphics card is " + b.gpu.getLengthMm() + " mm but the case fits only "
                    + b.pcCase.getMaxGpuLengthMm() + " mm.");
        }
        if (b.gpu == null && b.cpu != null && b.cpu.getModel().endsWith("F")) {
            issues.add(b.cpu.getModel() + " has no integrated graphics - add a graphics card.");
        }
        if (b.psu != null) {
            int needed = recommendedPsuWatts(partsOf(b));
            if (b.psu.getWatts() < needed) {
                issues.add("Power supply " + b.psu.getWatts() + " W is too weak; at least "
                        + needed + " W recommended.");
            }
        }
        return issues;
    }

    private static List<Component> partsOf(PCBuilder b) {
        List<Component> list = new ArrayList<>();
        if (b.cpu != null) list.add(b.cpu);
        if (b.motherboard != null) list.add(b.motherboard);
        if (b.ram != null) for (int i = 0; i < b.ramModules; i++) list.add(b.ram);
        if (b.gpu != null) list.add(b.gpu);
        list.addAll(b.drives);
        return list;
    }

    static int recommendedFor(PCBuilder b) {
        return recommendedPsuWatts(partsOf(b));
    }
}
