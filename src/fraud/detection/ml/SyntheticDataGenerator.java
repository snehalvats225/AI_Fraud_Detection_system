package fraud.detection.ml;

import fraud.detection.model.MerchantCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates realistic training examples so the model can be trained on the very
 * first run, before any real feedback exists ("cold start").
 *
 * Genuine transactions: normal amounts, daytime, known city/device, low-risk shops.
 * Fraudulent ones: unusually large amounts, night-time, new city/device, bursts of
 * transactions, gift cards / crypto / gambling, foreign locations.
 * The two groups OVERLAP (some genuine look odd, some fraud looks normal), so the
 * model cannot be 100% perfect - just like real life.
 *
 * A fixed seed makes the data identical on every run (reproducible results).
 */
public final class SyntheticDataGenerator {

    public static final long DEFAULT_SEED = 20260426L;
    public static final double SYNTHETIC_WEIGHT = 1.0;

    private static final MerchantCategory[] LOW_RISK = {
        MerchantCategory.GROCERY, MerchantCategory.UTILITIES, MerchantCategory.FOOD_DINING,
        MerchantCategory.FUEL, MerchantCategory.SHOPPING, MerchantCategory.TRAVEL};
    private static final MerchantCategory[] HIGH_RISK = {
        MerchantCategory.ELECTRONICS, MerchantCategory.MONEY_TRANSFER, MerchantCategory.GIFT_CARDS,
        MerchantCategory.GAMBLING, MerchantCategory.CRYPTO};

    private SyntheticDataGenerator() {
    }

    public static List<LabeledSample> generate(int count, double fraudRatio, long seed) {
        Random rnd = new Random(seed);
        List<LabeledSample> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            boolean fraud = rnd.nextDouble() < fraudRatio;
            list.add(new LabeledSample(fraud ? fraud(rnd) : genuine(rnd), fraud ? 1 : 0, SYNTHETIC_WEIGHT, false));
        }
        return list;
    }

    private static double[] genuine(Random r) {
        double[] x = new double[Features.COUNT];
        double amount = Math.exp(Math.log(2500) + 0.9 * r.nextGaussian());
        double balance = Math.exp(Math.log(150000) + 0.8 * r.nextGaussian());
        x[Features.AMOUNT_ZSCORE] = clamp(r.nextGaussian() * 1.3, -3, 10);
        x[Features.LOG_AMOUNT] = Math.log1p(amount);
        x[Features.BALANCE_RATIO] = Math.min(2, amount / balance);
        x[Features.NIGHT_TIME] = r.nextDouble() < 0.08 ? 1 : 0;
        boolean international = r.nextDouble() < 0.02;
        x[Features.INTERNATIONAL] = international ? 1 : 0;
        x[Features.NEW_CITY] = international || r.nextDouble() < 0.10 ? 1 : 0;
        x[Features.NEW_DEVICE] = r.nextDouble() < 0.10 ? 1 : 0;
        x[Features.VELOCITY] = Math.min(20, poisson(r, 0.9));
        MerchantCategory c = r.nextDouble() < 0.86 ? pick(r, LOW_RISK) : pick(r, HIGH_RISK);
        x[Features.MERCHANT_RISK] = c.getRisk();
        return x;
    }

    private static double[] fraud(Random r) {
        double[] x = new double[Features.COUNT];
        double amount = Math.exp(Math.log(18000) + 1.1 * r.nextGaussian());
        double balance = Math.exp(Math.log(150000) + 0.8 * r.nextGaussian());
        double ratio = r.nextDouble() < 0.35 ? 0.6 + 0.4 * r.nextDouble() : amount / balance;
        x[Features.AMOUNT_ZSCORE] = clamp(3.0 + 2.5 * r.nextGaussian(), -3, 10);
        x[Features.LOG_AMOUNT] = Math.log1p(amount);
        x[Features.BALANCE_RATIO] = Math.min(2, ratio);
        x[Features.NIGHT_TIME] = r.nextDouble() < 0.35 ? 1 : 0;
        boolean international = r.nextDouble() < 0.22;
        x[Features.INTERNATIONAL] = international ? 1 : 0;
        x[Features.NEW_CITY] = international || r.nextDouble() < 0.45 ? 1 : 0;
        x[Features.NEW_DEVICE] = r.nextDouble() < 0.45 ? 1 : 0;
        x[Features.VELOCITY] = Math.min(20, poisson(r, 2.5));
        MerchantCategory c = r.nextDouble() < 0.55 ? pick(r, HIGH_RISK) : pick(r, LOW_RISK);
        x[Features.MERCHANT_RISK] = c.getRisk();
        return x;
    }

    private static MerchantCategory pick(Random r, MerchantCategory[] options) {
        return options[r.nextInt(options.length)];
    }

    /** Knuth's method for a Poisson-distributed random count. */
    private static int poisson(Random r, double lambda) {
        double limit = Math.exp(-lambda);
        double p = 1;
        int k = 0;
        do {
            k++;
            p *= r.nextDouble();
        } while (p > limit);
        return k - 1;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
