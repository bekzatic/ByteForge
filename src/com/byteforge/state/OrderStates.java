package com.byteforge.state;

import com.byteforge.observer.Order;
import com.byteforge.observer.OrderStatus;

/**
 * STATE pattern - the <b>Concrete States</b>.
 * <pre>
 *  PLACED -> PAID -> ASSEMBLING -> TESTING -> SHIPPED -> DELIVERED
 *     \        \
 *      +--------+--> CANCELLED      (only before assembly has started)
 * </pre>
 * States hold no data, so each one is a shared constant.
 */
public final class OrderStates {
    private OrderStates() {}

    public static final OrderState PLACED = new Placed();
    public static final OrderState PAID = new Paid();
    public static final OrderState ASSEMBLING = new Assembling();
    public static final OrderState TESTING = new Testing();
    public static final OrderState SHIPPED = new Shipped();
    public static final OrderState DELIVERED = new Delivered();
    public static final OrderState CANCELLED = new Cancelled();

    /** Common behaviour: a state that does not allow cancelling explains why. */
    private abstract static class Base implements OrderState {
        @Override public boolean canCancel() { return false; }

        @Override public void cancel(Order order, String reason) {
            throw new IllegalStateException("Order " + order.getId() + " cannot be cancelled: " + whyNoCancel());
        }

        protected String whyNoCancel() {
            return "it is already " + getStatus();
        }

        @Override public String toString() { return getStatus().name(); }
    }

    /** Base for states where the customer may still change their mind. */
    private abstract static class Cancellable extends Base {
        @Override public boolean canCancel() { return true; }

        @Override public void cancel(Order order, String reason) {
            order.cancelWith(reason);
            order.changeState(CANCELLED);
        }
    }

    static final class Placed extends Cancellable {
        @Override public OrderStatus getStatus() { return OrderStatus.PLACED; }
        @Override public void advance(Order order) { order.changeState(PAID); }
        @Override public String hint() { return "Waiting for payment. Can be cancelled."; }
    }

    static final class Paid extends Cancellable {
        @Override public OrderStatus getStatus() { return OrderStatus.PAID; }
        @Override public void advance(Order order) { order.changeState(ASSEMBLING); }
        @Override public String hint() { return "Paid, waiting in the workshop queue. Can still be cancelled with a full refund."; }
    }

    static final class Assembling extends Base {
        @Override public OrderStatus getStatus() { return OrderStatus.ASSEMBLING; }
        @Override public void advance(Order order) { order.changeState(TESTING); }
        @Override protected String whyNoCancel() { return "the PC is already being assembled"; }
        @Override public String hint() { return "On the workbench. Cancellation is no longer possible."; }
    }

    static final class Testing extends Base {
        @Override public OrderStatus getStatus() { return OrderStatus.TESTING; }
        @Override public void advance(Order order) { order.changeState(SHIPPED); }
        @Override protected String whyNoCancel() { return "the PC is already assembled"; }
        @Override public String hint() { return "4-hour stress test running."; }
    }

    static final class Shipped extends Base {
        @Override public OrderStatus getStatus() { return OrderStatus.SHIPPED; }
        @Override public void advance(Order order) { order.changeState(DELIVERED); }
        @Override protected String whyNoCancel() { return "it has been shipped (use a return instead)"; }
        @Override public String hint() { return "With the courier."; }
    }

    /** Final states reject any further step. */
    private abstract static class Final extends Base {
        @Override public void advance(Order order) {
            throw new IllegalStateException("Order " + order.getId() + " is already " + getStatus());
        }
    }

    static final class Delivered extends Final {
        @Override public OrderStatus getStatus() { return OrderStatus.DELIVERED; }
        @Override public String hint() { return "Delivered. Order complete."; }
    }

    static final class Cancelled extends Final {
        @Override public OrderStatus getStatus() { return OrderStatus.CANCELLED; }
        @Override public String hint() { return "Cancelled, money refunded and parts returned to stock."; }
    }
}
