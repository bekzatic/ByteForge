package com.byteforge;

import com.byteforge.builder.BuildDirector;
import com.byteforge.builder.Computer;
import com.byteforge.builder.IncompatibleBuildException;
import com.byteforge.builder.PCBuilder;
import com.byteforge.command.CommandHistory;
import com.byteforge.command.ConfigCommands;
import com.byteforge.command.ConfigurationEditor;
import com.byteforge.facade.AddOnOption;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import java.util.EnumMap;
import java.util.Map;
import com.byteforge.facade.PaymentGateway;
import com.byteforge.facade.StoreFacade;
import com.byteforge.inventory.Inventory;
import com.byteforge.model.Parts;
import com.byteforge.observer.Order;
import com.byteforge.rules.CompatibilityRules;
import com.byteforge.strategy.PricingStrategies;
import com.byteforge.strategy.PricingStrategy;

import java.util.EnumSet;

/**
 * Text-mode walkthrough that exercises all eleven patterns in order.
 * Run with: {@code java -cp out com.byteforge.Main --console}
 */
public final class ConsoleDemo {
    private ConsoleDemo() {}

    public static void run() {
        section("1. ADAPTER + FACTORY + SINGLETON: loading the catalog");
        StoreFacade store = new StoreFacade(msg -> System.out.println("   " + msg));
        store.loadCatalog();
        Inventory inv = Inventory.getInstance();
        System.out.println("   Same inventory instance everywhere: " + (inv == Inventory.getInstance()));
        System.out.println("   Parts in catalog: " + inv.all().size());

        section("2. BUILDER + CHAIN OF RESPONSIBILITY: a rejected custom build");
        System.out.println("   Rule chain: " + String.join(" -> ",
                CompatibilityRules.namesOf(CompatibilityRules.standardChain())));
        PCBuilder bad = store.newBuild()
                .name("Mismatched build")
                .cpu(inv.findBySku("NT-CPU-03"))          // Intel LGA1700
                .motherboard(inv.findBySku("NT-MB-01"))   // AM5, DDR5
                .ram(inv.findBySku("PH-1003"), 2)         // DDR4
                .gpu(inv.findBySku("PH-2003"))            // RTX 4090, 336 mm
                .addDrive(inv.findBySku("PH-3002"))
                .psu(inv.findBySku("NT-PSU-01"))          // 550 W
                .pcCase(inv.findBySku("PH-4003"));        // ITX, 322 mm
        try {
            bad.build();
        } catch (IncompatibleBuildException e) {
            e.getIssues().forEach(i -> System.out.println("   x " + i));
        }

        section("2b. BUILDER (Director): a preset that passes validation");
        Computer gaming = store.preset(BuildDirector.Preset.GAMING).build();
        System.out.print(indent(gaming.summary()));

        section("3. DECORATOR + STRATEGY: same PC, different add-ons and pricing");
        PricingStrategy[] strategies = {
                new PricingStrategies.Regular(),
                new PricingStrategies.Student(),
                new PricingStrategies.CorporateBulk(),
                new PricingStrategies.SeasonalSale("Black Friday", 120)};
        StoreFacade.Quote q = store.quote(gaming, EnumSet.of(AddOnOption.OS_INSTALL, AddOnOption.WARRANTY),
                strategies[0], 1);
        System.out.println("   " + q.getProduct().getDescription());
        System.out.print(indent(q.receipt()));
        System.out.println();
        for (PricingStrategy s : strategies) {
            int qty = s instanceof PricingStrategies.CorporateBulk ? 6 : 1;
            StoreFacade.Quote sq = store.quote(gaming, EnumSet.of(AddOnOption.OS_INSTALL), s, qty);
            System.out.printf("   %-40s qty %-2d -> $%,10.2f%n", s.getName(), qty, sq.getTotal());
        }

        section("4. FACADE + OBSERVER: checkout and order tracking");
        Order order = store.checkout("Aruzhan S.", "4242", gaming,
                EnumSet.of(AddOnOption.OS_INSTALL), strategies[1], 1);
        while (!order.getStatus().isFinal()) {
            store.advanceOrder(order);
        }

        section("5. OBSERVER on the singleton: low-stock alerts");
        Computer workstation = store.preset(BuildDirector.Preset.WORKSTATION).build();
        store.checkout("Daniyar T.", "7310", workstation, EnumSet.noneOf(AddOnOption.class), strategies[0], 1);

        section("6. FACADE error handling: a declined card never touches stock");
        Parts.Gpu gpu = inv.findBySku("PH-2002");
        System.out.println("   RX 7800 XT stock before: " + inv.getStock(gpu));
        try {
            store.checkout("Bad Card Inc.", "0000", gaming, EnumSet.noneOf(AddOnOption.class), strategies[0], 1);
        } catch (PaymentGateway.PaymentDeclinedException e) {
            System.out.println("   Declined: " + e.getMessage());
        }
        System.out.println("   RX 7800 XT stock after:  " + inv.getStock(gpu));

        section("7. STATE: cancelling depends on where the order is");
        Computer office = store.preset(BuildDirector.Preset.OFFICE).build();
        Order early = store.checkout("Timur K.", "5521", office, EnumSet.noneOf(AddOnOption.class), strategies[0], 1);
        System.out.println("   " + early.getId() + " is " + early.getStatus() + " -> " + early.getHint());
        store.cancelOrder(early, "customer changed their mind");
        Order late = store.checkout("Aliya N.", "6012", office, EnumSet.noneOf(AddOnOption.class), strategies[0], 1);
        store.advanceOrder(late);
        System.out.println("   " + late.getId() + " is " + late.getStatus() + " -> " + late.getHint());
        try {
            store.cancelOrder(late, "too late");
        } catch (IllegalStateException e) {
            System.out.println("   Refused: " + e.getMessage());
        }

        section("8. COMMAND: undo / redo of configuration changes");
        Map<ComponentType, Component> selection = new EnumMap<>(ComponentType.class);
        int[] ram = {2};
        ConfigurationEditor editor = new ConfigurationEditor() {
            public Component getPart(ComponentType t) { return selection.get(t); }
            public void setPart(ComponentType t, Component c) { selection.put(t, c); }
            public int getRamModules() { return ram[0]; }
            public void setRamModules(int m) { ram[0] = m; }
        };
        CommandHistory history = new CommandHistory(50);
        history.execute(new ConfigCommands.SelectPart(editor, ComponentType.GPU, inv.findBySku("PH-2001")));
        history.execute(new ConfigCommands.SelectPart(editor, ComponentType.GPU, inv.findBySku("PH-2003")));
        history.execute(new ConfigCommands.ChangeRamModules(editor, 2, 4));
        System.out.println("   After 3 commands: GPU = " + selection.get(ComponentType.GPU).getName() + ", RAM modules = " + ram[0]);
        System.out.println("   Undo: " + history.undo().getName());
        System.out.println("   Undo: " + history.undo().getName());
        System.out.println("   Now:  GPU = " + selection.get(ComponentType.GPU).getName() + ", RAM modules = " + ram[0]);
        System.out.println("   Redo: " + history.redo().getName());
        System.out.println("   Now:  GPU = " + selection.get(ComponentType.GPU).getName());

        section("Done");
        System.out.printf("   Paid orders: %d, revenue $%,.2f%n",
                store.getDashboard().getPaidOrders(), store.getDashboard().getRevenue());
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " " + "=".repeat(Math.max(3, 70 - title.length())));
    }

    private static String indent(String s) {
        return "   " + s.replace("\n", "\n   ").replaceAll(" +$", "");
    }
}
