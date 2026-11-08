package com.byteforge.factory;

import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.model.Parts;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * FACTORY pattern.
 * <p>
 * The only place that knows which concrete {@link Component} subclass matches a
 * {@link ComponentType} and how to read its type-specific attributes. Client code
 * (the catalog loader, tests) asks for a component by record and gets back the
 * abstract {@link Component}; it never calls a concrete constructor.
 * New part types are supported by registering one more creator function.
 */
public final class ComponentFactory {

    private final Map<ComponentType, Function<ComponentRecord, Component>> creators =
            new EnumMap<>(ComponentType.class);

    public ComponentFactory() {
        creators.put(ComponentType.CPU, r -> new Parts.Cpu(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(), r.intAttr("tdp"),
                r.attr("socket"), r.intAttr("cores"), r.doubleAttr("ghz")));

        creators.put(ComponentType.MOTHERBOARD, r -> new Parts.Motherboard(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(), 50,
                r.attr("socket"), r.attr("ramType"), r.attr("formFactor")));

        creators.put(ComponentType.RAM, r -> new Parts.Ram(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(), 10,
                r.attr("ramType"), r.intAttr("capacityGb"), r.intAttr("speedMhz")));

        creators.put(ComponentType.GPU, r -> new Parts.Gpu(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(), r.intAttr("tdp"),
                r.intAttr("vramGb"), r.intAttr("lengthMm")));

        creators.put(ComponentType.STORAGE, r -> new Parts.Storage(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(), 8,
                r.attr("kind"), r.intAttr("capacityGb")));

        creators.put(ComponentType.PSU, r -> new Parts.PowerSupply(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(),
                r.intAttr("watts"), r.attr("rating")));

        creators.put(ComponentType.CASE, r -> new Parts.PcCase(
                r.getSku(), r.getBrand(), r.getModel(), r.getPrice(),
                r.attr("formFactor"), r.intAttr("maxGpuLengthMm")));
    }

    public Component create(ComponentRecord record) {
        Function<ComponentRecord, Component> creator = creators.get(record.getType());
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for " + record.getType());
        }
        return creator.apply(record);
    }
}
