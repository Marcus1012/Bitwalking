package bitwalking.bitwalking.transactions;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by Marcus on 10/25/16.
 */

public class PaymentRequest implements Comparable<PaymentRequest> {
    public String id;
    public String updateTimestamp;
    public String creationTimestamp;
    public String type;
    public String sender;
    public String recipient;
    public String transactionId;
    public PaymentStatus status;
    public ArrayList<PaymentTransferInfo> transfer;

    @Override
    public int compareTo(PaymentRequest another) {
        return creationTimestamp.compareTo(another.creationTimestamp);
    }

    public static class PaymentTransferInfo {
        public String currency;
        public BigDecimal amount;
    }

    public enum PaymentStatus {
        pending,
        paid,
        rejected,
        canceled
    }
}
