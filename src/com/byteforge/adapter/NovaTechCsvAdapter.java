package com.byteforge.adapter;

import com.byteforge.factory.ComponentRecord;
import com.byteforge.model.ComponentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ADAPTER pattern - <b>Adapter #1</b> (object adapter, wraps {@link NovaTechCsvService}).
 * Converts the distributor's CSV text into {@link ComponentRecord}s.
 */
public class NovaTechCsvAdapter implements SupplierFeed {

    private final NovaTechCsvService service;

    public NovaTechCsvAdapter(NovaTechCsvService service) {
        this.service = service;
    }

    @Override
    public String getSupplierName() {
        return "NovaTech Distribution (CSV)";
    }

    @Override
    public List<ComponentRecord> fetchRecords() {
        List<ComponentRecord> records = new ArrayList<>();
        for (String line : service.exportCatalogCsv().split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split(";");
            Map<String, String> attrs = new LinkedHashMap<>();
            for (String pair : f[6].split("\\|")) {
                String[] kv = pair.split("=", 2);
                attrs.put(kv[0], kv[1]);
            }
            records.add(new ComponentRecord(
                    f[0], ComponentType.valueOf(f[1]), f[2], f[3],
                    Double.parseDouble(f[4]), Integer.parseInt(f[5]), attrs));
        }
        return records;
    }
}
