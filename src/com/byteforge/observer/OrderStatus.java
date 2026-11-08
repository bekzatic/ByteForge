package com.byteforge.observer;

/** Life cycle of a custom PC order. */
public enum OrderStatus {
    PLACED, PAID, ASSEMBLING, TESTING, SHIPPED, DELIVERED;

    public OrderStatus next() {
        return this == DELIVERED ? DELIVERED : values()[ordinal() + 1];
    }

    public boolean isFinal() {
        return this == DELIVERED;
    }
}
