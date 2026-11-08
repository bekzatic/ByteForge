package com.byteforge.decorator;

import com.byteforge.builder.Computer;

import java.util.ArrayList;
import java.util.List;

/**
 * DECORATOR pattern - the <b>Concrete Component</b>: the assembled hardware itself.
 */
public class BasePC implements PCProduct {

    public static final double ASSEMBLY_FEE = 59.00;

    private final Computer computer;

    public BasePC(Computer computer) {
        this.computer = computer;
    }

    public Computer getComputer() {
        return computer;
    }

    @Override
    public String getDescription() {
        return computer.getName();
    }

    @Override
    public double getPrice() {
        return computer.getHardwarePrice() + ASSEMBLY_FEE;
    }

    @Override
    public List<String> getLineItems() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("%-38s %10.2f", "Hardware (" + computer.allParts().size() + " parts)",
                computer.getHardwarePrice()));
        lines.add(String.format("%-38s %10.2f", "Assembly & stress test", ASSEMBLY_FEE));
        return lines;
    }
}
