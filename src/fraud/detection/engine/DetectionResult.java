package fraud.detection.engine;

import java.util.Collections;
import java.util.List;

/** A detector's answer: a risk score from 0 (safe) to 1 (certainly fraud) and the reasons. */
public class DetectionResult {

    private final String detectorName;
    private final double score;
    private final List<String> reasons;

    public DetectionResult(String detectorName, double score, List<String> reasons) {
        this.detectorName = detectorName;
        this.score = score;
        this.reasons = Collections.unmodifiableList(reasons);
    }

    public String getDetectorName() { return detectorName; }
    public double getScore()        { return score; }
    public List<String> getReasons() { return reasons; }
}
