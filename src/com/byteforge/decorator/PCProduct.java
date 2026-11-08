package com.byteforge.decorator;

import java.util.List;

/**
 * DECORATOR pattern - the <b>Component</b> interface.
 * Anything that can be sold as a finished PC: the bare machine or the machine
 * wrapped in any number of service add-ons.
 */
public interface PCProduct {
    String getDescription();

    double getPrice();

    /** One line per priced item, used for the receipt. */
    List<String> getLineItems();
}
