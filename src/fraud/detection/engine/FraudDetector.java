package fraud.detection.engine;

/**
 * INTERFACE every detection algorithm implements.
 * The engine only knows this interface, so a new algorithm can be added without
 * changing the engine - just write a new class that implements it (POLYMORPHISM).
 */
public interface FraudDetector {

    String getName();

    /** Short description shown on the Algorithm Management screen. */
    String getDescription();

    DetectionResult evaluate(TransactionContext context);
}
