package bitwalking.bitwalking.user_info;

import java.math.BigDecimal;

/**
 * Created by Marcus on 6/30/16.
 */
public class BalanceInfo {
    private BigDecimal balance;

    public void setBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
