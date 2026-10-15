package fraud.detection.engine;

import fraud.detection.ml.Features;
import fraud.detection.model.Cities;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.TransactionRequest;

/** Turns a transaction + the user's profile into the 9 numbers the detectors use. */
public final class FeatureExtractor {

    private FeatureExtractor() {
    }

    public static double[] extract(TransactionRequest req, UserProfile profile, DetectionConfig config) {
        double amount = req.getAmount().doubleValue();
        double balance = profile.getBalance().doubleValue();
        double[] x = new double[Features.COUNT];
        x[Features.AMOUNT_ZSCORE] = clamp((amount - profile.effectiveMean()) / profile.effectiveStd(), -3, 10);
        x[Features.LOG_AMOUNT] = Math.log1p(amount);
        x[Features.BALANCE_RATIO] = balance > 0 ? Math.min(2, amount / balance) : 2;
        x[Features.NIGHT_TIME] = config.isNightHour(req.getTime().getHour()) ? 1 : 0;
        x[Features.NEW_CITY] = profile.isNewCity(req.getCity()) ? 1 : 0;
        x[Features.NEW_DEVICE] = profile.isNewDevice(req.getDevice()) ? 1 : 0;
        x[Features.VELOCITY] = Math.min(20, profile.getLastHourCount());
        x[Features.MERCHANT_RISK] = req.getCategory().getRisk();
        x[Features.INTERNATIONAL] = Cities.isInternational(req.getCity()) ? 1 : 0;
        return x;
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
