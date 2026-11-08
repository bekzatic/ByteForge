package com.byteforge.rules;

import com.byteforge.builder.PCBuilder;
import com.byteforge.model.Component;
import com.byteforge.model.Parts;

import java.util.ArrayList;
import java.util.List;

/**
 * CHAIN OF RESPONSIBILITY pattern - the <b>Concrete Handlers</b> and the standard chain.
 */
public final class CompatibilityRules {
    private CompatibilityRules() {}

    /** Recommended PSU headroom over the estimated peak draw. */
    public static final double PSU_HEADROOM = 1.25;

    /** The store's standard chain, in the order problems are reported to the customer. */
    public static CompatibilityRule standardChain() {
        CompatibilityRule first = new RequiredParts();
        first.then(new CpuSocket())
             .then(new MemoryType())
             .then(new MemorySlots())
             .then(new BoardFitsCase())
             .then(new GpuClearance())
             .then(new DriveBays())
             .then(new IntegratedGraphics())
             .then(new PowerBudget());
        return first;
    }

    /** Names of all rules in a chain, in order. */
    public static List<String> namesOf(CompatibilityRule first) {
        List<String> names = new ArrayList<>();
        for (CompatibilityRule r = first; r != null; r = r.getNext()) names.add(r.getName());
        return names;
    }

    // ------------------------------------------------------------ power helpers

    public static int estimatePower(List<Component> parts) {
        int sum = 0;
        for (Component c : parts) sum += c.getPowerDraw();
        return sum;
    }

    public static int recommendedPsuWatts(List<Component> parts) {
        return (int) Math.ceil(estimatePower(parts) * PSU_HEADROOM);
    }

    /** Ranking of form factors: a case accepts boards of its size or smaller. */
    static int formFactorSize(String ff) {
        switch (ff) {
            case "ITX":  return 1;
            case "mATX": return 2;
            case "ATX":  return 3;
            default:     return 99;
        }
    }

    // ------------------------------------------------------------ the rules

    public static class RequiredParts extends CompatibilityRule {
        @Override public String getName() { return "Required parts"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            if (b.getCpu() == null)         issues.add("A processor is required.");
            if (b.getMotherboard() == null) issues.add("A motherboard is required.");
            if (b.getRam() == null)         issues.add("Memory (RAM) is required.");
            if (b.getDrives().isEmpty())    issues.add("At least one storage drive is required.");
            if (b.getPsu() == null)         issues.add("A power supply is required.");
            if (b.getCase() == null)        issues.add("A case is required.");
        }
    }

    public static class CpuSocket extends CompatibilityRule {
        @Override public String getName() { return "CPU socket"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            Parts.Cpu cpu = b.getCpu();
            Parts.Motherboard mb = b.getMotherboard();
            if (cpu != null && mb != null && !cpu.getSocket().equals(mb.getSocket())) {
                issues.add("CPU socket " + cpu.getSocket() + " does not fit motherboard socket " + mb.getSocket() + ".");
            }
        }
    }

    public static class MemoryType extends CompatibilityRule {
        @Override public String getName() { return "Memory type"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            Parts.Ram ram = b.getRam();
            Parts.Motherboard mb = b.getMotherboard();
            if (ram != null && mb != null && !ram.getRamType().equals(mb.getRamType())) {
                issues.add(ram.getRamType() + " memory is not supported by a " + mb.getRamType() + " motherboard.");
            }
        }
    }

    public static class MemorySlots extends CompatibilityRule {
        @Override public String getName() { return "Memory slots"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            int max = b.getMotherboard() != null && "ITX".equals(b.getMotherboard().getFormFactor()) ? 2 : 4;
            if (b.getRamModules() < 1 || b.getRamModules() > max) {
                issues.add("This motherboard supports 1 to " + max + " RAM modules.");
            }
        }
    }

    public static class BoardFitsCase extends CompatibilityRule {
        @Override public String getName() { return "Board fits case"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            Parts.Motherboard mb = b.getMotherboard();
            Parts.PcCase pcCase = b.getCase();
            if (mb != null && pcCase != null
                    && formFactorSize(mb.getFormFactor()) > formFactorSize(pcCase.getFormFactor())) {
                issues.add(mb.getFormFactor() + " motherboard does not fit in the " + pcCase.getFormFactor() + " case.");
            }
        }
    }

    public static class GpuClearance extends CompatibilityRule {
        @Override public String getName() { return "GPU clearance"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            Parts.Gpu gpu = b.getGpu();
            Parts.PcCase pcCase = b.getCase();
            if (gpu != null && pcCase != null && gpu.getLengthMm() > pcCase.getMaxGpuLengthMm()) {
                issues.add("Graphics card is " + gpu.getLengthMm() + " mm but the case fits only "
                        + pcCase.getMaxGpuLengthMm() + " mm.");
            }
        }
    }

    /** Small cases have fewer drive bays: ITX 2, mATX 4, ATX 6. */
    public static class DriveBays extends CompatibilityRule {
        @Override public String getName() { return "Drive bays"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            Parts.PcCase pcCase = b.getCase();
            if (pcCase == null) return;
            int bays = 2 * formFactorSize(pcCase.getFormFactor());
            if (b.getDrives().size() > bays) {
                issues.add("The " + pcCase.getFormFactor() + " case has room for " + bays + " drives, "
                        + b.getDrives().size() + " selected.");
            }
        }
    }

    public static class IntegratedGraphics extends CompatibilityRule {
        @Override public String getName() { return "Integrated graphics"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            if (b.getGpu() == null && b.getCpu() != null && b.getCpu().getModel().endsWith("F")) {
                issues.add(b.getCpu().getModel() + " has no integrated graphics - add a graphics card.");
            }
        }
    }

    public static class PowerBudget extends CompatibilityRule {
        @Override public String getName() { return "Power budget"; }
        @Override protected void check(PCBuilder b, List<String> issues) {
            if (b.getPsu() == null) return;
            int needed = recommendedPsuWatts(b.loadParts());
            if (b.getPsu().getWatts() < needed) {
                issues.add("Power supply " + b.getPsu().getWatts() + " W is too weak; at least "
                        + needed + " W recommended.");
            }
        }
    }
}
