package fraud.detection.ml;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the ACTIVE model that scores every transaction.
 *
 * AtomicReference makes switching to a newly trained model ATOMIC: a transaction
 * being scored at that moment uses either the old model or the new one - never a
 * half-updated mix - and no lock is needed for reading.
 */
public final class ModelRegistry {

    private static final AtomicReference<ModelVersion> ACTIVE = new AtomicReference<>();

    private ModelRegistry() {
    }

    public static ModelVersion getActive() {
        return ACTIVE.get();
    }

    public static void setActive(ModelVersion version) {
        ACTIVE.set(version);
    }
}
