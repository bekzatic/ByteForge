package com.byteforge.decorator;

import java.util.ArrayList;
import java.util.List;

/**
 * DECORATOR pattern - the abstract <b>Decorator</b>.
 * <p>
 * Holds a reference to another {@link PCProduct} and adds its own description and
 * price on top. Because a decorator is itself a PCProduct, add-ons can be stacked
 * in any combination at runtime without a subclass per combination.
 */
public abstract class AddOnDecorator implements PCProduct {

    protected final PCProduct wrapped;

    protected AddOnDecorator(PCProduct wrapped) {
        this.wrapped = wrapped;
    }

    protected abstract String addOnName();

    protected abstract double addOnPrice();

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + " + addOnName();
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice() + addOnPrice();
    }

    @Override
    public List<String> getLineItems() {
        List<String> lines = new ArrayList<>(wrapped.getLineItems());
        lines.add(String.format("%-38s %10.2f", addOnName(), addOnPrice()));
        return lines;
    }
}
