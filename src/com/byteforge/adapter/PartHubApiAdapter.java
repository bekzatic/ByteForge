package com.byteforge.adapter;

import com.byteforge.factory.ComponentRecord;
import com.byteforge.model.ComponentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ADAPTER pattern - <b>Adapter #2</b> (object adapter, wraps {@link PartHubApiClient}).
 * Translates the API's vocabulary (category names, cents, spec keys) into ours.
 */
public class PartHubApiAdapter implements SupplierFeed {

    private final PartHubApiClient client;

    public PartHubApiAdapter(PartHubApiClient client) {
        this.client = client;
    }

    @Override
    public String getSupplierName() {
        return "PartHub REST API (JSON)";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ComponentRecord> fetchRecords() {
        List<ComponentRecord> records = new ArrayList<>();
        for (Map<String, Object> p : client.getProducts()) {
            ComponentType type = mapCategory((String) p.get("category"));
            Map<String, String> specs = (Map<String, String>) p.get("specs");
            records.add(new ComponentRecord(
                    (String) p.get("id"),
                    type,
                    (String) p.get("manufacturer"),
                    (String) p.get("name"),
                    ((Integer) p.get("priceCents")) / 100.0,
                    (Integer) p.get("qty"),
                    mapSpecs(type, specs)));
        }
        return records;
    }

    private static ComponentType mapCategory(String category) {
        switch (category) {
            case "memory":   return ComponentType.RAM;
            case "graphics": return ComponentType.GPU;
            case "drive":    return ComponentType.STORAGE;
            case "chassis":  return ComponentType.CASE;
            default: throw new IllegalArgumentException("Unknown PartHub category: " + category);
        }
    }

    /** Renames supplier-specific spec keys to the attribute names the factory expects. */
    private static Map<String, String> mapSpecs(ComponentType type, Map<String, String> s) {
        Map<String, String> a = new LinkedHashMap<>();
        switch (type) {
            case RAM:
                a.put("ramType", s.get("standard"));
                a.put("capacityGb", s.get("sizeGb"));
                a.put("speedMhz", s.get("mhz"));
                break;
            case GPU:
                a.put("vramGb", s.get("memoryGb"));
                a.put("lengthMm", s.get("lengthMm"));
                a.put("tdp", s.get("boardPower"));
                break;
            case STORAGE:
                a.put("kind", s.get("interface"));
                a.put("capacityGb", s.get("sizeGb"));
                break;
            case CASE:
                a.put("formFactor", s.get("boardSupport"));
                a.put("maxGpuLengthMm", s.get("gpuClearanceMm"));
                break;
            default:
                break;
        }
        return a;
    }
}
