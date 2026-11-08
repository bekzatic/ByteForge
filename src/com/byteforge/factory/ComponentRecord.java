package com.byteforge.factory;

import com.byteforge.model.ComponentType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Neutral, format-independent description of a part as it arrives from a supplier.
 * Adapters turn supplier data into records; the factory turns records into parts.
 */
public class ComponentRecord {
    private final String sku;
    private final ComponentType type;
    private final String brand;
    private final String model;
    private final double price;
    private final int stock;
    private final Map<String, String> attributes;

    public ComponentRecord(String sku, ComponentType type, String brand, String model,
                           double price, int stock, Map<String, String> attributes) {
        this.sku = sku;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.stock = stock;
        this.attributes = new LinkedHashMap<>(attributes);
    }

    public String getSku() { return sku; }
    public ComponentType getType() { return type; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }

    public String attr(String key) {
        String value = attributes.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Record " + sku + " is missing attribute '" + key + "'");
        }
        return value;
    }

    public int intAttr(String key) {
        return Integer.parseInt(attr(key).trim());
    }

    public double doubleAttr(String key) {
        return Double.parseDouble(attr(key).trim());
    }
}
