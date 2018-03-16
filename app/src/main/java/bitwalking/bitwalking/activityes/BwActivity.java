package bitwalking.bitwalking.activityes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.remote_service.BWServiceApi;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/8/16.
 */
public abstract class BwActivity extends AppCompatActivity {

    private final static String TAG = BwActivity.class.getSimpleName();

    // BW Service
    protected BWServiceApi _serviceApi;
    protected boolean _boundToService = false;

    protected void bindToBwService() {
        if (!_boundToService) {
            Intent intent = new Intent(this, BwService.class);
            intent.setAction(BwService.class.getName());
            intent.putExtra(Globals.BITWALKING_JUST_BIND, true);
            bindService(intent, _bwServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        }
    }

    protected void unbindBwService() {
        if (_boundToService) {
            unbindService(_bwServiceConnection);
            _boundToService = false;
            _serviceApi = null;
        }
    }

    private ServiceConnection _bwServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // that's how we get the client side of the IPC connection
            _serviceApi = BWServiceApi.Stub.asInterface(service);
            _boundToService = true;
            onBwServiceConnected();
            Logger.instance().Log(Logger.VERB, TAG, "connected to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            _boundToService = false;
            _serviceApi = null;
            onBwServiceDisconnected();
            Logger.instance().Log(Logger.VERB, TAG, "disconnected from service");
        }
    };

    protected abstract void onBwServiceConnected();
    protected abstract void onBwServiceDisconnected();
}
