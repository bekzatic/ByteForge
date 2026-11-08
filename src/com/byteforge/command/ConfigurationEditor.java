package com.byteforge.command;

import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;

/**
 * COMMAND pattern - the <b>Receiver</b>.
 * The object that actually holds the current selection (the GUI implements it;
 * tests use a simple in-memory version).
 */
public interface ConfigurationEditor {
    Component getPart(ComponentType type);

    void setPart(ComponentType type, Component part);

    int getRamModules();

    void setRamModules(int modules);
}
