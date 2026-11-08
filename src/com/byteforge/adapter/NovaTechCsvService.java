package com.byteforge.adapter;

/**
 * ADAPTER pattern - <b>Adaptee #1</b>.
 * <p>
 * Simulates a legacy wholesale distributor that can only export its price list as
 * semicolon-separated text: {@code sku;TYPE;brand;model;price;stock;k=v|k=v}.
 * We cannot change this class (it represents third-party code).
 */
public class NovaTechCsvService {

    public String exportCatalogCsv() {
        return String.join("\n",
                "# NovaTech Distribution - price list",
                "NT-CPU-01;CPU;AMD;Ryzen 5 7600;199.00;12;socket=AM5|cores=6|ghz=3.8|tdp=65",
                "NT-CPU-02;CPU;AMD;Ryzen 7 7800X3D;389.00;6;socket=AM5|cores=8|ghz=4.2|tdp=120",
                "NT-CPU-03;CPU;Intel;Core i5-14400F;179.00;10;socket=LGA1700|cores=10|ghz=2.5|tdp=148",
                "NT-CPU-04;CPU;Intel;Core i9-14900K;549.00;3;socket=LGA1700|cores=24|ghz=3.2|tdp=253",
                "NT-MB-01;MOTHERBOARD;ASUS;TUF Gaming B650-Plus;189.00;8;socket=AM5|ramType=DDR5|formFactor=ATX",
                "NT-MB-02;MOTHERBOARD;MSI;PRO B760M-A DDR4;129.00;9;socket=LGA1700|ramType=DDR4|formFactor=mATX",
                "NT-MB-03;MOTHERBOARD;Gigabyte;Z790 AORUS Elite;259.00;4;socket=LGA1700|ramType=DDR5|formFactor=ATX",
                "NT-MB-04;MOTHERBOARD;ASRock;A620I Lightning;139.00;5;socket=AM5|ramType=DDR5|formFactor=ITX",
                "NT-PSU-01;PSU;Corsair;CX550;69.00;15;watts=550|rating=Bronze",
                "NT-PSU-02;PSU;Corsair;RM850x;139.00;7;watts=850|rating=Gold",
                "NT-PSU-03;PSU;Seasonic;Prime TX-1000;259.00;3;watts=1000|rating=Titanium");
    }
}
