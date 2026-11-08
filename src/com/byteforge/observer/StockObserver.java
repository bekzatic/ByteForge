package com.byteforge.observer;

import com.byteforge.model.Component;

/**
 * OBSERVER pattern - observer interface for inventory events.
 * The {@link com.byteforge.inventory.Inventory} is the subject.
 */
public interface StockObserver {
    void onStockChanged(Component component, int oldQty, int newQty);
}
