package com.byteforge.decorator;

/**
 * DECORATOR pattern - the <b>Concrete Decorators</b> (service add-ons).
 */
public final class AddOns {
    private AddOns() {}

    /** Warranty price depends on what it wraps: 6% of the product value per extra year. */
    public static class ExtendedWarranty extends AddOnDecorator {
        private final int years;

        public ExtendedWarranty(PCProduct wrapped, int years) {
            super(wrapped);
            this.years = years;
        }

        @Override protected String addOnName() { return "Extended warranty (+" + years + " yr)"; }
        @Override protected double addOnPrice() {
            return Math.round(wrapped.getPrice() * 0.06 * years * 100) / 100.0;
        }
    }

    public static class RgbLighting extends AddOnDecorator {
        public RgbLighting(PCProduct wrapped) { super(wrapped); }

        @Override protected String addOnName() { return "ARGB fan & lighting kit"; }
        @Override protected double addOnPrice() { return 49.00; }
    }

    public static class Overclocking extends AddOnDecorator {
        public Overclocking(PCProduct wrapped) { super(wrapped); }

        @Override protected String addOnName() { return "Pro overclock & tuning"; }
        @Override protected double addOnPrice() { return 79.00; }
    }

    public static class OsInstallation extends AddOnDecorator {
        public OsInstallation(PCProduct wrapped) { super(wrapped); }

        @Override protected String addOnName() { return "Windows 11 Pro + drivers setup"; }
        @Override protected double addOnPrice() { return 139.00; }
    }
}
