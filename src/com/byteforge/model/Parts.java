package com.byteforge.model;

/**
 * Concrete hardware parts. They are grouped in one file as static nested classes
 * to keep the model compact; each one is a normal {@link Component} subclass.
 */
public final class Parts {
    private Parts() {}

    public static class Cpu extends Component {
        private final String socket;
        private final int cores;
        private final double ghz;

        public Cpu(String sku, String brand, String model, double price, int tdp,
                   String socket, int cores, double ghz) {
            super(sku, brand, model, price, tdp);
            this.socket = socket;
            this.cores = cores;
            this.ghz = ghz;
        }

        public String getSocket() { return socket; }
        public int getCores() { return cores; }
        public double getGhz() { return ghz; }

        @Override public ComponentType getType() { return ComponentType.CPU; }
        @Override public String getSpecs() {
            return String.format("%s, %d cores @ %.1f GHz, %d W", socket, cores, ghz, getPowerDraw());
        }
    }

    public static class Motherboard extends Component {
        private final String socket;
        private final String ramType;
        private final String formFactor;

        public Motherboard(String sku, String brand, String model, double price, int power,
                           String socket, String ramType, String formFactor) {
            super(sku, brand, model, price, power);
            this.socket = socket;
            this.ramType = ramType;
            this.formFactor = formFactor;
        }

        public String getSocket() { return socket; }
        public String getRamType() { return ramType; }
        public String getFormFactor() { return formFactor; }

        @Override public ComponentType getType() { return ComponentType.MOTHERBOARD; }
        @Override public String getSpecs() {
            return String.format("%s, %s, %s", socket, ramType, formFactor);
        }
    }

    public static class Ram extends Component {
        private final String ramType;
        private final int capacityGb;
        private final int speedMhz;

        public Ram(String sku, String brand, String model, double price, int power,
                   String ramType, int capacityGb, int speedMhz) {
            super(sku, brand, model, price, power);
            this.ramType = ramType;
            this.capacityGb = capacityGb;
            this.speedMhz = speedMhz;
        }

        public String getRamType() { return ramType; }
        public int getCapacityGb() { return capacityGb; }
        public int getSpeedMhz() { return speedMhz; }

        @Override public ComponentType getType() { return ComponentType.RAM; }
        @Override public String getSpecs() {
            return String.format("%d GB %s-%d", capacityGb, ramType, speedMhz);
        }
    }

    public static class Gpu extends Component {
        private final int vramGb;
        private final int lengthMm;

        public Gpu(String sku, String brand, String model, double price, int tdp,
                   int vramGb, int lengthMm) {
            super(sku, brand, model, price, tdp);
            this.vramGb = vramGb;
            this.lengthMm = lengthMm;
        }

        public int getVramGb() { return vramGb; }
        public int getLengthMm() { return lengthMm; }

        @Override public ComponentType getType() { return ComponentType.GPU; }
        @Override public String getSpecs() {
            return String.format("%d GB VRAM, %d mm, %d W", vramGb, lengthMm, getPowerDraw());
        }
    }

    public static class Storage extends Component {
        private final String kind; // NVMe, SATA SSD, HDD
        private final int capacityGb;

        public Storage(String sku, String brand, String model, double price, int power,
                       String kind, int capacityGb) {
            super(sku, brand, model, price, power);
            this.kind = kind;
            this.capacityGb = capacityGb;
        }

        public String getKind() { return kind; }
        public int getCapacityGb() { return capacityGb; }

        @Override public ComponentType getType() { return ComponentType.STORAGE; }
        @Override public String getSpecs() {
            String cap = capacityGb >= 1000 ? (capacityGb / 1000) + " TB" : capacityGb + " GB";
            return cap + " " + kind;
        }
    }

    public static class PowerSupply extends Component {
        private final int watts;
        private final String rating;

        public PowerSupply(String sku, String brand, String model, double price,
                           int watts, String rating) {
            super(sku, brand, model, price, 0);
            this.watts = watts;
            this.rating = rating;
        }

        public int getWatts() { return watts; }
        public String getRating() { return rating; }

        @Override public ComponentType getType() { return ComponentType.PSU; }
        @Override public String getSpecs() {
            return watts + " W, 80+ " + rating;
        }
    }

    public static class PcCase extends Component {
        private final String formFactor; // largest supported board
        private final int maxGpuLengthMm;

        public PcCase(String sku, String brand, String model, double price,
                      String formFactor, int maxGpuLengthMm) {
            super(sku, brand, model, price, 0);
            this.formFactor = formFactor;
            this.maxGpuLengthMm = maxGpuLengthMm;
        }

        public String getFormFactor() { return formFactor; }
        public int getMaxGpuLengthMm() { return maxGpuLengthMm; }

        @Override public ComponentType getType() { return ComponentType.CASE; }
        @Override public String getSpecs() {
            return formFactor + ", GPU up to " + maxGpuLengthMm + " mm";
        }
    }
}
