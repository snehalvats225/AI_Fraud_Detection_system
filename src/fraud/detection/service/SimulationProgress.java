package fraud.detection.service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live counters of a running simulation. Worker threads INCREMENT them while
 * the UI thread READS them - AtomicInteger makes that safe without locks.
 */
public final class SimulationProgress {

    public final AtomicInteger done = new AtomicInteger();
    public final AtomicInteger approved = new AtomicInteger();
    public final AtomicInteger flagged = new AtomicInteger();
    public final AtomicInteger blocked = new AtomicInteger();
    public final AtomicInteger skipped = new AtomicInteger();
    public final AtomicInteger injectedFraud = new AtomicInteger();
    public final AtomicInteger caughtFraud = new AtomicInteger();
    public final AtomicInteger falseAlarms = new AtomicInteger();
    public final AtomicInteger genuine = new AtomicInteger();

    private volatile boolean cancelled;     // volatile: a change by the UI thread is seen by workers at once

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
