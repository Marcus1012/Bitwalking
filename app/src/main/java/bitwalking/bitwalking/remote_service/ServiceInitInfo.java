package bitwalking.bitwalking.remote_service;

/**
 * Created by Marcus on 10/26/16.
 */

public class ServiceInitInfo {
    public String userSecret;
    public String userMsisdn;
    public String userEmail;

    public boolean isInfoValid() {
        return (null != userSecret &&
                null != userMsisdn &&
                null != userEmail);
    }

    @Override
    public boolean equals(Object o) {
        if (null != o && o instanceof ServiceInitInfo) {
            ServiceInitInfo other = (ServiceInitInfo)o;
            return
                userSecret.contentEquals(other.userSecret) &&
                userMsisdn.contentEquals(other.userMsisdn) &&
                userEmail.contentEquals(other.userEmail);
        }

        return false;
    }
}
