package com.byteforge.ui;

import com.byteforge.builder.BuildDirector;
import com.byteforge.facade.AddOnOption;
import com.byteforge.inventory.Inventory;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.observer.Order;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Drives the GUI through a scripted scenario and saves PNG screenshots for the report.
 * Run with: {@code java -cp out com.byteforge.Main --screenshots docs/screenshots}
 */
public final class ScreenshotTour {
    private ScreenshotTour() {}

    public static void run(File dir) throws Exception {
        dir.mkdirs();
        SwingUtilities.invokeAndWait(() -> {
            try {
                tour(dir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.exit(0);
    }

    private static void tour(File dir) throws Exception {
        MainFrame f = new MainFrame();
        f.headless = true;
        f.setVisible(true);

        // 1. Gaming preset built by the Director, with two add-ons and student pricing
        f.loadPreset(BuildDirector.Preset.GAMING);
        f.setAddOn(AddOnOption.OS_INSTALL, true);
        f.setAddOn(AddOnOption.WARRANTY, true);
        f.setPricing(1);
        shot(f, new File(dir, "01_configure_gaming.png"));

        // 2. Incompatible build: Intel CPU on AM5 board, RTX 4090 in a mini-ITX case, 550 W PSU
        f.select(ComponentType.CPU, part("NT-CPU-04"));
        f.select(ComponentType.GPU, part("PH-2003"));
        f.select(ComponentType.CASE, part("PH-4003"));
        f.select(ComponentType.PSU, part("NT-PSU-01"));
        f.select(ComponentType.RAM, part("PH-1003"));
        f.refresh();
        shot(f, new File(dir, "02_incompatible_build.png"));

        // 2b. Command pattern: undo the last three changes (RAM, PSU, case)
        f.undo();
        f.undo();
        f.undo();
        shot(f, new File(dir, "03_undo_command.png"));
        f.undo();
        f.undo();

        // 3. Place several orders through the facade
        f.loadPreset(BuildDirector.Preset.GAMING);
        f.setAddOn(AddOnOption.WARRANTY, false);
        f.setAddOn(AddOnOption.OS_INSTALL, true);
        f.setPricing(1);
        f.setCustomer("Aruzhan S.", "4242");
        f.placeOrder();

        f.loadPreset(BuildDirector.Preset.OFFICE);
        f.setAddOn(AddOnOption.RGB, false);
        f.setPricing(2);
        f.setQuantity(6);
        f.setCustomer("Steppe Code LLP", "1881");
        shot(f, new File(dir, "04_corporate_quote.png"));
        f.placeOrder();

        f.loadPreset(BuildDirector.Preset.WORKSTATION);
        f.setQuantity(1);
        f.setAddOn(AddOnOption.OVERCLOCK, true);
        f.setAddOn(AddOnOption.RGB, true);
        f.setPricing(3);
        f.setCustomer("Daniyar T.", "7310");
        f.placeOrder();

        f.setCustomer("Bad Card Inc.", "0000");
        f.placeOrder(); // declined -> stock released

        // advance orders to show Observer notifications
        java.util.List<Order> orders = f.store().getOrders();
        for (int i = 0; i < 5; i++) f.store().advanceOrder(orders.get(0));
        for (int i = 0; i < 2; i++) f.store().advanceOrder(orders.get(1));
        f.store().advanceOrder(orders.get(2));
        f.refreshTables();

        f.tabs().setSelectedIndex(1);
        f.ordersTable().setRowSelectionInterval(1, 1);
        shot(f, new File(dir, "05_orders_observer.png"));

        f.tabs().setSelectedIndex(2);
        shot(f, new File(dir, "06_inventory_singleton.png"));
        f.dispose();
    }

    private static Component part(String sku) {
        return Inventory.getInstance().findBySku(sku);
    }

    private static void shot(MainFrame f, File file) throws Exception {
        JComponent root = (JComponent) f.getContentPane();
        root.revalidate();
        root.validate();
        f.scrollLogToEnd();
        BufferedImage img = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        root.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println("Saved " + file.getPath());
    }
}
