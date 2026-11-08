package com.byteforge.strategy;

/**
 * STRATEGY pattern - the <b>Strategy</b> interface.
 * <p>
 * Encapsulates one way of turning a subtotal into a final price. The checkout
 * (the context) holds a PricingStrategy and can swap it at runtime without any
 * if/else chain over customer types.
 */
public interface PricingStrategy {
    String getName();

    /** Discount amount (positive number) for {@code quantity} units costing {@code unitPrice}. */
    double discount(double unitPrice, int quantity);

    default double total(double unitPrice, int quantity) {
        double subtotal = unitPrice * quantity;
        return Math.round((subtotal - discount(unitPrice, quantity)) * 100) / 100.0;
    }
}
