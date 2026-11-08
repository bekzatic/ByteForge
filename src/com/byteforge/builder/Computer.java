package com.byteforge.builder;

import com.byteforge.model.Component;
import com.byteforge.model.Parts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BUILDER pattern - the <b>Product</b>.
 * <p>
 * An immutable, already-validated PC configuration. It has many parts, some optional
 * (GPU) and some repeated (RAM modules, drives), which is exactly the situation where a
 * telescoping constructor becomes unreadable and a builder is preferred.
 * Instances can only be created by {@link PCBuilder}.
 */
public final class Computer {
    private final String name;
    private final Parts.Cpu cpu;
    private final Parts.Motherboard motherboard;
    private final Parts.Ram ram;
    private final int ramModules;
    private final Parts.Gpu gpu; // may be null (integrated graphics)
    private final List<Parts.Storage> drives;
    private final Parts.PowerSupply psu;
    private final Parts.PcCase pcCase;

    Computer(PCBuilder b) {
        this.name = b.name;
        this.cpu = b.cpu;
        this.motherboard = b.motherboard;
        this.ram = b.ram;
        this.ramModules = b.ramModules;
        this.gpu = b.gpu;
        this.drives = Collections.unmodifiableList(new ArrayList<>(b.drives));
        this.psu = b.psu;
        this.pcCase = b.pcCase;
    }

    public String getName() { return name; }
    public Parts.Cpu getCpu() { return cpu; }
    public Parts.Motherboard getMotherboard() { return motherboard; }
    public Parts.Ram getRam() { return ram; }
    public int getRamModules() { return ramModules; }
    public Parts.Gpu getGpu() { return gpu; }
    public List<Parts.Storage> getDrives() { return drives; }
    public Parts.PowerSupply getPsu() { return psu; }
    public Parts.PcCase getCase() { return pcCase; }

    public int getTotalRamGb() {
        return ram.getCapacityGb() * ramModules;
    }

    /** Every physical unit that must be taken from stock (RAM repeated per module). */
    public List<Component> allParts() {
        List<Component> list = new ArrayList<>();
        list.add(cpu);
        list.add(motherboard);
        for (int i = 0; i < ramModules; i++) {
            list.add(ram);
        }
        if (gpu != null) {
            list.add(gpu);
        }
        list.addAll(drives);
        list.add(psu);
        list.add(pcCase);
        return list;
    }

    public double getHardwarePrice() {
        return allParts().stream().mapToDouble(Component::getPrice).sum();
    }

    public int getEstimatedPowerDraw() {
        return CompatibilityChecker.estimatePower(allParts());
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append('\n');
        for (Component c : allParts().stream().distinct().toArray(Component[]::new)) {
            String qty = (c == ram && ramModules > 1) ? ramModules + " x " : "";
            sb.append(String.format("  %-15s %s%s%n", c.getType().label() + ":", qty, c.getName()));
        }
        sb.append(String.format("  %-15s %d GB%n", "Total RAM:", getTotalRamGb()));
        sb.append(String.format("  %-15s ~%d W (PSU %d W)%n", "Power draw:", getEstimatedPowerDraw(), psu.getWatts()));
        return sb.toString();
    }
}
