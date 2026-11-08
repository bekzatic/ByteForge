package com.byteforge.observer;

/**
 * OBSERVER pattern - the <b>Observer</b> interface for order events.
 * {@link Order} (the subject) calls this on every status change.
 */
public interface OrderObserver {
    void onStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus);
}
