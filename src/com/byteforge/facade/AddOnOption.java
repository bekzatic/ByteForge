package com.byteforge.facade;

/** Add-ons a customer can tick; the facade turns them into decorators. */
public enum AddOnOption {
    OS_INSTALL("Windows 11 Pro + drivers ($139)"),
    RGB("ARGB lighting kit ($49)"),
    OVERCLOCK("Pro overclock & tuning ($79)"),
    WARRANTY("Extended warranty +2 yr (6%/yr)");

    private final String label;

    AddOnOption(String label) { this.label = label; }

    public String label() { return label; }
}
