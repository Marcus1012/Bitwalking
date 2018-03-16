package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class RegistrationResponse extends BasicServerResponse {

    public RegistrationResponse(BasicServerResponse base) {
        super(base);
    }

    public RegistrationPayload getRegistrationPayload() {
        return getPayload(RegistrationPayload.class);
    }

    public class RegistrationPayload {
        public UpdatePhoneResponse.PhoneUpdateInfo.CodeInfo code;
        public TelephoneInfo phone;
        public Registration registration;

        public class Registration {
            public String sessionIdentifier;
        }
    }
}
