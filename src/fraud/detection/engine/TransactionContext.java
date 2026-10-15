package fraud.detection.engine;

import fraud.detection.model.DetectionConfig;
import fraud.detection.model.TransactionRequest;

/** Everything a detector needs to judge one transaction (immutable, safe to share between threads). */
public final class TransactionContext {

    private final TransactionRequest request;
    private final UserProfile profile;
    private final DetectionConfig config;
    private final double[] features;

    public TransactionContext(TransactionRequest request, UserProfile profile, DetectionConfig config, double[] features) {
        this.request = request;
        this.profile = profile;
        this.config = config;
        this.features = features.clone();
    }

    public TransactionRequest getRequest() { return request; }
    public UserProfile getProfile()        { return profile; }
    public DetectionConfig getConfig()     { return config; }

    public double feature(int index) {
        return features[index];
    }

    public double[] getFeatures() {
        return features.clone();
    }
}
