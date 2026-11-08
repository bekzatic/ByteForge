package com.byteforge.facade;

import com.byteforge.adapter.NovaTechCsvAdapter;
import com.byteforge.adapter.NovaTechCsvService;
import com.byteforge.adapter.PartHubApiAdapter;
import com.byteforge.adapter.PartHubApiClient;
import com.byteforge.adapter.SupplierFeed;
import com.byteforge.builder.BuildDirector;
import com.byteforge.builder.Computer;
import com.byteforge.builder.PCBuilder;
import com.byteforge.decorator.AddOns;
import com.byteforge.decorator.BasePC;
import com.byteforge.decorator.PCProduct;
import com.byteforge.factory.ComponentFactory;
import com.byteforge.inventory.Inventory;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.observer.Listeners;
import com.byteforge.observer.Order;
import com.byteforge.observer.OrderObserver;
import com.byteforge.strategy.PricingStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * FACADE pattern.
 * <p>
 * One simple entry point for the user interface. Behind it, checkout touches seven
 * subsystems: supplier adapters, the component factory, the inventory singleton, the
 * builder/director, add-on decorators, pricing strategies, the payment gateway and the
 * order observers. The GUI and the console demo only ever talk to this class.
 */
public class StoreFacade {

    private final Inventory inventory = Inventory.getInstance();
    private final ComponentFactory factory = new ComponentFactory();
    private final BuildDirector director = new BuildDirector(inventory);
    private final PaymentGateway payments = new PaymentGateway();
    private final List<Order> orders = new ArrayList<>();
    private final List<OrderObserver> orderObservers;
    private final Listeners.SalesDashboard dashboard;
    private final Consumer<String> log;

    public StoreFacade(Consumer<String> log) {
        this.log = log;
        this.dashboard = new Listeners.SalesDashboard(log);
        this.orderObservers = Arrays.asList(
                new Listeners.CustomerNotifier(log),
                new Listeners.AssemblyDepartment(log),
                dashboard);
        this.lowStockAlert = new Listeners.LowStockAlert(log);
        inventory.addObserver(lowStockAlert);
    }

    private final Listeners.LowStockAlert lowStockAlert;

    /** Detaches this facade's listener from the shared inventory (used when a facade is discarded). */
    public void shutdown() {
        inventory.removeObserver(lowStockAlert);
    }

    // ------------------------------------------------------------ catalog

    /** Imports the catalog from every supplier through its adapter. */
    public void loadCatalog() {
        inventory.clear();
        List<SupplierFeed> feeds = Arrays.asList(
                new NovaTechCsvAdapter(new NovaTechCsvService()),
                new PartHubApiAdapter(new PartHubApiClient()));
        for (SupplierFeed feed : feeds) {
            int n = inventory.importFrom(feed, factory);
            log.accept("[Catalog] Imported " + n + " parts from " + feed.getSupplierName());
        }
    }

    public List<Component> getParts(ComponentType type) {
        return inventory.byType(type);
    }

    public int getStock(Component c) {
        return inventory.getStock(c);
    }

    public List<Component> getAllParts() {
        return inventory.all();
    }

    /** Purchasing receives a delivery from a supplier. */
    public void restock(Component c, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        inventory.restock(c, quantity);
        log.accept(String.format("[Purchasing] Restocked %d x %s - now %d in stock.",
                quantity, c.getName(), inventory.getStock(c)));
    }

    // ------------------------------------------------------------ configuring

    public PCBuilder newBuild() {
        return new PCBuilder();
    }

    public PCBuilder preset(BuildDirector.Preset preset) {
        return director.construct(preset);
    }

    /** Wraps the computer in one decorator per selected add-on. */
    public PCProduct decorate(Computer computer, Set<AddOnOption> addOns) {
        PCProduct product = new BasePC(computer);
        if (addOns.contains(AddOnOption.OS_INSTALL)) product = new AddOns.OsInstallation(product);
        if (addOns.contains(AddOnOption.RGB))        product = new AddOns.RgbLighting(product);
        if (addOns.contains(AddOnOption.OVERCLOCK))  product = new AddOns.Overclocking(product);
        if (addOns.contains(AddOnOption.WARRANTY))   product = new AddOns.ExtendedWarranty(product, 2);
        return product;
    }

    public Quote quote(Computer computer, Set<AddOnOption> addOns, PricingStrategy pricing, int quantity) {
        PCProduct product = decorate(computer, addOns);
        double subtotal = product.getPrice() * quantity;
        double discount = pricing.discount(product.getPrice(), quantity);
        return new Quote(product, quantity, pricing, subtotal, discount, pricing.total(product.getPrice(), quantity));
    }

    // ------------------------------------------------------------ checkout

    /**
     * The whole checkout in one call:
     * decorate -> price -> authorize card -> reserve stock -> capture payment
     * -> create order -> attach observers -> mark paid.
     * A declined card never touches the inventory; if stock is short the card hold is voided.
     */
    public Order checkout(String customer, String cardLast4, Computer computer,
                          Set<AddOnOption> addOns, PricingStrategy pricing, int quantity) {
        if (customer == null || customer.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        Quote q = quote(computer, addOns, pricing, quantity);

        String auth;
        try {
            auth = payments.authorize(customer, cardLast4, q.getTotal());
        } catch (RuntimeException e) {
            log.accept("[Checkout] Payment declined for " + customer + ": " + e.getMessage());
            throw e;
        }

        List<Component> units = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            units.addAll(computer.allParts());
        }
        try {
            inventory.reserve(units);
        } catch (RuntimeException e) {
            payments.voidAuthorization(auth);
            log.accept("[Checkout] " + e.getMessage() + " - card hold released.");
            throw e;
        }
        String tx = payments.capture(auth);

        Order order = new Order(customer, q.getProduct(), quantity, pricing);
        order.setPaymentId(tx);
        orderObservers.forEach(order::attach);
        orders.add(order);
        reservedUnits.put(order, units);
        log.accept(String.format("[Checkout] %s placed by %s - %s, total $%,.2f (payment %s)",
                order.getId(), customer, pricing.getName(), order.getTotal(), tx));
        order.advance(); // PLACED -> PAID (State pattern decides the next step)
        return order;
    }

    public void advanceOrder(Order order) {
        order.advance();
    }

    /**
     * Cancels an order if its current state allows it (State pattern), then refunds the
     * payment and puts every reserved part back into the inventory.
     */
    public void cancelOrder(Order order, String reason) {
        order.cancel(reason); // throws IllegalStateException if the state forbids it
        payments.refund(order.getPaymentId(), order.getTotal());
        for (Component c : reservedUnits.getOrDefault(order, Collections.emptyList())) {
            inventory.restock(c, 1);
        }
        reservedUnits.remove(order);
        log.accept(String.format("[Checkout] %s cancelled (%s): $%,.2f refunded, parts returned to stock.",
                order.getId(), order.getCancelReason(), order.getTotal()));
    }

    private final Map<Order, List<Component>> reservedUnits = new HashMap<>();

    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public PaymentGateway getPaymentGateway() {
        return payments;
    }

    public Listeners.SalesDashboard getDashboard() {
        return dashboard;
    }

    // ------------------------------------------------------------ value object

    public static class Quote {
        private final PCProduct product;
        private final int quantity;
        private final PricingStrategy pricing;
        private final double subtotal;
        private final double discount;
        private final double total;

        Quote(PCProduct product, int quantity, PricingStrategy pricing,
              double subtotal, double discount, double total) {
            this.product = product;
            this.quantity = quantity;
            this.pricing = pricing;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }

        public PCProduct getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public PricingStrategy getPricing() { return pricing; }
        public double getSubtotal() { return subtotal; }
        public double getDiscount() { return discount; }
        public double getTotal() { return total; }

        public String receipt() {
            StringBuilder sb = new StringBuilder();
            for (String line : product.getLineItems()) {
                sb.append(line).append('\n');
            }
            sb.append(String.format("%-38s %10.2f%n", "Price per PC", product.getPrice()));
            sb.append(String.format("%-38s %10d%n", "Quantity", quantity));
            sb.append(String.format("%-38s %10.2f%n", "Subtotal", subtotal));
            sb.append(String.format("%-38s%n", "Pricing: " + pricing.getName()));
            sb.append(String.format("%-38s %10.2f%n", "Discount", -discount));
            sb.append(String.format("%-38s %10.2f%n", "TOTAL", total));
            return sb.toString();
        }
    }
}
