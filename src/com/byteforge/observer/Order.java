package com.byteforge.observer;

import com.byteforge.decorator.PCProduct;
import com.byteforge.state.OrderState;
import com.byteforge.state.OrderStates;
import com.byteforge.strategy.PricingStrategy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OBSERVER pattern - the <b>Subject</b>, and STATE pattern - the <b>Context</b>.
 * <p>
 * Observer: an order does not know who cares about it. Customer e-mails, the assembly
 * line and the sales dashboard register themselves and are pushed every status change.
 * <p>
 * State: the order delegates "what happens next" and "may I be cancelled?" to its
 * current {@link OrderState} object instead of checking its status in if/else chains.
 */
public class Order {
    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private final String id;
    private final String customer;
    private final PCProduct product;
    private final int quantity;
    private final PricingStrategy pricing;
    private final double total;
    private OrderState state = OrderStates.PLACED;
    private String paymentId;
    private String cancelReason;
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

    // ------------------------------------------------------------ State pattern: delegation

    /** Moves to the next step of the life cycle (the current state decides which). */
    public void advance() {
        state.advance(this);
    }

    /** Cancels the order if the current state allows it, otherwise throws IllegalStateException. */
    public void cancel(String reason) {
        state.cancel(this, reason);
    }

    public boolean canCancel() {
        return state.canCancel();
    }

    public String getHint() {
        return state.hint();
    }

    /** Called only by state objects. Switches state and notifies the observers. */
    public void changeState(OrderState next) {
        OrderStatus old = state.getStatus();
        state = next;
        for (OrderObserver o : observers) {
            o.onStatusChanged(this, old, next.getStatus());
        }
    }

    /** Called by a state object right before it switches to CANCELLED. */
    public void cancelWith(String reason) {
        this.cancelReason = (reason == null || reason.trim().isEmpty()) ? "cancelled by customer" : reason.trim();
    }

    // ------------------------------------------------------------ data

    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPaymentId() { return paymentId; }
    public String getCancelReason() { return cancelReason; }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public PCProduct getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public PricingStrategy getPricing() { return pricing; }
    public double getTotal() { return total; }
    public OrderStatus getStatus() { return state.getStatus(); }
}
