package com.byteforge.builder;

import com.byteforge.inventory.Inventory;

/**
 * BUILDER pattern - the <b>Director</b>.
 * <p>
 * Knows the recipes of the store's ready-made configurations and drives a
 * {@link PCBuilder} through the right steps. The customer can then keep tweaking
 * the returned builder before calling {@code build()}.
 */
public class BuildDirector {

    public enum Preset {
        OFFICE("Office Starter"),
        GAMING("Gaming Pro"),
        WORKSTATION("Creator Workstation");

        private final String title;

        Preset(String title) { this.title = title; }

        @Override public String toString() { return title; }
    }

    private final Inventory inventory;

    public BuildDirector(Inventory inventory) {
        this.inventory = inventory;
    }

    public PCBuilder construct(Preset preset) {
        switch (preset) {
            case OFFICE:      return office();
            case GAMING:      return gaming();
            case WORKSTATION: return workstation();
            default: throw new IllegalArgumentException(preset.name());
        }
    }

    private PCBuilder office() {
        return new PCBuilder()
                .name(Preset.OFFICE.toString())
                .cpu(inventory.findBySku("NT-CPU-01"))
                .motherboard(inventory.findBySku("NT-MB-01"))
                .ram(inventory.findBySku("PH-1001"), 1)
                .addDrive(inventory.findBySku("PH-3002"))
                .psu(inventory.findBySku("NT-PSU-01"))
                .pcCase(inventory.findBySku("PH-4001"));
    }

    private PCBuilder gaming() {
        return new PCBuilder()
                .name(Preset.GAMING.toString())
                .cpu(inventory.findBySku("NT-CPU-02"))
                .motherboard(inventory.findBySku("NT-MB-01"))
                .ram(inventory.findBySku("PH-1001"), 2)
                .gpu(inventory.findBySku("PH-2002"))
                .addDrive(inventory.findBySku("PH-3001"))
                .psu(inventory.findBySku("NT-PSU-02"))
                .pcCase(inventory.findBySku("PH-4001"));
    }

    private PCBuilder workstation() {
        return new PCBuilder()
                .name(Preset.WORKSTATION.toString())
                .cpu(inventory.findBySku("NT-CPU-04"))
                .motherboard(inventory.findBySku("NT-MB-03"))
                .ram(inventory.findBySku("PH-1002"), 2)
                .gpu(inventory.findBySku("PH-2003"))
                .addDrive(inventory.findBySku("PH-3001"))
                .addDrive(inventory.findBySku("PH-3003"))
                .psu(inventory.findBySku("NT-PSU-03"))
                .pcCase(inventory.findBySku("PH-4001"));
    }
}
