package com.byteforge.observer;

import com.byteforge.inventory.Inventory;
import com.byteforge.model.Component;

import java.util.function.Consumer;

/**
 * OBSERVER pattern - the <b>Concrete Observers</b>.
 * Each writes to an event log sink (console or the GUI log panel).
 */
public final class Listeners {
    private Listeners() {}

    /** Simulates e-mailing the customer on every step. */
    public static class CustomerNotifier implements OrderObserver {
        private final Consumer<String> log;

        public CustomerNotifier(Consumer<String> log) { this.log = log; }

        @Override
        public void onStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
            log.accept(String.format("[Email -> %s] Your order %s is now %s.",
                    order.getCustomer(), order.getId(), newStatus));
        }
    }

    /** The workshop only cares about paid orders and finished tests. */
    public static class AssemblyDepartment implements OrderObserver {
        private final Consumer<String> log;

        public AssemblyDepartment(Consumer<String> log) { this.log = log; }

        @Override
        public void onStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
            if (newStatus == OrderStatus.PAID) {
                log.accept(String.format("[Workshop] Queued %d x '%s' for assembly (%s).",
                        order.getQuantity(), order.getProduct().getDescription(), order.getId()));
            } else if (newStatus == OrderStatus.TESTING) {
                log.accept("[Workshop] Running 4h stress test on " + order.getId() + ".");
            }
        }
    }

    /** Live revenue and order counters for management. */
    public static class SalesDashboard implements OrderObserver {
        private final Consumer<String> log;
        private int paidOrders;
        private int delivered;
        private double revenue;

        public SalesDashboard(Consumer<String> log) { this.log = log; }

        @Override
        public synchronized void onStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
            if (newStatus == OrderStatus.PAID) {
                paidOrders++;
                revenue += order.getTotal();
                log.accept(String.format("[Dashboard] Orders paid: %d | Revenue: $%,.2f", paidOrders, revenue));
            } else if (newStatus == OrderStatus.DELIVERED) {
                delivered++;
                log.accept(String.format("[Dashboard] Delivered orders: %d", delivered));
            }
        }

        public synchronized int getPaidOrders() { return paidOrders; }
        public synchronized int getDelivered() { return delivered; }
        public synchronized double getRevenue() { return revenue; }
    }

    /** Warns purchasing when a part runs low (observer of the Inventory singleton). */
    public static class LowStockAlert implements StockObserver {
        private final Consumer<String> log;

        public LowStockAlert(Consumer<String> log) { this.log = log; }

        @Override
        public void onStockChanged(Component c, int oldQty, int newQty) {
            if (newQty <= Inventory.LOW_STOCK_THRESHOLD && newQty < oldQty) {
                log.accept(String.format("[Purchasing] LOW STOCK: %s - only %d left, reorder from supplier.",
                        c.getName(), newQty));
            }
        }
    }
}
