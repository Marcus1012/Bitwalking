package bitwalking.bitwalking.server.requests;

import bitwalking.bitwalking.util.PhoneLocation;
import bitwalking.bitwalking.util.PhoneNetwork;

/**
 * Created by Marcus on 10/14/16.
 */

public class JoinEventRequest extends JsonRequest {
    private JoinEventLocation currentLocation;

    public JoinEventRequest(JoinEventLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public static class JoinEventLocation {
        public PhoneLocation location;
        public PhoneNetwork network;
    }
}
