package com.byteforge.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ADAPTER pattern - <b>Adaptee #2</b>.
 * <p>
 * Simulates a modern third-party REST API that returns JSON-like maps with its own
 * vocabulary: categories such as "graphics", prices in cents, specs nested in a map.
 */
public class PartHubApiClient {

    public List<Map<String, Object>> getProducts() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(product("PH-1001", "memory", "Kingston", "FURY Beast 16GB", 5499, 20,
                spec("standard", "DDR5", "sizeGb", "16", "mhz", "5600")));
        list.add(product("PH-1002", "memory", "Corsair", "Vengeance 32GB", 10999, 11,
                spec("standard", "DDR5", "sizeGb", "32", "mhz", "6000")));
        list.add(product("PH-1003", "memory", "Crucial", "Ballistix 16GB", 3999, 14,
                spec("standard", "DDR4", "sizeGb", "16", "mhz", "3200")));
        list.add(product("PH-2001", "graphics", "NVIDIA", "GeForce RTX 4060", 29999, 9,
                spec("memoryGb", "8", "lengthMm", "240", "boardPower", "115")));
        list.add(product("PH-2002", "graphics", "AMD", "Radeon RX 7800 XT", 49999, 5,
                spec("memoryGb", "16", "lengthMm", "267", "boardPower", "263")));
        list.add(product("PH-2003", "graphics", "NVIDIA", "GeForce RTX 4090", 179999, 2,
                spec("memoryGb", "24", "lengthMm", "336", "boardPower", "450")));
        list.add(product("PH-3001", "drive", "Samsung", "990 PRO 1TB", 10999, 18,
                spec("interface", "NVMe", "sizeGb", "1000")));
        list.add(product("PH-3002", "drive", "WD", "Blue SN580 500GB", 4999, 25,
                spec("interface", "NVMe", "sizeGb", "500")));
        list.add(product("PH-3003", "drive", "Seagate", "BarraCuda 2TB", 5999, 8,
                spec("interface", "HDD", "sizeGb", "2000")));
        list.add(product("PH-4001", "chassis", "NZXT", "H5 Flow", 9499, 10,
                spec("boardSupport", "ATX", "gpuClearanceMm", "365")));
        list.add(product("PH-4002", "chassis", "Cooler Master", "Q300L", 4999, 12,
                spec("boardSupport", "mATX", "gpuClearanceMm", "360")));
        list.add(product("PH-4003", "chassis", "Fractal", "Terra Mini", 17999, 3,
                spec("boardSupport", "ITX", "gpuClearanceMm", "322")));
        return list;
    }

    private static Map<String, Object> product(String id, String category, String manufacturer,
                                               String name, int priceCents, int qty,
                                               Map<String, String> specs) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", id);
        p.put("category", category);
        p.put("manufacturer", manufacturer);
        p.put("name", name);
        p.put("priceCents", priceCents);
        p.put("qty", qty);
        p.put("specs", specs);
        return p;
    }

    private static Map<String, String> spec(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
