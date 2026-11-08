package com.byteforge.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * COMMAND pattern - the <b>Invoker</b>.
 * Executes commands and keeps two stacks for undo and redo. A new command clears
 * the redo stack, exactly like in a text editor.
 */
public class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final int limit;

    public CommandHistory(int limit) {
        this.limit = limit;
    }

    public void execute(Command c) {
        c.execute();
        record(c);
    }

    /** Stores a command whose effect has already happened (e.g. the user changed a combo box). */
    public void record(Command c) {
        undoStack.push(c);
        if (undoStack.size() > limit) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public Command undo() {
        if (undoStack.isEmpty()) return null;
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        return c;
    }

    public Command redo() {
        if (redoStack.isEmpty()) return null;
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
        return c;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public String peekUndoName() { return canUndo() ? undoStack.peek().getName() : null; }
    public String peekRedoName() { return canRedo() ? redoStack.peek().getName() : null; }
    public int size() { return undoStack.size(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
