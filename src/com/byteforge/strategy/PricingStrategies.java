package com.byteforge.strategy;

/**
 * STRATEGY pattern - the <b>Concrete Strategies</b>.
 */
public final class PricingStrategies {
    private PricingStrategies() {}

    /** Full price. */
    public static class Regular implements PricingStrategy {
        @Override public String getName() { return "Regular price"; }
        @Override public double discount(double unitPrice, int quantity) { return 0; }
        @Override public String toString() { return getName(); }
    }

    /** 10% off for students, capped at $150 per order. */
    public static class Student implements PricingStrategy {
        @Override public String getName() { return "Student (-10%, max $150)"; }
        @Override public double discount(double unitPrice, int quantity) {
            return Math.min(unitPrice * quantity * 0.10, 150.0);
        }
        @Override public String toString() { return getName(); }
    }

    /** Volume tiers for companies: 5+ units 7%, 10+ units 12%. */
    public static class CorporateBulk implements PricingStrategy {
        @Override public String getName() { return "Corporate bulk (5+: -7%, 10+: -12%)"; }
        @Override public double discount(double unitPrice, int quantity) {
            double rate = quantity >= 10 ? 0.12 : quantity >= 5 ? 0.07 : 0.0;
            return unitPrice * quantity * rate;
        }
        @Override public String toString() { return getName(); }
    }

    /** Flat amount off each machine during a seasonal campaign. */
    public static class SeasonalSale implements PricingStrategy {
        private final String campaign;
        private final double amountPerUnit;

        public SeasonalSale(String campaign, double amountPerUnit) {
            this.campaign = campaign;
            this.amountPerUnit = amountPerUnit;
        }

        @Override public String getName() {
            return String.format("%s (-$%.0f per PC)", campaign, amountPerUnit);
        }
        @Override public double discount(double unitPrice, int quantity) {
            return Math.min(amountPerUnit, unitPrice) * quantity;
        }
        @Override public String toString() { return getName(); }
    }
}
