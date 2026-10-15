package fraud.detection.engine;

import fraud.detection.model.DeviceType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * What "normal" looks like for one customer, built from their past approved
 * transactions. Fraud is mostly detected as a DEVIATION from this profile.
 */
public final class UserProfile {

    /** With fewer past transactions than this, the population average is used instead. */
    public static final int MIN_HISTORY = 3;
    public static final double POPULATION_MEAN = 3000;
    public static final double POPULATION_STD = 4000;

    private final int historyCount;
    private final double meanAmount;
    private final double stdAmount;
    private final Set<String> knownCities;       // HashSet - fast contains()
    private final Set<DeviceType> knownDevices;  // EnumSet - a Set made for enum values
    private final int lastHourCount;
    private final BigDecimal balance;
    private final String homeCity;

    public UserProfile(int historyCount, double meanAmount, double stdAmount, Set<String> cities,
                       Set<DeviceType> devices, int lastHourCount, BigDecimal balance, String homeCity) {
        this.historyCount = historyCount;
        this.meanAmount = meanAmount;
        this.stdAmount = stdAmount;
        Set<String> c = new HashSet<>(cities);
        c.add(homeCity);
        this.knownCities = Collections.unmodifiableSet(c);
        this.knownDevices = devices.isEmpty() ? EnumSet.noneOf(DeviceType.class) : EnumSet.copyOf(devices);
        this.lastHourCount = lastHourCount;
        this.balance = balance;
        this.homeCity = homeCity;
    }

    public boolean hasHistory() {
        return historyCount >= MIN_HISTORY;
    }

    /** Average used for comparison - the user's own, or the population's for new users. */
    public double effectiveMean() {
        return hasHistory() ? meanAmount : POPULATION_MEAN;
    }

    /** Spread used for comparison. A minimum is applied so a very regular user is not over-sensitive. */
    public double effectiveStd() {
        if (!hasHistory()) {
            return POPULATION_STD;
        }
        return Math.max(stdAmount, Math.max(500, 0.25 * meanAmount));
    }

    public boolean isNewCity(String city) {
        return !knownCities.contains(city);
    }

    public boolean isNewDevice(DeviceType device) {
        return hasHistory() && !knownDevices.contains(device);
    }

    public int getHistoryCount()          { return historyCount; }
    public double getMeanAmount()         { return meanAmount; }
    public double getStdAmount()          { return stdAmount; }
    public Set<String> getKnownCities()   { return knownCities; }
    public Set<DeviceType> getKnownDevices() { return Collections.unmodifiableSet(knownDevices); }
    public int getLastHourCount()         { return lastHourCount; }
    public BigDecimal getBalance()        { return balance; }
    public String getHomeCity()           { return homeCity; }
}
