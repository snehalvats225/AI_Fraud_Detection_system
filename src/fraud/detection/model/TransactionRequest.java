package fraud.detection.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the customer typed in the "New Transaction" form (immutable). */
public final class TransactionRequest {

    private final TransactionType type;
    private final String payee;
    private final BigDecimal amount;
    private final MerchantCategory category;
    private final String city;
    private final DeviceType device;
    private final LocalDateTime time;

    public TransactionRequest(TransactionType type, String payee, BigDecimal amount, MerchantCategory category,
                              String city, DeviceType device, LocalDateTime time) {
        this.type = type;
        this.payee = payee;
        this.amount = amount;
        this.category = category;
        this.city = city;
        this.device = device;
        this.time = time;
    }

    public TransactionType getType()        { return type; }
    public String getPayee()                { return payee; }
    public BigDecimal getAmount()           { return amount; }
    public MerchantCategory getCategory()   { return category; }
    public String getCity()                 { return city; }
    public DeviceType getDevice()           { return device; }
    public LocalDateTime getTime()          { return time; }
}
