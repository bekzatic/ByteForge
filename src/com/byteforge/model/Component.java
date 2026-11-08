package com.byteforge.model;

/**
 * Base class of every hardware part sold in the store.
 * Concrete parts are created only through {@link com.byteforge.factory.ComponentFactory}.
 */
public abstract class Component {
    private final String sku;
    private final String brand;
    private final String model;
    private final double price;
    private final int powerDraw; // watts

    protected Component(String sku, String brand, String model, double price, int powerDraw) {
        this.sku = sku;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.powerDraw = powerDraw;
    }

    public abstract ComponentType getType();

    /** Short human-readable list of the technical specs. */
    public abstract String getSpecs();

    public String getSku() { return sku; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public double getPrice() { return price; }
    public int getPowerDraw() { return powerDraw; }

    public String getName() {
        return brand + " " + model;
    }

    @Override
    public String toString() {
        return String.format("%s  ($%.2f)", getName(), price);
    }
}
