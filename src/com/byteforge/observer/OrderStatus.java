package com.byteforge.observer;

/**
 * Label of each step in the order life cycle. The behaviour of each step lives in
 * the matching {@link com.byteforge.state.OrderState} class (State pattern);
 * this enum is only what observers and the GUI display.
 */
public enum OrderStatus {
    PLACED, PAID, ASSEMBLING, TESTING, SHIPPED, DELIVERED, CANCELLED;

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
