package com.byteforge.builder;

import com.byteforge.model.Component;
import com.byteforge.model.Parts;
import com.byteforge.rules.CompatibilityRule;
import com.byteforge.rules.CompatibilityRules;

import java.util.ArrayList;
import java.util.List;

/**
 * BUILDER pattern - the <b>Builder</b>.
 * <p>
 * Collects parts step by step through a fluent API and only produces a
 * {@link Computer} from {@link #build()} if the configuration is physically
 * compatible. Callers that want live feedback (the GUI) use {@link #validate()}.
 */
public class PCBuilder {
    String name = "Custom PC";
    Parts.Cpu cpu;
    Parts.Motherboard motherboard;
    Parts.Ram ram;
    int ramModules = 2;
    Parts.Gpu gpu;
    final List<Parts.Storage> drives = new ArrayList<>();
    Parts.PowerSupply psu;
    Parts.PcCase pcCase;

    public PCBuilder name(String name) { this.name = name; return this; }
    public PCBuilder cpu(Parts.Cpu cpu) { this.cpu = cpu; return this; }
    public PCBuilder motherboard(Parts.Motherboard mb) { this.motherboard = mb; return this; }
    public PCBuilder ram(Parts.Ram ram, int modules) { this.ram = ram; this.ramModules = modules; return this; }
    public PCBuilder gpu(Parts.Gpu gpu) { this.gpu = gpu; return this; }
    public PCBuilder addDrive(Parts.Storage drive) { if (drive != null) drives.add(drive); return this; }
    public PCBuilder clearDrives() { drives.clear(); return this; }
    public PCBuilder psu(Parts.PowerSupply psu) { this.psu = psu; return this; }
    public PCBuilder pcCase(Parts.PcCase pcCase) { this.pcCase = pcCase; return this; }

    /** Generic setter used by the GUI combo boxes. */
    public PCBuilder part(Component c) {
        if (c instanceof Parts.Cpu) cpu((Parts.Cpu) c);
        else if (c instanceof Parts.Motherboard) motherboard((Parts.Motherboard) c);
        else if (c instanceof Parts.Ram) ram((Parts.Ram) c, ramModules);
        else if (c instanceof Parts.Gpu) gpu((Parts.Gpu) c);
        else if (c instanceof Parts.Storage) { clearDrives(); addDrive((Parts.Storage) c); }
        else if (c instanceof Parts.PowerSupply) psu((Parts.PowerSupply) c);
        else if (c instanceof Parts.PcCase) pcCase((Parts.PcCase) c);
        return this;
    }

    /** Lists every compatibility problem of the current configuration (empty = OK). */
    public List<String> validate() {
        return CompatibilityChecker.check(this, rules);
    }

    /** Replaces the rule chain (Chain of Responsibility), e.g. to add a store-specific rule. */
    public PCBuilder rules(CompatibilityRule chain) {
        this.rules = chain;
        return this;
    }

    private CompatibilityRule rules = CompatibilityRules.standardChain();

    // ---- read-only view used by the compatibility rules
    public Parts.Cpu getCpu() { return cpu; }
    public Parts.Motherboard getMotherboard() { return motherboard; }
    public Parts.Ram getRam() { return ram; }
    public int getRamModules() { return ramModules; }
    public Parts.Gpu getGpu() { return gpu; }
    public List<Parts.Storage> getDrives() { return java.util.Collections.unmodifiableList(drives); }
    public Parts.PowerSupply getPsu() { return psu; }
    public Parts.PcCase getCase() { return pcCase; }

    /** Every part that draws power (RAM counted once per module). */
    public List<Component> loadParts() {
        List<Component> list = new ArrayList<>();
        if (cpu != null) list.add(cpu);
        if (motherboard != null) list.add(motherboard);
        if (ram != null) for (int i = 0; i < ramModules; i++) list.add(ram);
        if (gpu != null) list.add(gpu);
        list.addAll(drives);
        return list;
    }

    public int recommendedPsuWatts() {
        return CompatibilityChecker.recommendedFor(this);
    }

    public Computer build() {
        List<String> issues = validate();
        if (!issues.isEmpty()) {
            throw new IncompatibleBuildException(issues);
        }
        return new Computer(this);
    }
}
