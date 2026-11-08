package com.byteforge.command;

/**
 * COMMAND pattern - the <b>Command</b> interface.
 * <p>
 * Every change the customer makes to a configuration is wrapped in an object that
 * knows how to do it and how to undo it. The {@link CommandHistory} (the invoker)
 * keeps these objects, which gives the configurator unlimited undo/redo.
 */
public interface Command {
    void execute();

    void undo();

    /** Short text shown on the Undo/Redo buttons and in the log. */
    String getName();
}
