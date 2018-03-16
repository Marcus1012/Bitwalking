package bitwalking.bitwalking.mvi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hannesdorfmann.mosby3.ActivityMviDelegate;
import com.hannesdorfmann.mosby3.ActivityMviDelegateImpl;
import com.hannesdorfmann.mosby3.MviDelegateCallback;
import com.hannesdorfmann.mosby3.mvi.MviPresenter;
import com.hannesdorfmann.mosby3.mvp.MvpView;

import bitwalking.bitwalking.activityes.BwActivity;
import bitwalking.bitwalking.remote_service.BWServiceApi;
import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by alexey on 25.08.17.
 */

public abstract class MviBtwActivity<V extends MvpView, P extends MviPresenter<V, ?>>
        extends AppCompatActivity implements MvpView, MviDelegateCallback<V, P> {

    private boolean isRestoringViewState = false;
    protected ActivityMviDelegate<V, P> mvpDelegate;


    private final static String TAG = BwActivity.class.getSimpleName();

    // BW Service
    protected BWServiceApi _serviceApi;
    protected boolean _boundToService = false;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMvpDelegate().onCreate(savedInstanceState);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        getMvpDelegate().onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getMvpDelegate().onSaveInstanceState(outState);
    }

    @Override protected void onPause() {
        super.onPause();
        getMvpDelegate().onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        getMvpDelegate().onResume();
    }

    @Override protected void onStart() {
        super.onStart();
        getMvpDelegate().onStart();
    }

    @Override protected void onStop() {
        super.onStop();
        getMvpDelegate().onStop();
    }

    @Override protected void onRestart() {
        super.onRestart();
        getMvpDelegate().onRestart();
    }

    @Override public void onContentChanged() {
        super.onContentChanged();
        getMvpDelegate().onContentChanged();
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getMvpDelegate().onPostCreate(savedInstanceState);
    }

    /**
     * Instantiate a presenter instance
     *
     * @return The {@link MvpPresenter} for this viewState
     */
    @NonNull
    public abstract P createPresenter();

    /**
     * Get the mvp delegate. This is internally used for creating presenter, attaching and detaching
     * viewState from presenter.
     *
     * <p><b>Please note that only one instance of mvp delegate should be used per Activity
     * instance</b>.
     * </p>
     *
     * <p>
     * Only override this method if you really know what you are doing.
     * </p>
     *
     * @return {@link ActivityMviDelegate}
     */
    @NonNull protected ActivityMviDelegate<V, P> getMvpDelegate() {
        if (mvpDelegate == null) {
            mvpDelegate = new ActivityMviDelegateImpl<V, P>(this, this);
        }

        return mvpDelegate;
    }

    @NonNull @Override public V getMvpView() {
        try {
            return (V) this;
        } catch (ClassCastException e) {
            String msg =
                    "Couldn't cast the View to the corresponding View interface. Most likely you forgot to add \"Activity implements YourMviViewInterface\".";
            Log.e(this.toString(), msg);
            throw new RuntimeException(msg, e);
        }
    }

    @Override public final Object onRetainCustomNonConfigurationInstance() {
        return getMvpDelegate().onRetainCustomNonConfigurationInstance();
    }

    @Override public void setRestoringViewState(boolean restoringViewState) {
        this.isRestoringViewState = restoringViewState;
    }

    protected boolean isRestoringViewState() {
        return isRestoringViewState;
    }


    //region Btw
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
    //end region
}
