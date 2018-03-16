package bitwalking.bitwalking.user_info;

import java.math.BigDecimal;

/**
 * Created by Marcus on 12/1/15.
 */
public class UserInfo {
    AuthInfo authObject;
    MeInfo meObject;
    BalanceInfo balanceObject;
    CurrentEventInfo currentEventObject;
    public Boolean isNewUser;

    public final AuthInfo getAuthInfo() {
        return authObject;
    }

    public void initAuthInfo(String userSecret) {
        authObject = new AuthInfo();
        authObject.userSecret = userSecret;
    }

    public final MeInfo getMeInfo() {
        return meObject;
    }

    public void setMeInfo(MeInfo newMeInfo) { meObject = newMeInfo;}

    public final BalanceInfo getBalanceInfo() {
        return balanceObject;
    }

    public void initBalanceInfo(BigDecimal balance) {
        balanceObject = new BalanceInfo();
        balanceObject.setBalance(balance);
    }

    public CurrentEventInfo getCurrentEventObject() { return currentEventObject; }

    public void setCurrentEventObject(CurrentEventInfo newCurrentEventObject) {
        currentEventObject = newCurrentEventObject;
    }
}
