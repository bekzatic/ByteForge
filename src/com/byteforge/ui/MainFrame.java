package com.byteforge.ui;

import com.byteforge.builder.BuildDirector;
import com.byteforge.builder.Computer;
import com.byteforge.builder.PCBuilder;
import com.byteforge.command.Command;
import com.byteforge.command.CommandHistory;
import com.byteforge.command.ConfigCommands;
import com.byteforge.command.ConfigurationEditor;
import com.byteforge.facade.AddOnOption;
import com.byteforge.facade.StoreFacade;
import com.byteforge.inventory.Inventory;
import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;
import com.byteforge.model.Parts;
import com.byteforge.observer.Order;
import com.byteforge.observer.OrderStatus;
import com.byteforge.strategy.PricingStrategies;
import com.byteforge.strategy.PricingStrategy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Swing user interface. It talks to the domain exclusively through {@link StoreFacade}.
 */
public class MainFrame extends JFrame implements ConfigurationEditor {

    static final Color ACCENT = new Color(0x2F6FEB);
    static final Color OK = new Color(0x1A7F37);
    static final Color BAD = new Color(0xC62828);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final JTextArea logArea = new JTextArea(7, 80);
    private final StoreFacade store;

    private final Map<ComponentType, JComboBox<Object>> pickers = new EnumMap<>(ComponentType.class);
    private final JSpinner ramModules = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
    private final JComboBox<BuildDirector.Preset> presetBox = new JComboBox<>(BuildDirector.Preset.values());
    private final Map<AddOnOption, JCheckBox> addOnBoxes = new EnumMap<>(AddOnOption.class);
    private final JComboBox<PricingStrategy> pricingBox = new JComboBox<>(new PricingStrategy[]{
            new PricingStrategies.Regular(),
            new PricingStrategies.Student(),
            new PricingStrategies.CorporateBulk(),
            new PricingStrategies.SeasonalSale("Black Friday", 120)});
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JTextField customerField = new JTextField("Aruzhan S.", 12);
    private final JTextField cardField = new JTextField("4242", 5);

    private final JLabel statusLabel = new JLabel();
    private final JTextArea issuesArea = new JTextArea(5, 30);
    private final JProgressBar powerBar = new JProgressBar(0, 100);
    private final JTextArea receiptArea = new JTextArea(12, 40);
    private final JButton orderButton = new JButton("Place order");

    private final OrdersModel ordersModel = new OrdersModel();
    private final JTable ordersTable = new JTable(ordersModel);
    private final JLabel dashboardLabel = new JLabel();
    private final InventoryModel inventoryModel = new InventoryModel();
    private final JTable inventoryTable = new JTable(inventoryModel);
    private TableRowSorter<InventoryModel> inventorySorter; // created once the store exists
    private final JTextField inventorySearch = new JTextField(14);
    private final JComboBox<String> inventoryTypeBox = new JComboBox<>();
    private final JCheckBox lowStockOnly = new JCheckBox("Low stock only");
    private final JSpinner restockQty = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JLabel inventoryCount = new JLabel();
    private final JLabel orderHint =new JLabel("Select an order to see what can happen next.");
    private final JButton cancelButton = new JButton("Cancel selected order");
    private final JButton detailsButton = new JButton("Details...");
    private final JTabbedPane tabs = new JTabbedPane();

    private Computer currentComputer;
    private boolean updating;

    // Command pattern: invoker + snapshot of the last applied selection (the "old" values)
    private final CommandHistory history = new CommandHistory(100);
    private final Map<ComponentType, Component> lastParts = new EnumMap<>(ComponentType.class);
    private int lastRamModules = 2;
    private boolean batch;
    private final JButton undoButton = new JButton("Undo  (Ctrl+Z)");
    private final JButton redoButton = new JButton("Redo  (Ctrl+Y)");

    public MainFrame() {
        super("ByteForge - Custom PC Configurator");
        logArea.setEditable(false);
        logArea.setFont(MONO);
        store = new StoreFacade(this::log);
        store.loadCatalog();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.add(header(), BorderLayout.NORTH);

        tabs.addTab("Configure PC", configureTab());
        tabs.addTab("Orders", ordersTab());
        tabs.addTab("Inventory", inventoryTab());
        tabs.setBorder(new EmptyBorder(6, 8, 0, 8));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(100, 170));
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(4, 8, 8, 8),
                titled("Event log (Observer notifications)")));
        logPanel.add(logScroll);
        root.add(tabs, BorderLayout.CENTER);
        root.add(logPanel, BorderLayout.SOUTH);
        setContentPane(root);

        loadPreset(BuildDirector.Preset.GAMING);
        history.clear(); // the start-up preset is not something the user can undo
        updateHistoryButtons();
        installShortcuts(root);
        setSize(1360, 980);
        setLocationRelativeTo(null);
    }

    // =============================================================== layout

    private JComponent header() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setBackground(new Color(0x0D1B2A));
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("ByteForge  |  Custom PC Configurator & Store");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("Catalog imported through supplier adapters:   "
                + String.join("   •   ", Inventory.getInstance().getLoadedSuppliers()));
        sub.setForeground(new Color(0xA9C1E8));
        p.add(title);
        p.add(sub);
        return p;
    }

    private JComponent configureTab() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(titled("1. Choose components"));
        GridBagConstraints g = gbc();

        JButton presetBtn = new JButton("Load preset");
        presetBtn.addActionListener(e -> loadPreset((BuildDirector.Preset) presetBox.getSelectedItem()));
        addRow(left, g, "Preset (Director)", row(presetBox, presetBtn));

        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        JPanel history = new JPanel(new GridLayout(1, 2, 6, 0));
        history.add(undoButton);
        history.add(redoButton);
        addRow(left, g, "Undo / Redo", history);

        for (ComponentType type : ComponentType.values()) {
            JComboBox<Object> box = new JComboBox<>();
            if (type == ComponentType.GPU) {
                box.addItem("(none - integrated graphics)");
            }
            for (Component c : store.getParts(type)) {
                box.addItem(c);
            }
            box.setRenderer(new PartRenderer());
            box.addActionListener(e -> onPartChangedByUser(type));
            pickers.put(type, box);
            addRow(left, g, type.label(), box);
            if (type == ComponentType.RAM) {
                ramModules.addChangeListener(e -> onRamModulesChangedByUser());
                addRow(left, g, "RAM modules", ramModules);
            }
        }

        JPanel compat = new JPanel(new BorderLayout(4, 4));
        compat.setBorder(titled("2. Compatibility check (Builder + Chain of Responsibility, " + com.byteforge.rules.CompatibilityRules.namesOf(com.byteforge.rules.CompatibilityRules.standardChain()).size() + " rules)"));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        issuesArea.setEditable(false);
        issuesArea.setLineWrap(true);
        issuesArea.setWrapStyleWord(true);
        issuesArea.setOpaque(false);
        powerBar.setStringPainted(true);
        compat.add(statusLabel, BorderLayout.NORTH);
        compat.add(issuesArea, BorderLayout.CENTER);
        compat.add(powerBar, BorderLayout.SOUTH);
        g.gridx = 0; g.gridwidth = 2; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        left.add(compat, g);

        // ---- right column
        JPanel addOns = new JPanel(new GridLayout(0, 1));
        addOns.setBorder(titled("3. Services (Decorator)"));
        for (AddOnOption opt : AddOnOption.values()) {
            JCheckBox cb = new JCheckBox(opt.label());
            cb.addActionListener(e -> refresh());
            addOnBoxes.put(opt, cb);
            addOns.add(cb);
        }

        JPanel pricing = new JPanel(new GridBagLayout());
        pricing.setBorder(titled("4. Pricing (Strategy)"));
        GridBagConstraints g2 = gbc();
        pricingBox.addActionListener(e -> refresh());
        quantity.addChangeListener(e -> refresh());
        addRow(pricing, g2, "Customer type ", pricingBox);
        addRow(pricing, g2, "Quantity", quantity);

        receiptArea.setEditable(false);
        receiptArea.setFont(MONO);
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(titled("Quote"));

        JPanel checkout = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkout.setBorder(titled("5. Checkout (Facade)"));
        checkout.add(new JLabel("Customer:"));
        checkout.add(customerField);
        checkout.add(new JLabel("Card ****"));
        checkout.add(cardField);
        orderButton.setBackground(ACCENT);
        orderButton.setForeground(Color.WHITE);
        orderButton.setFont(orderButton.getFont().deriveFont(Font.BOLD));
        orderButton.addActionListener(e -> placeOrder());
        checkout.add(orderButton);

        JPanel rightTop = new JPanel(new BorderLayout());
        rightTop.add(addOns, BorderLayout.NORTH);
        rightTop.add(pricing, BorderLayout.SOUTH);
        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.add(rightTop, BorderLayout.NORTH);
        right.add(receiptScroll, BorderLayout.CENTER);
        right.add(checkout, BorderLayout.SOUTH);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(6, 6, 6, 6));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.58;
        c.insets = new Insets(0, 0, 0, 10);
        left.setPreferredSize(new Dimension(1, 1));
        right.setPreferredSize(new Dimension(1, 1));
        p.add(left, c);
        c.weightx = 0.42;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(right, c);
        return p;
    }

    private JComponent ordersTab() {
        ordersTable.setRowHeight(24);
        int[] widths = {70, 120, 480, 40, 240, 90, 100};
        for (int i = 0; i < widths.length; i++) {
            ordersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        ordersTable.setDefaultRenderer(Object.class, new StatusRenderer());
        JButton advance = new JButton("Advance selected order ->");
        advance.addActionListener(e -> advanceSelected());
        JButton advanceAll = new JButton("Advance all open orders");
        advanceAll.addActionListener(e -> {
            for (Order o : store.getOrders()) {
                if (!o.getStatus().isFinal()) store.advanceOrder(o);
            }
            ordersModel.fireTableDataChanged();
            updateDashboard();
        });
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelSelected());
        ordersTable.getSelectionModel().addListSelectionListener(e -> updateOrderHint());
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(e -> showOrderDetails());
        ordersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && ordersTable.getSelectedRow() >= 0) showOrderDetails();
            }
        });
        ordersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        dashboardLabel.setFont(dashboardLabel.getFont().deriveFont(Font.BOLD, 13f));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.add(advance);
        bar.add(advanceAll);
        bar.add(cancelButton);
        bar.add(detailsButton);
        bar.add(Box.createHorizontalStrut(20));
        bar.add(dashboardLabel);
        updateDashboard();

        orderHint.setBorder(new EmptyBorder(4, 12, 0, 0));
        orderHint.setFont(orderHint.getFont().deriveFont(Font.ITALIC));
        JPanel south = new JPanel(new BorderLayout());
        south.add(orderHint, BorderLayout.NORTH);
        south.add(bar, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(6, 6, 6, 6));
        p.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JComponent inventoryTab() {
        inventoryTable.setRowHeight(22);
        inventorySorter = new TableRowSorter<>(inventoryModel);
        inventoryTable.setRowSorter(inventorySorter);
        // stock changes (orders, cancellations, restocks) re-filter the rows; keep the counter in step
        inventorySorter.addRowSorterListener(e -> updateInventoryCount());
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(260);
        inventoryTable.setDefaultRenderer(Object.class, new StockRenderer());
        inventoryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ---- toolbar: search, category filter, low-stock filter, restock
        inventoryTypeBox.addItem("All categories");
        for (ComponentType t : ComponentType.values()) inventoryTypeBox.addItem(t.label());
        inventorySearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilter(); }
        });
        inventoryTypeBox.addActionListener(e -> applyInventoryFilter());
        lowStockOnly.addActionListener(e -> applyInventoryFilter());
        JButton restock = new JButton("Restock selected");
        restock.addActionListener(e -> restockSelected());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("Search:"));
        bar.add(inventorySearch);
        bar.add(inventoryTypeBox);
        bar.add(lowStockOnly);
        bar.add(Box.createHorizontalStrut(24));
        bar.add(new JLabel("Quantity:"));
        bar.add(restockQty);
        bar.add(restock);
        bar.add(Box.createHorizontalStrut(24));
        bar.add(inventoryCount);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(6, 6, 6, 6));
        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        JLabel note = new JLabel("  Single shared Inventory instance (Singleton). Rows in red are at or below "
                + Inventory.LOW_STOCK_THRESHOLD + " units; every stock change notifies the observers.");
        p.add(note, BorderLayout.SOUTH);
        applyInventoryFilter();
        return p;
    }

    void applyInventoryFilter() {
        String text = inventorySearch.getText().trim().toLowerCase();
        String type = inventoryTypeBox.getSelectedIndex() <= 0 ? null : (String) inventoryTypeBox.getSelectedItem();
        boolean low = lowStockOnly.isSelected();
        inventorySorter.setRowFilter(new RowFilter<InventoryModel, Integer>() {
            @Override
            public boolean include(Entry<? extends InventoryModel, ? extends Integer> e) {
                Component c = store.getAllParts().get(e.getIdentifier());
                if (type != null && !c.getType().label().equals(type)) return false;
                if (low && store.getStock(c) > Inventory.LOW_STOCK_THRESHOLD) return false;
                if (text.isEmpty()) return true;
                return (c.getSku() + " " + c.getName() + " " + c.getSpecs()).toLowerCase().contains(text);
            }
        });
        updateInventoryCount();
    }

    private void updateInventoryCount() {
        inventoryCount.setText(inventoryTable.getRowCount() + " of " + store.getAllParts().size() + " parts shown");
        inventoryCount.revalidate();
    }

    private void restockSelected() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            showError("Select a part in the table first.", "Restock");
            return;
        }
        restockPart(store.getAllParts().get(inventoryTable.convertRowIndexToModel(row)), (Integer) restockQty.getValue());
    }

    void restockPart(Component c, int qty) {
        store.restock(c, qty);
        inventoryModel.fireTableDataChanged();
        applyInventoryFilter();
        refreshPickerLabels();
    }

    void setInventoryFilter(String text, int typeIndex, boolean lowOnly) {
        inventorySearch.setText(text);
        inventoryTypeBox.setSelectedIndex(typeIndex);
        lowStockOnly.setSelected(lowOnly);
        applyInventoryFilter();
    }

    // =============================================================== behaviour

    /** Loads a preset as one undoable macro command. */
    void loadPreset(BuildDirector.Preset preset) {
        Computer c = store.preset(preset).build();
        ConfigCommands.Macro macro = new ConfigCommands.Macro("Load preset '" + preset + "'");
        addIfChanged(macro, ComponentType.CPU, c.getCpu());
        addIfChanged(macro, ComponentType.MOTHERBOARD, c.getMotherboard());
        addIfChanged(macro, ComponentType.RAM, c.getRam());
        addIfChanged(macro, ComponentType.GPU, c.getGpu());
        addIfChanged(macro, ComponentType.STORAGE, c.getDrives().get(0));
        addIfChanged(macro, ComponentType.PSU, c.getPsu());
        addIfChanged(macro, ComponentType.CASE, c.getCase());
        if (getRamModules() != c.getRamModules()) {
            macro.add(new ConfigCommands.ChangeRamModules(this, getRamModules(), c.getRamModules()));
        }
        updating = true;
        presetBox.setSelectedItem(preset);
        updating = false;
        if (!macro.isEmpty()) {
            batch = true;
            try {
                history.execute(macro);
            } finally {
                batch = false;
            }
        }
        log("[Director] Loaded preset '" + preset + "'.");
        refresh();
    }

    private void addIfChanged(ConfigCommands.Macro macro, ComponentType type, Component part) {
        if (getPart(type) != part) {
            macro.add(new ConfigCommands.SelectPart(this, type, part));
        }
    }

    // ---- Command pattern: receiver methods (ConfigurationEditor)

    @Override
    public Component getPart(ComponentType type) {
        Object item = pickers.get(type).getSelectedItem();
        return item instanceof Component ? (Component) item : null;
    }

    @Override
    public void setPart(ComponentType type, Component part) {
        updating = true;
        try {
            select(type, part);
        } finally {
            updating = false;
        }
        if (!batch) refresh();
    }

    @Override
    public int getRamModules() {
        return (Integer) ramModules.getValue();
    }

    @Override
    public void setRamModules(int modules) {
        updating = true;
        try {
            ramModules.setValue(modules);
        } finally {
            updating = false;
        }
        if (!batch) refresh();
    }

    // ---- Command pattern: user edits become commands

    private void onPartChangedByUser(ComponentType type) {
        if (updating) return;
        Component now = getPart(type);
        Component before = lastParts.get(type);
        if (now != before) {
            history.record(new ConfigCommands.SelectPart(this, type, before, now));
        }
        refresh();
    }

    private void onRamModulesChangedByUser() {
        if (updating) return;
        int now = getRamModules();
        if (now != lastRamModules) {
            history.record(new ConfigCommands.ChangeRamModules(this, lastRamModules, now));
        }
        refresh();
    }

    void undo() {
        batch = true;
        Command c;
        try {
            c = history.undo();
        } finally {
            batch = false;
        }
        if (c != null) log("[Command] Undo: " + c.getName());
        refresh();
    }

    void redo() {
        batch = true;
        Command c;
        try {
            c = history.redo();
        } finally {
            batch = false;
        }
        if (c != null) log("[Command] Redo: " + c.getName());
        refresh();
    }

    private void updateHistoryButtons() {
        undoButton.setEnabled(history.canUndo());
        redoButton.setEnabled(history.canRedo());
        undoButton.setToolTipText(history.canUndo() ? "Undo: " + history.peekUndoName() + "  (Ctrl+Z)" : null);
        redoButton.setToolTipText(history.canRedo() ? "Redo: " + history.peekRedoName() + "  (Ctrl+Y)" : null);
    }

    private void installShortcuts(JComponent root) {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, mask), "undo");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, mask), "redo");
        root.getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });
        root.getActionMap().put("redo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { redo(); }
        });
    }

    void select(ComponentType type, Component c) {
        JComboBox<Object> box = pickers.get(type);
        if (c == null) {
            box.setSelectedIndex(0);
        } else {
            box.setSelectedItem(c);
        }
    }

    private PCBuilder builderFromUi() {
        PCBuilder b = store.newBuild().name("Custom build");
        for (ComponentType t : ComponentType.values()) {
            Object item = pickers.get(t).getSelectedItem();
            if (item instanceof Component) {
                b.part((Component) item);
            }
        }
        Object ram = pickers.get(ComponentType.RAM).getSelectedItem();
        if (ram instanceof Parts.Ram) {
            b.ram((Parts.Ram) ram, (Integer) ramModules.getValue());
        }
        Object preset = presetBox.getSelectedItem();
        if (preset != null && matchesPreset((BuildDirector.Preset) preset)) {
            b.name(preset.toString());
        }
        return b;
    }

    private boolean matchesPreset(BuildDirector.Preset preset) {
        try {
            Computer p = store.preset(preset).build();
            return pickers.get(ComponentType.CPU).getSelectedItem() == p.getCpu()
                    && pickers.get(ComponentType.GPU).getSelectedItem() == (p.getGpu() == null
                    ? pickers.get(ComponentType.GPU).getItemAt(0) : p.getGpu());
        } catch (RuntimeException e) {
            return false;
        }
    }

    void refresh() {
        if (updating) {
            return;
        }
        for (ComponentType t : ComponentType.values()) {
            lastParts.put(t, getPart(t));
        }
        lastRamModules = getRamModules();
        updateHistoryButtons();

        PCBuilder b = builderFromUi();
        List<String> issues = b.validate();
        currentComputer = null;
        if (issues.isEmpty()) {
            currentComputer = b.build();
            statusLabel.setText("✔  All parts are compatible");
            statusLabel.setForeground(OK);
            issuesArea.setText(currentComputer.summary());
            issuesArea.setFont(MONO);
            int draw = currentComputer.getEstimatedPowerDraw();
            int psu = currentComputer.getPsu().getWatts();
            powerBar.setValue(Math.min(100, draw * 100 / psu));
            powerBar.setString("Estimated load " + draw + " W of " + psu + " W PSU");
            powerBar.setForeground(draw * 1.25 <= psu ? OK : BAD);
        } else {
            statusLabel.setText("✖  " + issues.size() + " compatibility problem" + (issues.size() > 1 ? "s" : ""));
            statusLabel.setForeground(BAD);
            issuesArea.setFont(UIManager.getFont("Label.font"));
            StringBuilder sb = new StringBuilder();
            for (String i : issues) sb.append("• ").append(i).append('\n');
            issuesArea.setText(sb.toString());
            powerBar.setValue(0);
            powerBar.setString("Recommended PSU: " + b.recommendedPsuWatts() + " W");
        }
        updateQuote();
    }

    private Set<AddOnOption> selectedAddOns() {
        Set<AddOnOption> set = EnumSet.noneOf(AddOnOption.class);
        addOnBoxes.forEach((k, v) -> { if (v.isSelected()) set.add(k); });
        return set;
    }

    private void updateQuote() {
        if (currentComputer == null) {
            receiptArea.setText("Fix the compatibility problems to get a quote.");
            orderButton.setEnabled(false);
            return;
        }
        StoreFacade.Quote q = store.quote(currentComputer, selectedAddOns(),
                (PricingStrategy) pricingBox.getSelectedItem(), (Integer) quantity.getValue());
        receiptArea.setText(q.getProduct().getDescription() + "\n\n" + q.receipt());
        receiptArea.setCaretPosition(0);
        orderButton.setEnabled(true);
    }

    void placeOrder() {
        if (currentComputer == null) {
            return;
        }
        try {
            Order o = store.checkout(customerField.getText().trim(), cardField.getText().trim(), currentComputer,
                    selectedAddOns(), (PricingStrategy) pricingBox.getSelectedItem(), (Integer) quantity.getValue());
            ordersModel.fireTableDataChanged();
            inventoryModel.fireTableDataChanged();
            updateDashboard();
            if (!headless) {
                JOptionPane.showMessageDialog(this, String.format("Order %s confirmed. Total $%,.2f",
                        o.getId(), o.getTotal()), "Order placed", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (RuntimeException ex) {
            inventoryModel.fireTableDataChanged();
            if (!headless) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Order failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        refreshPickerLabels();
    }

    private Order selectedOrder() {
        int row = ordersTable.getSelectedRow();
        return row < 0 ? null : store.getOrders().get(ordersTable.convertRowIndexToModel(row));
    }

    private void advanceSelected() {
        Order o = selectedOrder();
        if (o == null) return;
        try {
            store.advanceOrder(o);
        } catch (IllegalStateException ex) {
            showError(ex.getMessage(), "Cannot advance");
        }
        afterOrderChange(o);
    }

    /** State pattern in the GUI: the current state decides whether cancelling is allowed. */
    void cancelSelected() {
        Order o = selectedOrder();
        if (o == null) return;
        String reason = headless ? "customer changed their mind"
                : JOptionPane.showInputDialog(this, "Reason for cancelling " + o.getId() + ":",
                "customer changed their mind");
        if (reason == null) return; // dialog closed
        try {
            store.cancelOrder(o, reason);
        } catch (IllegalStateException ex) {
            showError(ex.getMessage(), "Cannot cancel");
        }
        afterOrderChange(o);
    }

    /** Shows the full receipt of the selected order. */
    void showOrderDetails() {
        Order o = selectedOrder();
        if (o == null) return;
        String text = store.orderDetails(o);
        if (headless) {
            log("[GUI] Details of " + o.getId() + " opened");
            return;
        }
        JTextArea area = new JTextArea(text);
        area.setFont(MONO);
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 380));
        JOptionPane.showMessageDialog(this, scroll, "Order " + o.getId(), JOptionPane.PLAIN_MESSAGE);
    }

    private void afterOrderChange(Order o) {
        int index = store.getOrders().indexOf(o);
        ordersModel.fireTableDataChanged();
        inventoryModel.fireTableDataChanged();
        if (index >= 0) ordersTable.setRowSelectionInterval(index, index);
        updateDashboard();
        updateOrderHint();
    }

    private void updateOrderHint() {
        Order o = selectedOrder();
        if (o == null) {
            orderHint.setText("Select an order to see what can happen next. Double-click for details.");
            cancelButton.setEnabled(false);
            detailsButton.setEnabled(false);
        } else {
            orderHint.setText(o.getId() + " - " + o.getStatus() + ": " + o.getHint());
            cancelButton.setEnabled(o.canCancel());
            detailsButton.setEnabled(true);
        }
    }

    private void showError(String message, String title) {
        log("[GUI] " + message);
        if (!headless) {
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
        }
    }

    void selectOrder(int index) {
        ordersTable.setRowSelectionInterval(index, index);
        updateOrderHint();
    }

    private void updateDashboard() {
        dashboardLabel.setText(String.format("Dashboard:  %d paid   |   %d cancelled   |   %d delivered   |   revenue $%,.2f",
                store.getDashboard().getPaidOrders(), store.getDashboard().getCancelled(),
                store.getDashboard().getDelivered(), store.getDashboard().getRevenue()));
    }

    private void refreshPickerLabels() {
        pickers.values().forEach(JComboBox::repaint);
    }

    void log(String message) {
        String line = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + message + "\n";
        System.out.print(line);
        Runnable r = () -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) r.run(); else SwingUtilities.invokeLater(r);
    }

    // =============================================================== helpers for the screenshot tour

    boolean headless;

    void scrollLogToEnd() {
        getContentPane().validate();
        JViewport vp = (JViewport) logArea.getParent();
        vp.setViewPosition(new Point(0, Math.max(0, logArea.getPreferredSize().height - vp.getHeight())));
    }

    StoreFacade store() { return store; }
    JTabbedPane tabs() { return tabs; }
    JTable ordersTable() { return ordersTable; }
    void setAddOn(AddOnOption o, boolean on) { addOnBoxes.get(o).setSelected(on); refresh(); }
    void setPricing(int index) { pricingBox.setSelectedIndex(index); }
    void setQuantity(int q) { quantity.setValue(q); }
    void setCustomer(String name, String card) { customerField.setText(name); cardField.setText(card); }
    void refreshTables() { ordersModel.fireTableDataChanged(); inventoryModel.fireTableDataChanged(); updateDashboard(); }

    // =============================================================== small UI utilities

    private static TitledBorder titled(String text) {
        TitledBorder b = BorderFactory.createTitledBorder(text);
        b.setTitleColor(ACCENT.darker());
        b.setTitleFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
        return b;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        return g;
    }

    private static void addRow(JPanel p, GridBagConstraints g, String label, JComponent field) {
        g.gridx = 0; g.weightx = 0; g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        // small preferred width so the label column never gets squeezed; the field takes the extra space
        field.setPreferredSize(new Dimension(80, field.getPreferredSize().height));
        p.add(field, g);
        g.gridy++;
    }

    private static JComponent row(JComponent a, JComponent b) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(a, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        return p;
    }

    private class PartRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                               boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, value, index, sel, focus);
            if (value instanceof Component) {
                Component c = (Component) value;
                int stock = store.getStock(c);
                setText(String.format("<html><b>%s</b> &nbsp;<font color='#666666'>%s</font> &nbsp;$%.0f"
                                + " &nbsp;<font color='%s'>[%d left]</font></html>",
                        c.getName(), c.getSpecs(), c.getPrice(), stock <= Inventory.LOW_STOCK_THRESHOLD
                                ? "#C62828" : "#1A7F37", stock));
            }
            return this;
        }
    }

    private class OrdersModel extends AbstractTableModel {
        private final String[] cols = {"Order", "Customer", "Product", "Qty", "Pricing", "Total", "Status"};

        @Override public int getRowCount() { return store.getOrders().size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Order o = store.getOrders().get(r);
            switch (c) {
                case 0: return o.getId();
                case 1: return o.getCustomer();
                case 2: return o.getProduct().getDescription();
                case 3: return o.getQuantity();
                case 4: return o.getPricing().getName();
                case 5: return String.format("$%,.2f", o.getTotal());
                default: return o.getStatus();
            }
        }
    }

    private class InventoryModel extends AbstractTableModel {
        private final String[] cols = {"SKU", "Type", "Part", "Specs", "Price", "Stock"};

        @Override public int getRowCount() { return store.getAllParts().size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Component p = store.getAllParts().get(r);
            switch (c) {
                case 0: return p.getSku();
                case 1: return p.getType().label();
                case 2: return p.getName();
                case 3: return p.getSpecs();
                case 4: return String.format("$%.2f", p.getPrice());
                default: return store.getStock(p);
            }
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
                                                                int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setForeground(sel ? t.getSelectionForeground() : Color.BLACK);
            setFont(getFont().deriveFont(Font.PLAIN));
            if (v instanceof OrderStatus) {
                OrderStatus s = (OrderStatus) v;
                setFont(getFont().deriveFont(Font.BOLD));
                if (!sel) setForeground(s == OrderStatus.DELIVERED ? OK : s == OrderStatus.PAID ? ACCENT
                        : s == OrderStatus.CANCELLED ? Color.GRAY : new Color(0xB26A00));
            }
            return this;
        }
    }

    private static class StockRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
                                                                int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            Object stock = t.getValueAt(row, 5);
            boolean low = stock instanceof Integer && (Integer) stock <= Inventory.LOW_STOCK_THRESHOLD;
            if (!sel) {
                setBackground(low ? new Color(0xFDECEA) : Color.WHITE);
                setForeground(low ? BAD : Color.BLACK);
            }
            return this;
        }
    }
}
