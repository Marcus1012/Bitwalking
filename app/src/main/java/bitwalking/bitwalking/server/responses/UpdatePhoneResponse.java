package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class UpdatePhoneResponse extends BasicServerResponse {

    public UpdatePhoneResponse(BasicServerResponse base) {
        super(base);
    }

    public PhoneUpdateInfo getCodeInfo() {
        return getPayload(PhoneUpdateInfo.class);
    }

    public class PhoneUpdateInfo {
        public CodeInfo code;
        public TelephoneInfo phone;

        public class CodeInfo {
            public int length;
            public String type;
        }
    }
}
