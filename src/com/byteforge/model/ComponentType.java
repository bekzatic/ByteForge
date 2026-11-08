package com.byteforge.model;

/** All hardware categories a PC build is made of. */
public enum ComponentType {
    CPU("Processor"),
    MOTHERBOARD("Motherboard"),
    RAM("Memory"),
    GPU("Graphics Card"),
    STORAGE("Storage"),
    PSU("Power Supply"),
    CASE("Case");

    private final String label;

    ComponentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
