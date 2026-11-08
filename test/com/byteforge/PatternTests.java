package com.byteforge;

import com.byteforge.adapter.NovaTechCsvAdapter;
import com.byteforge.adapter.NovaTechCsvService;
import com.byteforge.adapter.PartHubApiAdapter;
import com.byteforge.adapter.PartHubApiClient;
import com.byteforge.builder.BuildDirector;
import com.byteforge.builder.Computer;
import com.byteforge.builder.IncompatibleBuildException;
import com.byteforge.builder.PCBuilder;
import com.byteforge.command.CommandHistory;
import com.byteforge.command.ConfigCommands;
import com.byteforge.command.ConfigurationEditor;
import com.byteforge.decorator.AddOns;
import com.byteforge.decorator.BasePC;
import com.byteforge.decorator.PCProduct;
import com.byteforge.facade.AddOnOption;
import com.byteforge.facade.PaymentGateway;
import com.byteforge.facade.StoreFacade;
import com.byteforge.factory.ComponentFactory;
import com.byteforge.factory.ComponentRecord;
import com.byteforge.inventory.Inventory;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.model.Parts;
import com.byteforge.observer.Order;
import com.byteforge.rules.CompatibilityRule;
import com.byteforge.rules.CompatibilityRules;
import com.byteforge.observer.OrderObserver;
import com.byteforge.observer.OrderStatus;
import com.byteforge.strategy.PricingStrategies;
import com.byteforge.strategy.PricingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated tests for all nine patterns. Dependency-free (no JUnit jar needed):
 * {@code run.bat --test}. Exit code is 1 if any test fails.
 */
public final class PatternTests {

    // ------------------------------------------------------------------ mini framework

    private interface Body { void run() throws Exception; }

    private static int passed;
    private static final List<String> failures = new ArrayList<>();
    private static StoreFacade store;
    private static final List<String> log = Collections.synchronizedList(new ArrayList<>());

    private static void test(String pattern, String name, Body body) {
        log.clear();
        store = new StoreFacade(log::add);
        store.loadCatalog();
        try {
            body.run();
            passed++;
            System.out.printf("  PASS  %-10s %s%n", pattern, name);
        } catch (Throwable t) {
            failures.add(pattern + " - " + name + ": " + t);
            System.out.printf("  FAIL  %-10s %s%n        -> %s%n", pattern, name, t);
        } finally {
            store.shutdown();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void near(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.005) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, Body body) throws Exception {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    private static <T extends Component> T sku(String s) {
        return Inventory.getInstance().findBySku(s);
    }

    private static Computer preset(BuildDirector.Preset p) {
        return store.preset(p).build();
    }

    // ------------------------------------------------------------------ tests

    public static void main(String[] args) {
        java.util.Locale.setDefault(java.util.Locale.US);
        System.out.println("ByteForge pattern tests");
        System.out.println("=======================");

        // ---- Singleton
        test("Singleton", "getInstance() always returns the same object", () ->
                check(Inventory.getInstance() == Inventory.getInstance(), "different instances"));
        test("Singleton", "constructor is private", () ->
                check(java.lang.reflect.Modifier.isPrivate(Inventory.class.getDeclaredConstructor().getModifiers()),
                        "constructor is not private"));
        test("Singleton", "reserve() is all-or-nothing", () -> {
            Parts.Gpu rtx4090 = sku("PH-2003");      // 2 in stock
            Parts.Cpu ryzen = sku("NT-CPU-01");      // 12 in stock
            int before = Inventory.getInstance().getStock(ryzen);
            List<Component> units = new ArrayList<>();
            units.add(ryzen);
            for (int i = 0; i < 3; i++) units.add(rtx4090);
            expect(IllegalStateException.class, () -> Inventory.getInstance().reserve(units));
            eq(before, Inventory.getInstance().getStock(ryzen));
            eq(2, Inventory.getInstance().getStock(rtx4090));
        });

        // ---- Factory
        test("Factory", "creates the right subclass for each type", () -> {
            Map<String, String> a = new LinkedHashMap<>();
            a.put("vramGb", "12"); a.put("lengthMm", "300"); a.put("tdp", "200");
            Component c = new ComponentFactory().create(
                    new ComponentRecord("X-1", ComponentType.GPU, "Test", "Card", 100, 1, a));
            check(c instanceof Parts.Gpu, "not a Gpu: " + c.getClass());
            eq(300, ((Parts.Gpu) c).getLengthMm());
        });
        test("Factory", "missing attribute is reported clearly", () -> {
            IllegalArgumentException e = expect(IllegalArgumentException.class, () -> new ComponentFactory().create(
                    new ComponentRecord("X-2", ComponentType.CPU, "Test", "Chip", 100, 1, new LinkedHashMap<>())));
            check(e.getMessage().contains("missing attribute"), e.getMessage());
        });

        // ---- Adapter
        test("Adapter", "CSV supplier: 11 records with parsed attributes", () -> {
            List<ComponentRecord> r = new NovaTechCsvAdapter(new NovaTechCsvService()).fetchRecords();
            eq(11, r.size());
            eq("AM5", r.get(0).attr("socket"));
        });
        test("Adapter", "JSON supplier: categories mapped, cents converted", () -> {
            List<ComponentRecord> r = new PartHubApiAdapter(new PartHubApiClient()).fetchRecords();
            eq(12, r.size());
            ComponentRecord gpu = r.stream().filter(x -> x.getSku().equals("PH-2001")).findFirst().get();
            eq(ComponentType.GPU, gpu.getType());
            near(299.99, gpu.getPrice());
            eq("115", gpu.attr("tdp")); // "boardPower" renamed
        });
        test("Adapter", "catalog contains 23 parts from both suppliers", () ->
                eq(23, Inventory.getInstance().all().size()));

        // ---- Builder
        test("Builder", "all three presets are valid", () -> {
            for (BuildDirector.Preset p : BuildDirector.Preset.values()) {
                Computer c = preset(p);
                eq(p.toString(), c.getName());
            }
        });
        test("Builder", "socket mismatch is rejected", () -> {
            PCBuilder b = store.preset(BuildDirector.Preset.GAMING).cpu(sku("NT-CPU-03"));
            IncompatibleBuildException e = expect(IncompatibleBuildException.class, b::build);
            check(e.getIssues().get(0).contains("socket"), e.getIssues().toString());
        });
        test("Builder", "DDR4 on a DDR5 board is rejected", () -> {
            List<String> issues = store.preset(BuildDirector.Preset.GAMING).ram(sku("PH-1003"), 2).validate();
            check(issues.size() == 1 && issues.get(0).contains("DDR4"), issues.toString());
        });
        test("Builder", "GPU longer than the case is rejected", () -> {
            List<String> issues = store.preset(BuildDirector.Preset.WORKSTATION).pcCase(sku("PH-4003")).validate();
            check(issues.stream().anyMatch(i -> i.contains("336 mm")), issues.toString());
        });
        test("Builder", "weak PSU is rejected with a recommendation", () -> {
            List<String> issues = store.preset(BuildDirector.Preset.WORKSTATION).psu(sku("NT-PSU-02")).validate();
            check(issues.stream().anyMatch(i -> i.contains("too weak")), issues.toString());
        });
        test("Builder", "F-series CPU without GPU is rejected", () -> {
            List<String> issues = store.newBuild().cpu(sku("NT-CPU-03")).motherboard(sku("NT-MB-02"))
                    .ram(sku("PH-1003"), 2).addDrive(sku("PH-3002")).psu(sku("NT-PSU-01"))
                    .pcCase(sku("PH-4001")).validate();
            check(issues.size() == 1 && issues.get(0).contains("integrated graphics"), issues.toString());
        });
        test("Builder", "incomplete build lists every missing part", () ->
                eq(6, store.newBuild().validate().size()));
        test("Builder", "built Computer is immutable", () -> {
            Computer c = preset(BuildDirector.Preset.WORKSTATION);
            expect(UnsupportedOperationException.class, () -> c.getDrives().clear());
        });

        // ---- Chain of Responsibility
        test("Chain", "standard chain contains 9 rules in a fixed order", () ->
                eq("[Required parts, CPU socket, Memory type, Memory slots, Board fits case, GPU clearance, "
                                + "Drive bays, Integrated graphics, Power budget]",
                        CompatibilityRules.namesOf(CompatibilityRules.standardChain()).toString()));
        test("Chain", "every rule runs, so all problems are reported at once", () -> {
            List<String> issues = store.preset(BuildDirector.Preset.GAMING)
                    .cpu(sku("NT-CPU-04")).ram(sku("PH-1003"), 2).gpu(sku("PH-2003"))
                    .psu(sku("NT-PSU-01")).pcCase(sku("PH-4003")).validate();
            eq(5, issues.size());
        });
        test("Chain", "ITX board accepts at most 2 RAM modules", () -> {
            List<String> issues = store.newBuild().cpu(sku("NT-CPU-01")).motherboard(sku("NT-MB-04"))
                    .ram(sku("PH-1001"), 4).addDrive(sku("PH-3002")).psu(sku("NT-PSU-01"))
                    .pcCase(sku("PH-4003")).validate();
            check(issues.size() == 1 && issues.get(0).contains("1 to 2 RAM modules"), issues.toString());
        });
        test("Chain", "ITX case has only 2 drive bays", () -> {
            List<String> issues = store.newBuild().cpu(sku("NT-CPU-01")).motherboard(sku("NT-MB-04"))
                    .ram(sku("PH-1001"), 2).addDrive(sku("PH-3001")).addDrive(sku("PH-3002"))
                    .addDrive(sku("PH-3003")).psu(sku("NT-PSU-01")).pcCase(sku("PH-4003")).validate();
            check(issues.size() == 1 && issues.get(0).contains("room for 2 drives"), issues.toString());
        });
        test("Chain", "a new rule can be appended without changing existing ones", () -> {
            CompatibilityRule budget = new CompatibilityRule() {
                public String getName() { return "Budget limit"; }
                protected void check(PCBuilder b, List<String> issues) {
                    double sum = b.loadParts().stream().mapToDouble(Component::getPrice).sum();
                    if (sum > 1000) issues.add("Over the $1000 budget.");
                }
            };
            CompatibilityRule chain = CompatibilityRules.standardChain();
            CompatibilityRule last = chain;
            while (last.getNext() != null) last = last.getNext();
            last.then(budget);
            List<String> issues = store.preset(BuildDirector.Preset.WORKSTATION).rules(chain).validate();
            eq("[Over the $1000 budget.]", issues.toString());
            check(store.preset(BuildDirector.Preset.WORKSTATION).validate().isEmpty(), "standard chain changed");
        });
        test("Chain", "a rule can stop the chain", () -> {
            CompatibilityRule stopper = new CompatibilityRules.RequiredParts() {
                protected boolean shouldContinue(PCBuilder b, List<String> issues) { return issues.isEmpty(); }
            };
            stopper.then(new CompatibilityRules.PowerBudget());
            List<String> issues = store.newBuild().psu(sku("NT-PSU-01")).rules(stopper).validate();
            eq(5, issues.size()); // only "required" messages, power rule never ran
        });

        // ---- Decorator
        test("Decorator", "base price = hardware + assembly", () -> {
            Computer c = preset(BuildDirector.Preset.GAMING);
            near(c.getHardwarePrice() + BasePC.ASSEMBLY_FEE, new BasePC(c).getPrice());
        });
        test("Decorator", "add-ons stack and describe themselves", () -> {
            PCProduct p = new AddOns.RgbLighting(new AddOns.OsInstallation(new BasePC(preset(BuildDirector.Preset.OFFICE))));
            check(p.getDescription().endsWith("+ Windows 11 Pro + drivers setup + ARGB fan & lighting kit"), p.getDescription());
            eq(4, p.getLineItems().size());
        });
        test("Decorator", "warranty price includes what it wraps", () -> {
            PCProduct base = new AddOns.OsInstallation(new BasePC(preset(BuildDirector.Preset.GAMING)));
            PCProduct w = new AddOns.ExtendedWarranty(base, 2);
            near(base.getPrice() * 1.12, w.getPrice());
        });

        // ---- Strategy
        test("Strategy", "regular price has no discount", () ->
                near(2000, new PricingStrategies.Regular().total(1000, 2)));
        test("Strategy", "student discount is 10% capped at $150", () -> {
            near(900, new PricingStrategies.Student().total(1000, 1));
            near(1850, new PricingStrategies.Student().total(2000, 1));
        });
        test("Strategy", "corporate tiers: 4 = 0%, 5 = 7%, 10 = 12%", () -> {
            PricingStrategy s = new PricingStrategies.CorporateBulk();
            near(4000, s.total(1000, 4));
            near(4650, s.total(1000, 5));
            near(8800, s.total(1000, 10));
        });
        test("Strategy", "seasonal sale is per unit and never negative", () -> {
            near(1760, new PricingStrategies.SeasonalSale("Sale", 120).total(1000, 2));
            near(0, new PricingStrategies.SeasonalSale("Sale", 500).total(100, 1));
        });

        // ---- Observer
        test("Observer", "all attached observers receive each change", () -> {
            List<String> seen = new ArrayList<>();
            Order o = new Order("T", new BasePC(preset(BuildDirector.Preset.OFFICE)), 1, new PricingStrategies.Regular());
            OrderObserver a = (ord, from, to) -> seen.add("a:" + to);
            OrderObserver b = (ord, from, to) -> seen.add("b:" + to);
            o.attach(a); o.attach(b);
            o.advance();
            o.detach(b);
            o.advance();
            eq("[a:PAID, b:PAID, a:ASSEMBLING]", seen.toString());
        });
        test("State", "life cycle PLACED -> ... -> DELIVERED, then no further step", () -> {
            Order o = new Order("T", new BasePC(preset(BuildDirector.Preset.OFFICE)), 1, new PricingStrategies.Regular());
            List<OrderStatus> seen = new ArrayList<>();
            seen.add(o.getStatus());
            for (int i = 0; i < 5; i++) { o.advance(); seen.add(o.getStatus()); }
            eq("[PLACED, PAID, ASSEMBLING, TESTING, SHIPPED, DELIVERED]", seen.toString());
            expect(IllegalStateException.class, o::advance);
        });
        test("State", "only PLACED and PAID orders can be cancelled", () -> {
            Order o = new Order("T", new BasePC(preset(BuildDirector.Preset.OFFICE)), 1, new PricingStrategies.Regular());
            List<Boolean> allowed = new ArrayList<>();
            for (int i = 0; i < 6; i++) { allowed.add(o.canCancel()); if (i < 5) o.advance(); }
            eq("[true, true, false, false, false, false]", allowed.toString());
        });
        test("State", "cancelling while ASSEMBLING is rejected with a reason", () -> {
            Order o = store.checkout("Ann", "4242", preset(BuildDirector.Preset.OFFICE),
                    EnumSet.noneOf(AddOnOption.class), new PricingStrategies.Regular(), 1);
            o.advance();
            IllegalStateException e = expect(IllegalStateException.class, () -> store.cancelOrder(o, "too late"));
            check(e.getMessage().contains("being assembled"), e.getMessage());
            eq(OrderStatus.ASSEMBLING, o.getStatus());
        });
        test("State", "cancelled order cannot advance", () -> {
            Order o = new Order("T", new BasePC(preset(BuildDirector.Preset.OFFICE)), 1, new PricingStrategies.Regular());
            o.cancel("test");
            eq(OrderStatus.CANCELLED, o.getStatus());
            expect(IllegalStateException.class, o::advance);
            expect(IllegalStateException.class, () -> o.cancel("again"));
        });
        test("Facade", "cancelling a paid order refunds and restocks", () -> {
            Parts.Gpu gpu = sku("PH-2002");
            int before = Inventory.getInstance().getStock(gpu);
            Order o = store.checkout("Ann", "4242", preset(BuildDirector.Preset.GAMING),
                    EnumSet.noneOf(AddOnOption.class), new PricingStrategies.Regular(), 1);
            store.cancelOrder(o, "changed my mind");
            eq(OrderStatus.CANCELLED, o.getStatus());
            eq(before, Inventory.getInstance().getStock(gpu));
            near(o.getTotal(), store.getPaymentGateway().refundedTotal());
            near(0, store.getDashboard().getRevenue());
            eq("changed my mind", o.getCancelReason());
        });
        test("Observer", "low-stock alert fires at the threshold", () -> {
            Parts.Cpu i9 = sku("NT-CPU-04"); // 3 in stock
            Inventory.getInstance().reserve(Collections.singletonList(i9));
            check(log.stream().anyMatch(l -> l.contains("LOW STOCK") && l.contains("i9-14900K")), log.toString());
        });

        // ---- Facade
        test("Facade", "checkout reserves stock, charges and notifies", () -> {
            Computer c = preset(BuildDirector.Preset.GAMING);
            Parts.Gpu gpu = sku("PH-2002");
            int before = Inventory.getInstance().getStock(gpu);
            Order o = store.checkout("Ann", "4242", c, EnumSet.of(AddOnOption.OS_INSTALL), new PricingStrategies.Student(), 1);
            eq(OrderStatus.PAID, o.getStatus());
            eq(before - 1, Inventory.getInstance().getStock(gpu));
            eq(1, store.getDashboard().getPaidOrders());
            check(log.stream().anyMatch(l -> l.startsWith("[Email -> Ann]")), log.toString());
            eq(0, store.getPaymentGateway().openHolds());
        });
        test("Facade", "declined card leaves stock untouched", () -> {
            Computer c = preset(BuildDirector.Preset.WORKSTATION);
            Parts.Gpu gpu = sku("PH-2003");
            expect(PaymentGateway.PaymentDeclinedException.class, () ->
                    store.checkout("Bob", "0000", c, EnumSet.noneOf(AddOnOption.class), new PricingStrategies.Regular(), 1));
            eq(2, Inventory.getInstance().getStock(gpu));
            check(log.stream().noneMatch(l -> l.contains("LOW STOCK")), "false low-stock alert: " + log);
        });
        test("Facade", "out of stock releases the card hold", () -> {
            Computer c = preset(BuildDirector.Preset.WORKSTATION);
            expect(IllegalStateException.class, () ->
                    store.checkout("Corp", "4242", c, EnumSet.noneOf(AddOnOption.class), new PricingStrategies.CorporateBulk(), 5));
            eq(0, store.getPaymentGateway().openHolds());
            eq(0, store.getOrders().size());
        });
        test("Facade", "quote equals the order total", () -> {
            Computer c = preset(BuildDirector.Preset.OFFICE);
            PricingStrategy s = new PricingStrategies.CorporateBulk();
            double quoted = store.quote(c, EnumSet.of(AddOnOption.RGB), s, 6).getTotal();
            Order o = store.checkout("Co", "1234", c, EnumSet.of(AddOnOption.RGB), s, 6);
            near(quoted, o.getTotal());
        });

        // ---- Command
        Map<ComponentType, Component> sel = new EnumMap<>(ComponentType.class);
        int[] ram = {2};
        ConfigurationEditor editor = new ConfigurationEditor() {
            public Component getPart(ComponentType t) { return sel.get(t); }
            public void setPart(ComponentType t, Component c) { sel.put(t, c); }
            public int getRamModules() { return ram[0]; }
            public void setRamModules(int m) { ram[0] = m; }
        };
        test("Command", "undo restores the previous part, redo re-applies", () -> {
            sel.clear();
            CommandHistory h = new CommandHistory(10);
            h.execute(new ConfigCommands.SelectPart(editor, ComponentType.GPU, sku("PH-2001")));
            h.execute(new ConfigCommands.SelectPart(editor, ComponentType.GPU, sku("PH-2003")));
            h.undo();
            eq("PH-2001", sel.get(ComponentType.GPU).getSku());
            h.redo();
            eq("PH-2003", sel.get(ComponentType.GPU).getSku());
        });
        test("Command", "macro command is undone as one step", () -> {
            sel.clear();
            ram[0] = 2;
            CommandHistory h = new CommandHistory(10);
            h.execute(new ConfigCommands.Macro("preset")
                    .add(new ConfigCommands.SelectPart(editor, ComponentType.CPU, sku("NT-CPU-01")))
                    .add(new ConfigCommands.SelectPart(editor, ComponentType.PSU, sku("NT-PSU-01")))
                    .add(new ConfigCommands.ChangeRamModules(editor, 2, 1)));
            eq(1, h.size());
            h.undo();
            eq(null, sel.get(ComponentType.CPU));
            eq(null, sel.get(ComponentType.PSU));
            eq(2, ram[0]);
        });
        test("Command", "a new command clears the redo stack", () -> {
            sel.clear();
            CommandHistory h = new CommandHistory(10);
            h.execute(new ConfigCommands.SelectPart(editor, ComponentType.CASE, sku("PH-4001")));
            h.undo();
            check(h.canRedo(), "redo should be possible");
            h.execute(new ConfigCommands.SelectPart(editor, ComponentType.CASE, sku("PH-4002")));
            check(!h.canRedo(), "redo stack should be empty");
        });
        test("Command", "history is limited", () -> {
            CommandHistory h = new CommandHistory(3);
            for (int i = 0; i < 5; i++) h.execute(new ConfigCommands.ChangeRamModules(editor, i, i + 1));
            eq(3, h.size());
        });

        System.out.println("=======================");
        System.out.printf("%d passed, %d failed%n", passed, failures.size());
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  " + f));
            System.exit(1);
        }
    }
}
