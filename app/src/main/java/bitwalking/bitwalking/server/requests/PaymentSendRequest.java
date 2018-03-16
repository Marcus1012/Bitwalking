package bitwalking.bitwalking.server.requests;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexey on 28.05.17.
 */

public class PaymentSendRequest extends JsonRequest {
    public String note;
    public Payee payee;
    public TransactionInformation transactionInformation;
    public List<PaymentTransferInfo> transfer = new ArrayList<>();


    public static class Payee{
        public String identityType="email";
        public String identity;
        public AdditionalInformation additionalInformation;
    }
    public static class AdditionalInformation{
        public String name;
    }
    public static class PaymentTransferInfo {
        public String currency = "xwd";
        public Double amount;
    }

    public static class TransactionInformation {
        public String currency = "xwd";
        public Double sum;
    }

}
