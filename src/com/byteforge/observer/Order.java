package com.byteforge.observer;

import com.byteforge.decorator.PCProduct;
import com.byteforge.strategy.PricingStrategy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OBSERVER pattern - the <b>Subject</b>.
 * <p>
 * An order does not know who cares about it. Customer e-mails, the assembly line and
 * the sales dashboard register themselves and are pushed every status change, so new
 * listeners (SMS, analytics, ...) can be added without touching this class.
 */
public class Order {
    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private final String id;
    private final String customer;
    private final PCProduct product;
    private final int quantity;
    private final PricingStrategy pricing;
    private final double total;
    private OrderStatus status = OrderStatus.PLACED;
    private final List<OrderObserver> observers = new CopyOnWriteArrayList<>();

    public Order(String customer, PCProduct product, int quantity, PricingStrategy pricing) {
        this.id = "BF-" + SEQ.incrementAndGet();
        this.customer = customer;
        this.product = product;
        this.quantity = quantity;
        this.pricing = pricing;
        this.total = pricing.total(product.getPrice(), quantity);
    }

    public void attach(OrderObserver o) { observers.add(o); }
    public void detach(OrderObserver o) { observers.remove(o); }

    public void setStatus(OrderStatus newStatus) {
        OrderStatus old = this.status;
        if (old == newStatus) {
            return;
        }
        this.status = newStatus;
        notifyObservers(old, newStatus);
    }

    public void advance() {
        setStatus(status.next());
    }

    private void notifyObservers(OrderStatus old, OrderStatus now) {
        for (OrderObserver o : observers) {
            o.onStatusChanged(this, old, now);
        }
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public PCProduct getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public PricingStrategy getPricing() { return pricing; }
    public double getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
}
