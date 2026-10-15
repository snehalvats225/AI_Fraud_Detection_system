package fraud.detection.engine;

import fraud.detection.model.Decision;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Live performance counters of the detection engine ("monitoring system performance").
 * Many threads score transactions at the same time, so the counters are
 * THREAD-SAFE: LongAdder for totals, LongAccumulator(Math::max) for the slowest call.
 * No synchronized block is needed for these.
 */
public final class PerformanceMonitor {

    private static final LongAdder SCORED = new LongAdder();
    private static final LongAdder APPROVED = new LongAdder();
    private static final LongAdder FLAGGED = new LongAdder();
    private static final LongAdder BLOCKED = new LongAdder();
    private static final LongAdder TOTAL_NANOS = new LongAdder();
    private static final LongAccumulator MAX_NANOS = new LongAccumulator(Math::max, 0);
    private static final long STARTED = System.currentTimeMillis();

    private PerformanceMonitor() {
    }

    public static void record(Decision decision, long nanos) {
        SCORED.increment();
        TOTAL_NANOS.add(nanos);
        MAX_NANOS.accumulate(nanos);
        switch (decision) {
            case APPROVED: APPROVED.increment(); break;
            case FLAGGED:  FLAGGED.increment(); break;
            default:       BLOCKED.increment(); break;
        }
    }

    public static long scored()    { return SCORED.sum(); }
    public static long approved()  { return APPROVED.sum(); }
    public static long flagged()   { return FLAGGED.sum(); }
    public static long blocked()   { return BLOCKED.sum(); }

    public static double averageMillis() {
        long n = SCORED.sum();
        return n == 0 ? 0 : TOTAL_NANOS.sum() / 1e6 / n;
    }

    public static double maxMillis() {
        return MAX_NANOS.get() / 1e6;
    }

    public static long uptimeSeconds() {
        return (System.currentTimeMillis() - STARTED) / 1000;
    }
}
