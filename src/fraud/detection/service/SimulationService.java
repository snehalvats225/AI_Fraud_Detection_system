package fraud.detection.service;

import fraud.detection.dao.UserDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.InsufficientBalanceException;
import fraud.detection.model.Cities;
import fraud.detection.model.Customer;
import fraud.detection.model.Decision;
import fraud.detection.model.DeviceType;
import fraud.detection.model.MerchantCategory;
import fraud.detection.model.TransactionRequest;
import fraud.detection.model.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * TRAFFIC SIMULATOR: many customers paying at the same time.
 *
 * MULTITHREADING: a thread pool (ExecutorService) submits transactions in
 * parallel through the REAL pipeline (TransactionService), exactly like many
 * users would. Each thread uses ThreadLocalRandom (a random generator per thread,
 * no sharing, no locking). Results are counted in AtomicIntegers.
 *
 * The simulator knows which transactions it made fraudulent, so it can measure
 * how many the system caught (recall) and how many genuine ones it wrongly
 * stopped (false alarms).
 */
public class SimulationService {

    private static final String[] GENUINE_PAYEES = {"BigBasket", "Swiggy", "Zomato", "Indian Oil", "Amazon",
        "Flipkart", "Airtel", "Tata Power", "IRCTC", "DMart", "Reliance Trends", "Uber"};
    private static final String[] FRAUD_PAYEES = {"QuickCash Wallet", "CryptoX Exchange", "Gift Card Hub",
        "LuckyBet Online", "Unknown Merchant 4471", "FastTransfer Ltd", "Intl Electronics FZE"};
    private static final MerchantCategory[] LOW = {MerchantCategory.GROCERY, MerchantCategory.FOOD_DINING,
        MerchantCategory.FUEL, MerchantCategory.SHOPPING, MerchantCategory.UTILITIES, MerchantCategory.TRAVEL};
    private static final MerchantCategory[] HIGH = {MerchantCategory.CRYPTO, MerchantCategory.GIFT_CARDS,
        MerchantCategory.GAMBLING, MerchantCategory.MONEY_TRANSFER, MerchantCategory.ELECTRONICS};

    private final TransactionService txnService = new TransactionService();

    public long run(int count, double fraudRatio, int threads, boolean storeGroundTruth, SimulationProgress progress)
            throws FraudSystemException, InterruptedException {
        List<Customer> customers = new UserDAO().findAll();
        if (customers.isEmpty()) {
            throw new FraudSystemException("No customers exist yet. Register at least one customer first.");
        }
        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "simulator");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                tasks.add(() -> {
                    if (progress.isCancelled()) {
                        return null;
                    }
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    Customer c = customers.get(rnd.nextInt(customers.size()));
                    boolean fraud = rnd.nextDouble() < fraudRatio;
                    TransactionRequest req = fraud ? fraudulent(c, rnd) : genuine(c, rnd);
                    try {
                        Decision d = txnService.process(c.getUserId(), req, storeGroundTruth ? (fraud ? 1 : 0) : null)
                                .getAssessment().getDecision();
                        count(progress, d, fraud);
                    } catch (InsufficientBalanceException e) {
                        progress.skipped.incrementAndGet();
                    } finally {
                        progress.done.incrementAndGet();
                    }
                    return null;
                });
            }
            pool.invokeAll(tasks);
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
        return System.currentTimeMillis() - start;
    }

    private static void count(SimulationProgress p, Decision d, boolean fraud) {
        switch (d) {
            case APPROVED: p.approved.incrementAndGet(); break;
            case FLAGGED:  p.flagged.incrementAndGet(); break;
            default:       p.blocked.incrementAndGet(); break;
        }
        if (fraud) {
            p.injectedFraud.incrementAndGet();
            if (d != Decision.APPROVED) {
                p.caughtFraud.incrementAndGet();
            }
        } else {
            p.genuine.incrementAndGet();
            if (d != Decision.APPROVED) {
                p.falseAlarms.incrementAndGet();
            }
        }
    }

    /** Normal behaviour: usual amounts, daytime, home city, the customer's usual app. */
    private static TransactionRequest genuine(Customer c, ThreadLocalRandom r) {
        double base = 1500 + (c.getUserId() % 5) * 700;               // each customer has a typical spend
        double amount = Math.exp(Math.log(base) + 0.6 * r.nextGaussian());
        String city = r.nextDouble() < 0.88 ? c.getHomeCity() : Cities.DOMESTIC.get(r.nextInt(Cities.DOMESTIC.size()));
        DeviceType device = r.nextDouble() < 0.85
                ? (c.getUserId() % 2 == 0 ? DeviceType.UPI_APP : DeviceType.MOBILE_APP) : DeviceType.POS_TERMINAL;
        LocalDateTime time = LocalDateTime.now().minusDays(r.nextInt(0, 7)).withHour(r.nextInt(8, 23))
                .withMinute(r.nextInt(60)).withSecond(r.nextInt(60)).withNano(0);
        if (time.isAfter(LocalDateTime.now())) {
            time = time.minusDays(1);
        }
        MerchantCategory cat = r.nextDouble() < 0.93 ? LOW[r.nextInt(LOW.length)] : HIGH[r.nextInt(HIGH.length)];
        return new TransactionRequest(r.nextBoolean() ? TransactionType.UPI_PAYMENT : TransactionType.CARD_PAYMENT,
                GENUINE_PAYEES[r.nextInt(GENUINE_PAYEES.length)], money(amount), cat, city, device, time);
    }

    /** Typical fraud: large amount, night, new/foreign city, new device, risky merchant. */
    private static TransactionRequest fraudulent(Customer c, ThreadLocalRandom r) {
        double amount = Math.exp(Math.log(30000) + 0.8 * r.nextGaussian());
        String city = r.nextDouble() < 0.35
                ? Cities.ALL.get(Cities.DOMESTIC.size() + r.nextInt(Cities.ALL.size() - Cities.DOMESTIC.size()))
                : Cities.DOMESTIC.get(r.nextInt(Cities.DOMESTIC.size()));
        DeviceType device = r.nextBoolean() ? DeviceType.NET_BANKING : DeviceType.ATM;
        int hour = r.nextDouble() < 0.5 ? r.nextInt(0, 5) : r.nextInt(8, 23);
        LocalDateTime time = LocalDateTime.now().minusDays(r.nextInt(0, 7)).withHour(hour)
                .withMinute(r.nextInt(60)).withSecond(r.nextInt(60)).withNano(0);
        if (time.isAfter(LocalDateTime.now())) {
            time = time.minusDays(1);
        }
        MerchantCategory cat = r.nextDouble() < 0.65 ? HIGH[r.nextInt(HIGH.length)] : LOW[r.nextInt(LOW.length)];
        TransactionType type = device == DeviceType.ATM ? TransactionType.ATM_WITHDRAWAL : TransactionType.ONLINE_PURCHASE;
        return new TransactionRequest(type, FRAUD_PAYEES[r.nextInt(FRAUD_PAYEES.length)], money(amount), cat, city,
                device, time);
    }

    private static BigDecimal money(double amount) {
        return BigDecimal.valueOf(Math.max(50, Math.min(900000, amount))).setScale(2, RoundingMode.HALF_UP);
    }
}
