package dev.archunitjava.rules.fixtures.cycles.left;

import dev.archunitjava.rules.fixtures.cycles.right.CycleRight;

/** Compiled by the test build; imported as bytes, never loaded or executed by the analyzer. */
public final class CycleLeft {
    private CycleRight right;

    public void work() {
        right.work();
    }

    public Runnable deferred() {
        return () -> right.work();
    }

    public static final class OneWay {
        private CycleRight.Leaf leaf;

        public void work() {
            leaf.work();
        }
    }
}
