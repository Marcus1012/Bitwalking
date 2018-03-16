package bitwalking.bitwalking.remote_service;

import java.math.BigDecimal;

import bitwalking.bitwalking.user_info.CurrentEventInfo;

/**
 * Created by Marcus on 12/15/15.
 */
public interface UserTodayListener {
    void onUserTodayUpdate(BigDecimal today);
}
