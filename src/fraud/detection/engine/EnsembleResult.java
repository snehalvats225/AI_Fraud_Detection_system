package fraud.detection.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The combined answer. It EXTENDS DetectionResult, so it can be used anywhere a
 * DetectionResult is expected, and adds each individual detector's result.
 */
public class EnsembleResult extends DetectionResult {

    private final Map<String, DetectionResult> components;

    public EnsembleResult(String name, double score, List<String> reasons, Map<String, DetectionResult> components) {
        super(name, score, reasons);
        this.components = Collections.unmodifiableMap(components);
    }

    public Map<String, DetectionResult> getComponents() {
        return components;
    }

    /** Score of one detector, or 0 if it did not run. */
    public double componentScore(String detectorName) {
        DetectionResult r = components.get(detectorName);
        return r == null ? 0 : r.getScore();
    }
}
