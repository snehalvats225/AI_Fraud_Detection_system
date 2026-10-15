package fraud.detection.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    private DateUtil() {
    }

    public static String format(LocalDateTime t) {
        return t == null ? "" : t.format(FULL);
    }

    public static String formatShort(LocalDateTime t) {
        return t == null ? "" : t.format(SHORT);
    }
}
