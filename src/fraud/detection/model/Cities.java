package fraud.detection.model;

import java.util.List;

/** Locations offered in the forms. Cities marked "(International)" set the international feature. */
public final class Cities {

    public static final String INTERNATIONAL_TAG = "(International)";

    /** Immutable List - nobody can accidentally add or remove a city at runtime. */
    public static final List<String> ALL = List.of(
            "Greater Noida", "Noida", "Delhi", "Gurugram", "Mumbai", "Pune", "Bengaluru", "Hyderabad",
            "Chennai", "Kolkata", "Jaipur", "Lucknow", "Ahmedabad", "Chandigarh",
            "Dubai " + INTERNATIONAL_TAG, "Singapore " + INTERNATIONAL_TAG, "London " + INTERNATIONAL_TAG,
            "Lagos " + INTERNATIONAL_TAG, "Moscow " + INTERNATIONAL_TAG);

    /** Only the Indian cities - used as home cities. */
    public static final List<String> DOMESTIC = ALL.subList(0, 14);

    private Cities() {
    }

    public static boolean isInternational(String city) {
        return city != null && city.contains(INTERNATIONAL_TAG);
    }
}
