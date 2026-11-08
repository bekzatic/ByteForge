package com.byteforge.adapter;

import com.byteforge.factory.ComponentRecord;

import java.util.List;

/**
 * ADAPTER pattern - the <b>Target</b> interface.
 * <p>
 * The store only understands suppliers that speak this interface. Every real supplier
 * has its own incompatible format, so each one is wrapped in an adapter.
 */
public interface SupplierFeed {
    String getSupplierName();

    List<ComponentRecord> fetchRecords();
}
