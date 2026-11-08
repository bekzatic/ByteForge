package com.byteforge.command;

import com.byteforge.model.Component;
import com.byteforge.model.ComponentType;

import java.util.ArrayList;
import java.util.List;

/**
 * COMMAND pattern - the <b>Concrete Commands</b>.
 */
public final class ConfigCommands {
    private ConfigCommands() {}

    /** Replaces the part of one category; remembers the previous part for undo. */
    public static class SelectPart implements Command {
        private final ConfigurationEditor editor;
        private final ComponentType type;
        private final Component oldPart;
        private final Component newPart;

        public SelectPart(ConfigurationEditor editor, ComponentType type, Component oldPart, Component newPart) {
            this.editor = editor;
            this.type = type;
            this.oldPart = oldPart;
            this.newPart = newPart;
        }

        /** Convenience: reads the current part from the editor as the "old" value. */
        public SelectPart(ConfigurationEditor editor, ComponentType type, Component newPart) {
            this(editor, type, editor.getPart(type), newPart);
        }

        @Override public void execute() { editor.setPart(type, newPart); }
        @Override public void undo()    { editor.setPart(type, oldPart); }
        @Override public String getName() {
            return type.label() + " -> " + (newPart == null ? "none" : newPart.getName());
        }
    }

    /** Changes the number of RAM modules. */
    public static class ChangeRamModules implements Command {
        private final ConfigurationEditor editor;
        private final int oldValue;
        private final int newValue;

        public ChangeRamModules(ConfigurationEditor editor, int oldValue, int newValue) {
            this.editor = editor;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override public void execute() { editor.setRamModules(newValue); }
        @Override public void undo()    { editor.setRamModules(oldValue); }
        @Override public String getName() { return "RAM modules " + oldValue + " -> " + newValue; }
    }

    /**
     * Macro command: a group of commands executed and undone as one step
     * (used for "Load preset", which changes up to eight parts at once).
     */
    public static class Macro implements Command {
        private final String name;
        private final List<Command> steps = new ArrayList<>();

        public Macro(String name) {
            this.name = name;
        }

        public Macro add(Command c) {
            steps.add(c);
            return this;
        }

        public boolean isEmpty() {
            return steps.isEmpty();
        }

        @Override public void execute() {
            for (Command c : steps) c.execute();
        }

        @Override public void undo() {
            for (int i = steps.size() - 1; i >= 0; i--) steps.get(i).undo();
        }

        @Override public String getName() { return name; }
    }
}
