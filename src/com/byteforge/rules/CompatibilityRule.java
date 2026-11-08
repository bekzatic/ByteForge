package com.byteforge.rules;

import com.byteforge.builder.PCBuilder;

import java.util.List;

/**
 * CHAIN OF RESPONSIBILITY pattern - the <b>Handler</b>.
 * <p>
 * Each hardware rule is one link of a chain. A link checks its own concern, records
 * any problem and passes the request to the next link. The builder only talks to the
 * first link, so rules can be added, removed or reordered without touching the builder
 * or the other rules.
 */
public abstract class CompatibilityRule {

    private CompatibilityRule next;

    /** Links the next rule and returns it, so a chain can be written fluently: a.then(b).then(c). */
    public CompatibilityRule then(CompatibilityRule next) {
        this.next = next;
        return next;
    }

    /** Runs this rule and then the rest of the chain. */
    public final void handle(PCBuilder build, List<String> issues) {
        check(build, issues);
        if (next != null && shouldContinue(build, issues)) {
            next.handle(build, issues);
        }
    }

    /** The rule itself. */
    protected abstract void check(PCBuilder build, List<String> issues);

    /** A rule may stop the chain; by default every rule runs so the customer sees all problems. */
    protected boolean shouldContinue(PCBuilder build, List<String> issues) {
        return true;
    }

    /** Short name shown in the report and tests. */
    public abstract String getName();

    public CompatibilityRule getNext() {
        return next;
    }
}
