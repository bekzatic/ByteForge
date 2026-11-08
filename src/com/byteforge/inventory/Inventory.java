package com.byteforge.inventory;

import com.byteforge.adapter.SupplierFeed;
import com.byteforge.factory.ComponentFactory;
import com.byteforge.factory.ComponentRecord;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.observer.StockObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * SINGLETON pattern (+ subject of the OBSERVER pattern for stock events).
 * <p>
 * There is exactly one stock ledger for the whole store: the GUI, the checkout facade
 * and the dashboards must all see the same quantities, otherwise two customers could
 * buy the last GPU. The instance is created lazily and thread-safely using the
 * initialization-on-demand holder idiom; the constructor is private.
 */
public final class Inventory {

    public static final int LOW_STOCK_THRESHOLD = 2;

    private final Map<String, Component> components = new LinkedHashMap<>();
    private final Map<String, Integer> stock = new LinkedHashMap<>();
    private final List<StockObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> loadedSuppliers = new ArrayList<>();

    private Inventory() {
        // private: use getInstance()
    }

    private static final class Holder {
        private static final Inventory INSTANCE = new Inventory();
    }

    public static Inventory getInstance() {
        return Holder.INSTANCE;
    }

    // ---------------------------------------------------------------- loading

    /** Pulls records through any supplier adapter and turns them into parts via the factory. */
    public synchronized int importFrom(SupplierFeed feed, ComponentFactory factory) {
        int count = 0;
        for (ComponentRecord record : feed.fetchRecords()) {
            Component c = factory.create(record);
            components.put(c.getSku(), c);
            stock.merge(c.getSku(), record.getStock(), Integer::sum);
            count++;
        }
        loadedSuppliers.add(feed.getSupplierName() + " - " + count + " items");
        return count;
    }

    /** Only used by tests/demo to start from a clean state. */
    public synchronized void clear() {
        components.clear();
        stock.clear();
        loadedSuppliers.clear();
    }

    // ---------------------------------------------------------------- queries

    public synchronized List<Component> byType(ComponentType type) {
        return components.values().stream()
                .filter(c -> c.getType() == type)
                .sorted(Comparator.comparingDouble(Component::getPrice))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T findBySku(String sku) {
        Component c = components.get(sku);
        if (c == null) {
            throw new IllegalArgumentException("Unknown SKU " + sku);
        }
        return (T) c;
    }

    public synchronized List<Component> all() {
        return new ArrayList<>(components.values());
    }

    public synchronized int getStock(Component c) {
        return stock.getOrDefault(c.getSku(), 0);
    }

    public synchronized List<String> getLoadedSuppliers() {
        return new ArrayList<>(loadedSuppliers);
    }

    // ---------------------------------------------------------------- changes

    /**
     * Reserves the given parts atomically: either every unit is taken from stock or,
     * if anything is short, nothing changes. A part may appear several times.
     */
    public synchronized void reserve(List<Component> parts) {
        Map<Component, Integer> needed = new LinkedHashMap<>();
        for (Component c : parts) {
            needed.merge(c, 1, Integer::sum);
        }
        for (Map.Entry<Component, Integer> e : needed.entrySet()) {
            if (getStock(e.getKey()) < e.getValue()) {
                throw new IllegalStateException("Not enough stock for " + e.getKey().getName()
                        + " (need " + e.getValue() + ", have " + getStock(e.getKey()) + ")");
            }
        }
        for (Map.Entry<Component, Integer> e : needed.entrySet()) {
            changeStock(e.getKey(), -e.getValue());
        }
    }

    public synchronized void restock(Component c, int qty) {
        changeStock(c, qty);
    }

    private void changeStock(Component c, int delta) {
        int old = getStock(c);
        int now = old + delta;
        stock.put(c.getSku(), now);
        for (StockObserver o : observers) {
            o.onStockChanged(c, old, now);
        }
    }

    // ---------------------------------------------------------------- observers

    public void addObserver(StockObserver o) {
        observers.add(o);
    }

    public void removeObserver(StockObserver o) {
        observers.remove(o);
    }
}
