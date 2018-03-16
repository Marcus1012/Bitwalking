package bitwalking.misfit;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by Marcus on 5/10/16.
 */
public class MisfitServiceRemote extends MisfitService {

    IBinder _binder = new MisfitServiceRemote.LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    public class LocalBinder extends Binder {
        public MisfitServiceRemote getService() {
            return MisfitServiceRemote.this;
        }
    }
}