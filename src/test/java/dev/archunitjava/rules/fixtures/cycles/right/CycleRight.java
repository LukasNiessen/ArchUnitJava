package dev.archunitjava.rules.fixtures.cycles.right;

import dev.archunitjava.rules.fixtures.cycles.left.CycleLeft;

/** Compiled by the test build; imported as bytes, never loaded or executed by the analyzer. */
public final class CycleRight {
    private CycleLeft left;

    public void work() {
        left.work();
    }

    public static final class Leaf {
        public void work() {}
    }
}
