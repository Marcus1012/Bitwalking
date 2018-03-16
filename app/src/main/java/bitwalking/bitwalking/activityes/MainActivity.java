package bitwalking.bitwalking.activityes;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.ViewDragHelper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.ShareScreenAsyncTask;
import bitwalking.bitwalking.events.EventSpecificActivity;
import bitwalking.bitwalking.events.EventsGlobals;
import bitwalking.bitwalking.mvi.events.EventsActivity;
import bitwalking.bitwalking.mvi.notification.ActivityNotifications;
import bitwalking.bitwalking.mvi.profile.ProfileActivity;
import bitwalking.bitwalking.mvi.settings.ActivitySettings;
import bitwalking.bitwalking.notifications.BitwalkingFirebaseInstanceIDService;
import bitwalking.bitwalking.registration_and_login.GoActivity;
import bitwalking.bitwalking.remote_service.BWServiceListener;
import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.remote_service.ServiceInitInfo;
import bitwalking.bitwalking.server.FinishAction;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateDeviceLogsRequest;
import bitwalking.bitwalking.server.responses.GetSurveyResponse;
import bitwalking.bitwalking.settings.InviteBusinessActivity;
import bitwalking.bitwalking.user_info.BalanceInfo;
import bitwalking.bitwalking.user_info.CurrentEventInfo;
import bitwalking.bitwalking.user_info.MeInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.ActivityUtils;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RoundImageView;
import bitwalking.bitwalking.vote_product.DownloadVoteProductsTask;
import bitwalking.bitwalking.vote_product.OnVoteReady;
import bitwalking.bitwalking.vote_product.VoteProductActivity;

import static bitwalking.bitwalking.util.AnimUtils.onCreateActivityAnimation;


// todo: zendesk
//import com.zendesk.sdk.model.access.AnonymousIdentity;
//import com.zendesk.sdk.model.access.Identity;
//import com.zendesk.sdk.network.impl.ZendeskConfig;

/**
 * Created by Marcus Greenberg on 9/25/15.
 *.........................
 *.........................
 *...............###.......
 *..............#   #......
 *..............#   #......
 *.......###....#   #......
 *......#   #...#   #......
 *......#   #...#   #......
 *......#   #....###.......
 *......#   #..............
 *......#   #..............
 *.......###...............
 *.........................
 *.........................
 *.......BitWalking ©......
 *.........................
 */

public class MainActivity extends BwActivity
        implements
            GoogleApiClient.OnConnectionFailedListener,
            GoogleApiClient.ConnectionCallbacks,
//            OnDataPointListener,
            OnVoteReady {

    //region Members

    private final static String TAG = MainActivity.class.getSimpleName();
    private final int SEND_INVITE_REQUEST = 4;

    // Side Menu
    private boolean openingActivity = false;
    private SwipeRefreshLayout _swipeContainer;
    private RelativeLayout _balanceDrawerPane, _menuDrawerPane;
    private long _lastMenuCloseTimeMs = 0;
    private DrawerLayout _drawerLayout;
    private boolean _drawerOpenedByButton = false;
    private boolean _invitePressed = false, _sharePressed = false;

    RoundImageView _profileImageMain, _profileImageMenu;
    int _profileImageCode = -1;

    BigDecimal _today;

    // Exit app
    boolean _logout = false, _loggingOut = false, _switchingOff = false;

    // User info
    boolean _login = false;
    AppPreferences _appPrefs;

    // Runtime permissions request
    final static int LOCATION_ACCESS_PERMISSION_REQUEST_ID = 1;

    // Google api
//    GoogleApiClient _googleApiClient;

    RelativeLayout _inviteTextLayout;
    ProgressBar _mydayLoading;
    View _rootLayout;
    View closeDrawerView;

    // Notifications
    TextView _newNotificationsCountText;

     TextView versionView;

    // Intent Extra Data
    private String _extraData;
    private String _openUri = null;
    private boolean _openStore = false;
    private boolean _sendLogs = false;
    private boolean _forceSendLogs = false;
    private boolean _openEvents = false;
    private boolean _openUserInvite = false;
    private boolean _openBalance = false;
    private boolean _openPlayStore = false;
    private boolean _forceUpdate = false;

    Gson _gson;
    AlertDialog _alert = null;

    //endregion

    //region Activity Events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if it is a switch off call
        if (handleIntent(getIntent())) {
            return;
        }

        _gson = new Gson();
        _appPrefs = new AppPreferences(getBaseContext());

        if (null != getIntent() && getIntent().hasExtra(Globals.BITWALKING_USER_INFO)) {
            if (!_appPrefs.isUserLoggedIn())
                logOut();

            _login = true;
        }

        setContentView(R.layout.main_activity);
        initViews();
        setViewsData();
        //ActivityUtils.ColorizeStatusBar(this,android.R.color.black);



        _switchingOff = false;
        try {
            _appPrefs.setSwitchedOff(false);
        } catch (Exception e) {}

        // Build the menu
        createBalanceDrawer();

        addLogoutHandle();
        _loggingOut = false;

        _inviteTextLayout = (RelativeLayout)findViewById(R.id.myday_invite_text_layout);
        _mydayLoading = (ProgressBar)findViewById(R.id.myday_loading);
        initRefreshSwipe();

        // reveal animation

        if (savedInstanceState == null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                getIntent().getBooleanExtra("CoolAnimation",false)) {
            _rootLayout.setVisibility(View.INVISIBLE);
            onCreateActivityAnimation(_rootLayout, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ActivityUtils.clearLightStatusBar(MainActivity.this);
                }
            });
        } else {
            _rootLayout.setVisibility(View.VISIBLE);
           // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }



        // Notifications
        _newNotificationsCountText = (TextView)findViewById(R.id.new_notifications_count);

        setLongClicks();
        FirebaseMessaging.getInstance().subscribeToTopic("news");
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().subscribeToTopic("debug");
            Logger.instance().initLogFile(MainActivity.this);

            findViewById(R.id.logs_menu_item_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.logs_menu_item_layout).setClickable(true);
        }

        if (Globals.EVENTS_ON)
            findViewById(R.id.main_menu_events).setVisibility(View.VISIBLE);

        pushToken();
        setBalanceDrawerVisible();
    }


    private void initViews(){
        versionView = (TextView)findViewById(R.id.versionTextView);
        closeDrawerView = findViewById(R.id.closeDrawerView);
        _rootLayout = findViewById(R.id.today_root_layout);
    }

    private void setViewsData(){
        versionView.setText("Version " + getVersionInfo());

        closeDrawerView.setOnClickListener(v -> closeDrawer(v));
    }


    private String getVersionInfo() {
        String versionName = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionName;
    }

    private void initRefreshSwipe() {
        _swipeContainer = (SwipeRefreshLayout) findViewById(R.id.today_refresh_layout);
        _swipeContainer.setDistanceToTriggerSync(20);// in dips
        _swipeContainer.setSize(SwipeRefreshLayout.DEFAULT);
        _swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                _swipeContainer.setRefreshing(true);
                ((BitwalkingApp)getApplication()).trackEvent("main", "swipe.refresh", "");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            _serviceApi.updateStepsAndDetails();
                        } catch (Exception e) {
                            BitwalkingApp.getInstance().trackException("Failed to update steps on swipe down", e);
                        }

                        updateToday();
                        updateBalance();
                        updateCurrentEvent();

                        Logger.instance().Log(Logger.DEBUG, TAG, "refresh stop");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    _swipeContainer.setRefreshing(false);
                                }
                            }, 2000);
                            }
                        });

                    }
                }).start();
            }
        });

        _swipeContainer.setColorSchemeResources(
                R.color.balance_background_color,
                R.color.cool_green,
                android.R.color.darker_gray,
                android.R.color.black);
    }

    private void initZendeskIdentity() {
        try {
            // todo: zendesk
//            if (null != _userInfo) {
//                Identity identity = new AnonymousIdentity.Builder()
//                        .withNameIdentifier(_userInfo.getMeInfo().fullName)
//                        .withEmailIdentifier(_userInfo.getMeInfo().email)
//                        .build();
//                ZendeskConfig.INSTANCE.setIdentity(identity);
//            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("Failed to init zendesk id", e));
        }
    }

    private void pushToken() {
        if (_appPrefs.needToPushToken()) {
            String token = FirebaseInstanceId.getInstance().getToken();

            BitwalkingFirebaseInstanceIDService.updateServer(token, getBaseContext());
            _appPrefs.setNeedToPushToken(false);
        }
    }
                       
    private void initUserData() {
        boolean userOk = false;

        try {
            UserInfo userInfo = getCurrentUserInfo();

            if (null != userInfo.getAuthInfo().userSecret &&
                null != userInfo.getMeInfo().phone.msisdn) {
                userOk = true;
            }
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("initUserData: failed", e);
        }

        if (!userOk) {
            new AppPreferences(getBaseContext()).clearAll();
            logOut("Please log in.");
            return;
        }

        if (_loadScreenAfterProfileComplete > 0) {
            switch (_loadScreenAfterProfileComplete) {
                case COMPLETE_PROFILE__BUY_SELL_CLICK:
                    onSellClick(null);
                    break;
                case COMPLETE_PROFILE__EVENTS_CLICK:
                    onEventsClick(null);
                    break;
                case COMPLETE_PROFILE__PROFILE_CLICK:
                    onProfileClick(null);
                    break;
                case COMPLETE_PROFILE__WALLET_CLICK:
                    onWalletClick(null);
                    break;
                default: break;
            }

            _loadScreenAfterProfileComplete = -1;
        }
    }

    private boolean _wdollarLongClicked = false;
    private boolean _bwSignLongClicked = false;

    void setLongClicks() {
        findViewById(R.id.today_wdollar_sign).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                _wdollarLongClicked = true;
                return false;
            }
        });

        findViewById(R.id.main_menu_button).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                _bwSignLongClicked = true;

                if (_wdollarLongClicked) {
                    Logger.instance().setLevel(Logger.VERB);
                    Logger.instance().Log(Logger.DEBUG, TAG, "logs level set to VERB");

                    findViewById(R.id.logs_menu_item_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.logs_menu_item_layout).setClickable(true);
                }

                if (BuildConfig.DEBUG) {
                    // --------------------- DEBUG ------------------------
                    final RelativeLayout debugLayout = (RelativeLayout) findViewById(R.id.main_debug_layout);
                    debugLayout.setVisibility(View.VISIBLE);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            debugLayout.setVisibility(View.INVISIBLE);
                        }
                    }, 1000);
                    // ----------------------------------------------------
                }

                return false;
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //first saving my state, so the bundle wont be empty.
        //https://code.google.com/p/android/issues/detail?id=19917
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    private boolean handleIntent(Intent intent) {
        boolean handled = false;
        if (null != intent){
            if (intent.hasExtra("SwitchOff")) {
                switchOff();
                handled = true;
            }
            else if (intent.hasExtra(Globals.BITWALKING_LOGOUT_BROADCAST)) {
                // Clear user's info
                if (null == _appPrefs)
                    _appPrefs = new AppPreferences(getBaseContext());
                _appPrefs.clearAll();

                setResult(RESULT_OK);
                MainActivity.this.finish();

                handled = true;
            }

            if (intent.hasExtra("extra_open")) {
                intent.putExtra(intent.getStringExtra("extra_open"), true);
            }

            if (intent.hasExtra("open_uri")) {
                intent.putExtra(Globals.BITWALKING_OPEN_URI, intent.getStringExtra("open_uri"));
            }

            _extraData = intent.getStringExtra("extra_data");
            _openPlayStore = intent.getBooleanExtra(Globals.BITWALKING_PLAY_STORE, false);
            _openBalance = intent.getBooleanExtra(Globals.BITWALKING_BALANCE, false);
            _openUserInvite = intent.getBooleanExtra(Globals.BITWALKING_USER_INVITE, false);
            _openStore = intent.getBooleanExtra(Globals.BITWALKING_STORE, false);
            _openEvents = intent.getBooleanExtra(Globals.BITWALKING_EVENT, false);
            _sendLogs = intent.getBooleanExtra(Globals.BITWALKING_SEND_LOGS, false);
            _forceSendLogs = intent.getBooleanExtra(Globals.BITWALKING_FORCE_SEND_LOGS, false);
            _forceUpdate = intent.getBooleanExtra(Globals.BITWALKING_FORCE_UPDATE, false);

            _openUri = intent.getStringExtra(Globals.BITWALKING_OPEN_URI);
        }

        return handled;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        Logger.instance().Log(Logger.DEBUG, TAG, "new intent");

        if (handleIntent(intent)) {
            // nothing
        }
        else if (intent.hasExtra(Globals.BITWALKING_USER_INFO)) {
            Logger.instance().Log(Logger.DEBUG, TAG, "onNewIntent: _userInfo set");

            try {
                String serviceInitJson = _gson.toJson(AppPreferences.getServiceInitInfo(MainActivity.this));
                Logger.instance().Log(Logger.DEBUG, TAG, "serviceInitJson = " + serviceInitJson);

                Intent serviceIntent = new Intent(MainActivity.this, BwService.class);
                serviceIntent.setAction(Globals.INIT_SERVICE_ACTION);
                serviceIntent.putExtra(Globals.BITWALKING_SERVICE_INIT_INFO, serviceInitJson);
                startService(serviceIntent);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }

            // If we got here, something wrong -> logout
            Logger.instance().Log(Logger.ERROR, TAG, "something went wrong, logout");
            ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "core.failure");
            logOut();
        }
        else {
            super.onNewIntent(intent);
        }
    }

    private void switchOff() {
        if (_switchingOff)
            return;

        _switchingOff = true;

        try {
            _appPrefs.setSwitchedOff(true);
        } catch (Exception e) {}

        ((BitwalkingApp)getApplication()).trackEvent("core", "switch.off", "");

        // stopRecording service and finish activity
        //old
//        Intent switchOffIntent = new Intent(MainActivity.this, BwService.class);
//        switchOffIntent.putExtra(Globals.BITWALKING_SWITCH_OFF_BROADCAST, true);
//        startService(switchOffIntent);
//        stopService(switchOffIntent);
        //new
        Intent switchOffIntent = new Intent(MainActivity.this, BwService.class);
        switchOffIntent.setAction(Globals.SWITCH_OFF_SERVICE_ACTION);
        startService(switchOffIntent);

        MainActivity.this.finish();
        // Inform logout to other activities
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Globals.BITWALKING_SWITCH_OFF_BROADCAST);
        sendBroadcast(broadcastIntent);
    }

    private void addLogoutHandle() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Globals.BITWALKING_PRE_LOGOUT_BROADCAST);
        registerReceiver(logoutReceiver, intentFilter);
    }

    private UserInfo getCurrentUserInfo() {
        return AppPreferences.getUserInfo(MainActivity.this);
    }

    private void updateSlideMenuInfo() {
        UserInfo userInfo = getCurrentUserInfo();
        Logger.instance().Log(Logger.DEBUG, TAG, "updateSlideMenuInfo: _userInfo set");

        if (null != userInfo && null != userInfo.getMeInfo()) {
            ((TextView)findViewById(R.id.main_menu_profile_name)).setText(userInfo.getMeInfo().fullName);
        }

        initZendeskIdentity();
    }

    AppPreferences.OnProfileImageChange profileImageChangeListener = new AppPreferences.OnProfileImageChange() {
        @Override
        public void profileImageChanged() {
            refreshProfileImage();
        }
    };

    private void refreshProfileImage() {
        _appPrefs.setProfileImageChangeListener(profileImageChangeListener);

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _profileImageMain = (RoundImageView)findViewById(R.id.profile_image_main);
                _profileImageMenu = (RoundImageView)findViewById(R.id.main_menu_profile_image);

                if (null != _profileImageMain)
                    _profileImageMain.setImageBitmap(_appPrefs.getProfileImage());
                if (null != _profileImageMenu)
                    _profileImageMenu.setImageBitmap(_appPrefs.getProfileImage());
            }
        });
    }

    ProgressDialog _progress;
    BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.instance().Log(Logger.DEBUG, TAG, "Logout in progress");
            _logout = true;
        }
    };

    private void preLogout() {
        _progress = new ProgressDialog(MainActivity.this);
        _progress.setMessage("Updating server ...");
        _progress.setCancelable(false);
        _progress.show();

        new Thread() {
            @Override
            public void run() {
                boolean updated = false;
                try {
                    updated = new UpdateServerAtLogout().execute().get(40, TimeUnit.SECONDS);
                } catch (Exception e) {
                    BitwalkingApp.getInstance().trackException("UpdateServerAtLogout failed", e);
                    e.printStackTrace();
                }

                if (null != _progress && _progress.isShowing())
                    _progress.dismiss();

                handleLogoutUpdate(updated);
            }
        }.start();
    }

    private void handleLogoutUpdate(boolean updated) {
        if (!updated) {
            ((BitwalkingApp)getApplication()).trackEvent("session", "logout", "failed");

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final String failedMessage = "Failed to update server, if you choose to sign out your unverified steps will be lost.";
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(failedMessage)
                            .setCancelable(true)
                            .setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "server.failure");
                                    logOut();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
        }
        else {
            ((BitwalkingApp)getApplication()).trackEvent("session", "logout", "success");
            logOut();
        }
    }

    private void logOut() {
        logOut(null);
    }

    private synchronized void logOut(String message) {
        if (_loggingOut)
            return;

        _loggingOut = true;
        // Clear user's info
        if (null == _appPrefs)
            _appPrefs = new AppPreferences(getBaseContext());
        _appPrefs.clearAll();
        _appPrefs.deleteProfileImage();
        // stopRecording service and finish activity
        //old
//        Intent logoutIntent = new Intent(MainActivity.this, BwService.class);
//        logoutIntent.putExtra(Globals.BITWALKING_LOGOUT_BROADCAST, true);
//        startService(logoutIntent);
//        stopService(logoutIntent);
        //new
        Intent logoutIntent = new Intent(MainActivity.this, BwService.class);
        logoutIntent.setAction(Globals.LOGOUT_SERVICE_ACTION);
        startService(logoutIntent);

        // Go activity
        Intent intent = new Intent(MainActivity.this, GoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (null != message) {
            intent.putExtra("logoutMessage", message);
        }
        startActivity(intent);
        MainActivity.this.finish();
        // Inform logout to other activities
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Globals.BITWALKING_LOGOUT_BROADCAST);
        sendBroadcast(broadcastIntent);
    }

//    @Override
//    public void onDataPoint(DataPoint dataPoint) {
//        printStepsDataPoint(dataPoint);
//    }

    @Override
    protected void onBwServiceConnected() {
        try {
            if (_logout) {
                _logout = false;
                preLogout();
            }
            else {
                checkIfServiceNeedUpdate();
                _serviceApi.addListener(bwListener);

                try {
                    if (null != _serviceApi)
                        _serviceApi.startSteps();

                    updateBalance();
                    updateToday();
                    updateCurrentEvent();
                    refreshEventUi();

                    if (getCurrentUserInfo().getMeInfo().email.endsWith("@bitwalking.com")) {
                        FirebaseMessaging.getInstance().subscribeToTopic("bitwalking");
                        Logger.instance().Log(Logger.INFO, TAG, "subscribe to bitwalking topic");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                refreshProfileImage();
                updateSlideMenuInfo();

                Logger.instance().Log(Logger.DEBUG, TAG, "onBwServiceConnected: updateSlideMenuInfo()");
                if (_login) {
                    _login = false;
                    loadUserData();
                }

                new DownloadVoteProductsTask(this, null).execute();
               // new LoadEvents(this, null).execute();

                handleOpenExtra();
            }
        } catch (RemoteException e) {
            Logger.instance().Log(Logger.ERROR, TAG, "Failed to add listener", e);
        }

//        startGoogleApiService();
    }

    @Override
    protected void onBwServiceDisconnected() {
    }

    private void checkIfServiceNeedUpdate() {
        try {
            String serviceInfoJson = _serviceApi.getServiceInfo();
            ServiceInitInfo currentInfo = _gson.fromJson(serviceInfoJson, ServiceInitInfo.class);
            ServiceInitInfo newInfo = AppPreferences.getServiceInitInfo(MainActivity.this);
            if (null != currentInfo && null != newInfo && !currentInfo.equals(newInfo))
                _serviceApi.updateServiceInfo(_gson.toJson(newInfo));

        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to check service info needs update");
            e.printStackTrace();
        }
    }

    private class UpdateServerAtLogout extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... args) {
            boolean updated = false;

            try {
                Logger.instance().Log(Logger.DEBUG, "UpdateServerAtLogout", "call update steps and details");

                while (null == _serviceApi)
                    Thread.sleep(500);

                updated = _serviceApi.updateStepsAndDetails();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return updated;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (_switchingOff)
            return;

        _wdollarLongClicked = false;
        _bwSignLongClicked = false;

        _appPrefs = new AppPreferences(getBaseContext());

        if (!_appPrefs.isUserLoggedIn()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "no logged in user");
            ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "session.failure");
            logOut();
        }
        else if (_appPrefs.getForceLogout()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "force logout");
            ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "session.failure");
            logOut("Your session has expired. Please log in.");
        }
        else if (_appPrefs.getForceUpdate()) {
            forceUpdate();
        }
        else {
            if (checkPermissions())
                startAll();
        }

        openingActivity = false;

        if (null == _checkConnectionPeriodicExecutor) {
            _checkConnectionPeriodicExecutor = new ScheduledThreadPoolExecutor(1);
        }

        _checkConnectionFuture = _checkConnectionPeriodicExecutor.scheduleAtFixedRate(
                _checkGridOnOffTask, 1000, GRID_ON_OFF_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void forceUpdate() {
        Logger.instance().Log(Logger.DEBUG, TAG, "force update");
        ((BitwalkingApp)getApplication()).trackEvent("session", "update.forced", "");

        final String updateMessage = "Update available now! Go to Play Store";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(updateMessage)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((BitwalkingApp)getApplication()).trackEvent("session", "update.forced", "open.store");
                        _appPrefs.clearForceUpdate();
                        Globals.hideForceUpdateNotification(MainActivity.this);
                        Globals.openAppPlayStore(MainActivity.this);
                    }
                })
                .setNegativeButton("Log off", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((BitwalkingApp)getApplication()).trackEvent("session", "update.forced", "session.logoff");
                        _appPrefs.clearForceUpdate();
                        Globals.hideForceUpdateNotification(MainActivity.this);
                        logOut("Please update Bitwalking app before log in");
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void startAll() {
        startBitwalkingService();
    }

    private void handleOpenExtra() {
        if (_openBalance) {
            _openBalance = false;
            onLogoClick(null);

            // Somehow we need to do this :\
            _closeDrawerTimeout = new Handler();
            _closeDrawerTimeout.postDelayed(_closeDrawerRunnable, 1200);
        }

        if (null != _openUri) {
            openUri(_openUri);
            _openUri = null;
        }

        if (_openStore) {
            _openStore = false;
            onStoreClick(null);
        }

        if (_openUserInvite) {
            _openUserInvite = false;
            onInviteFriendsClick(null);
        }

        if (_openEvents) {
            _openEvents = false;
            onEventsClick(null);
        }

        if (_sendLogs) {
            _sendLogs = false;
            handleSendLogs();
        }

        if (_forceSendLogs) {
            _forceSendLogs = false;
            handleForceSendLogs();
        }

        if (_openPlayStore) {
            _openPlayStore = false;
            Globals.openAppPlayStore(this);
        }

        if (_forceUpdate) {
            _forceUpdate = false;
            if (null == _appPrefs)
                _appPrefs = new AppPreferences(getBaseContext());
            _appPrefs.setForceUpdate();
            forceUpdate();
        }
    }

    private void handleSendLogs() {
        // Confirm logs send - popup user
        final String failedMessage = "Hi from Bitwalking support.\nWe would like to read the Bitwalking app logs to assess your problem";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(failedMessage)
                .setCancelable(true)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((BitwalkingApp)getApplication()).trackEvent("app", "logs.send", "accept");
                        sendLogsToServer();
                    }
                })
                .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((BitwalkingApp)getApplication()).trackEvent("app", "logs.send", "decline");
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void handleForceSendLogs() {
        sendLogsToServer();
    }

    private void sendLogsToServer() {
        byte[] logsBytes = null;
        try {
            logsBytes = getLogsToServer().getBytes("UTF-8");
        } catch (Exception e) {
            logsBytes = e.toString().getBytes();
        }

        ServerApi.putLogs(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                new UpdateDeviceLogsRequest(logsBytes),
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        switch (code) {
                            case 200:
                            case 201:
                                ((BitwalkingApp)getApplication()).trackEvent("app", "logs.send", "success");
                                break;
                            default:
                                ((BitwalkingApp)getApplication()).trackEvent("app", "logs.send", "failure");
                                break;
                        }
                    }
                });
    }

    private String getLogsToServer() {
        StringBuilder sbLogs = new StringBuilder();
        sbLogs.append("-- logs --\n");
        try {
            sbLogs.append(_serviceApi.getLogs());
        } catch (Exception e) {
            sbLogs.append(e.toString());
        }

        sbLogs.append("\n-- steps --\n");
        try {
            sbLogs.append(_serviceApi.getSteps());
        } catch (Exception e) {
            sbLogs.append(e.toString());
        }

        return sbLogs.toString();
    }

    @Override
    public void onVoteReady(GetSurveyResponse.SurveyInfo survey, String userChoice, boolean newVote) {
        if (newVote)
            findViewById(R.id.store_notification).setVisibility(View.VISIBLE);
    }

    @Override
    public void onVoteLoadError() {
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != _alert && _alert.isShowing())
            _alert.dismiss();

        if (_boundToService) {
            try {
                _serviceApi.removeListener(bwListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            unbindBwService();
        }

        if (null != _checkConnectionFuture) {
            _checkConnectionFuture.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(logoutReceiver);
        }
        catch (Exception e) {
        }

//        stopGooglePlayService();
        destroyProgress();
    }

    //endregion

    //region Menu Options Click

    public void onNoConnectionClick(View v) {
        startActivity(new Intent(MainActivity.this, OffGridActivity.class));
    }

    public void onInviteBusinessClick(View v) {
        menuStartActivity(InviteBusinessActivity.class, "invite.business");
    }

    public void onSettingsClick(View v){
        startActivity(new Intent(MainActivity.this, ActivitySettings.class));
    }

    public void onInviteFriendsClick(View v) {
       // menuStartActivity(UserInviteActivity.class, "invite.friend");
        sendInvitation(null);
    }



    private void sendInvitation(String affiliationCode) {
        Uri dynamicLink = Globals.getDownloadLink();
        String text = "Hi, I'm generating money by walking! Join #bitwalking, the new global currency: " + dynamicLink.toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivityForResult(Intent.createChooser(intent, "Invite friends"), SEND_INVITE_REQUEST);

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "");
    }

    public void onWalletClick(View v) {
        if (isProfileUpdateNeeded())
            showProfileUpdateActivity(COMPLETE_PROFILE__WALLET_CLICK);
        else {
            ((BitwalkingApp) getApplication()).trackEvent("menu", "open.wallet", "");

            Intent intent = new Intent(this, WalletActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    public void onSellClick(View v) {
        if (isProfileUpdateNeeded())
            showProfileUpdateActivity(COMPLETE_PROFILE__BUY_SELL_CLICK);
        else {
            ((BitwalkingApp) getApplication()).trackEvent("menu", "open.buy.sell", "");

            Intent intent = new Intent(this, WhatToDoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("Screen.Name", "buy/sell");
            startActivity(intent);
        }
    }

    public void onEventsClick(View v) {
        if (isProfileUpdateNeeded())
            showProfileUpdateActivity(COMPLETE_PROFILE__EVENTS_CLICK);
        else {
            ((BitwalkingApp)getApplication()).trackEvent("menu", "open.events", "");

            Intent intent = new Intent(this, EventsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (null != _extraData && !_extraData.isEmpty()) {
                intent.putExtra(Globals.BITWALKING_EVENT_ID, _extraData);
                _extraData = null;
            }

            startActivity(intent);
        }
    }

    public void onNotificationsClick(View v){
        ((BitwalkingApp)getApplication()).trackEvent("menu", "open.notifications", "");
        Intent intent = new Intent(this, ActivityNotifications.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void onLogsClick(View v) {
        menuStartActivity(DebugLogsActivity.class, "debug.logs");
    }

    public void onFAQClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("menu", "open.support.faq", "");
        ((BitwalkingApp)getApplication()).trackScreenView("support");

        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", "support.bitwalking.com");
        startActivity(intent);
    }

    private void menuStartActivity(Class<?> cls, String analyticsName) {
        ((BitwalkingApp)getApplication()).trackEvent("menu", "open." + analyticsName, "");

        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }

    //endregion

    //region Google Api

    private static final int REQUEST_RESOLVE_ERROR = 123;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 124;
    boolean _resolvingError = false;
    GoogleApiClient _googleApiClient = null;
    private void buildFitnessClient() {
        if (_googleApiClient == null) {
            _googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(LocationServices.API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Logger.instance().Log(Logger.DEBUG, TAG, "google api connected");

                                    _resolvingError = false;
                                    try {
//                                        _googleApiClient.disconnect();
                                        if (null != _serviceApi)
                                            _serviceApi.startSteps(); // todo: handle case when api is null - google api connected sooner than service
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Logger.instance().Log(Logger.INFO, TAG, "Connection lost.  Cause: Network Lost.");
                                        BitwalkingApp.getInstance().trackException(new Exception("Google api client network lost"));
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Logger.instance().Log(Logger.INFO, TAG, "Connection lost.  Reason: Service Disconnected");
                                        BitwalkingApp.getInstance().trackException(new Exception("Google api client disconnected"));
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult result) {
                            String snackMsg = "Failed connecting to Google Play services";
                            switch (result.getErrorCode()) {
                                case ConnectionResult.CANCELED:
                                    snackMsg += ": Choose google account";
                                    break;
                                default:
                                    break;
                            }

                            Logger.instance().Log(Logger.INFO, TAG, snackMsg);
                            Snackbar.make(
                                    MainActivity.this.findViewById(R.id.today_root_layout),
                                    snackMsg,
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Retry", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            overridePendingTransition(R.anim.hold, R.anim.hold);
                                            MainActivity.this.startActivity(new Intent(MainActivity.this, GoActivity.class));
                                            MainActivity.this.finish();
                                        }
                                    })
                                    .setActionTextColor(Color.YELLOW)
                                    .show();
                            BitwalkingApp.getInstance().trackException(new Exception(snackMsg));
                        }
                    })
                    .build();
        }
    }

    private void startGoogleApiService() {
        if (false == _resolvingError && null != _googleApiClient && !_googleApiClient.isConnected() && !_googleApiClient.isConnecting()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "connect google api client");
            _googleApiClient.connect();
        }
    }

    private void stopGooglePlayService() {
        if (null != _googleApiClient && (_googleApiClient.isConnected() || _googleApiClient.isConnecting())) {
            Logger.instance().Log(Logger.DEBUG, TAG, "disconnect google api client");

            _googleApiClient.disconnect();
        }
    }
//
    @Override
    public void onConnected(Bundle bundle) {
        Logger.instance().Log(Logger.DEBUG, TAG, "google api connected");

        _resolvingError = false;
        try {
            if (null != _serviceApi)
                _serviceApi.startSteps(); // todo: handle case when api is null - google api connected sooner than service
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//    private void printStepsDataPoint(DataPoint data) {
//        DateFormat df = Globals.getFullDateFormat();
//        long startTime = data.getStartTime(TimeUnit.MILLISECONDS);
//        long endTime = data.getEndTime(TimeUnit.MILLISECONDS);
//        long timestamp = endTime - startTime;
//        String steps = String.valueOf(data.getValue(com.google.android.gms.fitness.data.Field.FIELD_STEPS));
//
//        String dataInfo = String.format("steps: [%s]/[%s]*[%s]-[%s]~[%d]=[%s]",
//                data.getDataSource().toDebugString(),
//                data.getOriginalDataSource().toDebugString(),
//                df.format(new Date(startTime)),
//                df.format(new Date(endTime)),
//                timestamp,
//                steps);
//
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("fitness data: %s", dataInfo));
//    }
//
//    private void printLocationDataPoint(DataPoint data) {
//        DateFormat df = Globals.getFullDateFormat();
//        String from = data.getDataSource().toString();
//        String originalFrom = data.getOriginalDataSource().toString();
//        String type = data.getDataType().toString();
//        long startTime = data.getStartTime(TimeUnit.MILLISECONDS);
//        long endTime = data.getEndTime(TimeUnit.MILLISECONDS);
//        long timestamp = endTime - startTime;
//        String lat = String.valueOf(data.getValue(com.google.android.gms.fitness.data.Field.FIELD_LATITUDE));
//        String lon = String.valueOf(data.getValue(com.google.android.gms.fitness.data.Field.FIELD_LONGITUDE));
//
//        String dataInfo = String.format("location: [%s]-[%s]~[%d]=[%s],[%s]",
//                df.format(new Date(startTime)),
//                df.format(new Date(endTime)),
//                timestamp,
//                lat,
//                lon);
//
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("fitness data: %s", dataInfo));
//    }
//
    @Override
    public void onConnectionSuspended(int i) {
        Logger.instance().Log(Logger.ERROR, TAG, String.format("google api Suspended"));
        BitwalkingApp.getInstance().trackException(new Exception("google api suspended"));

//        try {
//            if (null != _serviceApi)
//                _serviceApi.stopSteps();
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        resolveGoogleApiError(result);
    }

    private void resolveGoogleApiError(ConnectionResult result) {
        String log = String.format("google api failed - error [%d]: %s", result.getErrorCode(), result.getErrorMessage());
        Logger.instance().Log(Logger.DEBUG, TAG, log);

        // Dispatch connection failed
        if (_resolvingError) {
            // Already attempting to resolve an error.
            Logger.instance().Log(Logger.DEBUG, TAG, "google api - already resolving");
            return;
        } else if (result.hasResolution()) {
            try {
                Logger.instance().Log(Logger.DEBUG, TAG, "google api - start resolving");
                _resolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (Exception e) {
                // There was an error with the resolution intent. Try again.
                BitwalkingApp.getInstance().trackException(e);
                _googleApiClient.connect();
            }
        } else {
            Logger.instance().Log(Logger.DEBUG, TAG, "google api - cannot resolve");
            BitwalkingApp.getInstance().trackException(new Exception(
                    String.format(Locale.US, "google api connection failed, error code = %d\nerror message = %s", result.getErrorCode(), result.getErrorMessage())));

            try {
                GoogleApiAvailability.getInstance().getErrorDialog(
                        MainActivity.this, result.getErrorCode(), REQUEST_RESOLVE_ERROR).show();
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
            }

            _resolvingError = true;
        }
    }

    //endregion

    //region Side Menu

    Handler _closeDrawerTimeout;
    Runnable _closeDrawerRunnable = new Runnable() {
        @Override
        public void run() {closeDrawer(null);}
    };


    private void closeDrawer(View v){
        Logger.instance().Log(Logger.DEBUG, TAG, "drawer close timeout");
        if (_drawerLayout.isDrawerOpen(_balanceDrawerPane)) {
            Logger.instance().Log(Logger.DEBUG, TAG, "drawer close()");
            _drawerLayout.closeDrawer(_balanceDrawerPane);
        }
    }

    private void createBalanceDrawer() {
        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _drawerLayout.setFocusableInTouchMode(false);
        _balanceDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        _menuDrawerPane = (RelativeLayout) findViewById(R.id.menuDrawerPane);

      /*  _balanceDrawerPane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_drawerLayout.isDrawerOpen(_balanceDrawerPane)) {
                    Logger.instance().Log(Logger.DEBUG, TAG, "drawer click");
                    _drawerLayout.closeDrawer(_balanceDrawerPane);
                    _drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Logger.instance().Log(Logger.DEBUG, TAG, "unlock drawer");
                            _drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        }
                    }, 500);
                }
            }
        });*/

        _drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
//                Logger.instance().Log(Logger.DEBUG, TAG, "onDrawerSlide");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Logger.instance().Log(Logger.DEBUG, TAG, "onDrawerOpened");
                if (drawerView.getId() == _balanceDrawerPane.getId()) {
                    if (_drawerOpenedByButton) {
                        ((BitwalkingApp) getApplication()).trackEvent("main", "show.balance", "source.button");
                    } else {
                        ((BitwalkingApp) getApplication()).trackEvent("main", "show.balance", "source.slide");
                    }

                    _drawerOpenedByButton = false;
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {

                if (drawerView == _balanceDrawerPane) {
                    Logger.instance().Log(Logger.DEBUG, TAG, "onDrawerClosed - balance");
//                    setMenuDrawerVisible();
                }
                if (drawerView == _menuDrawerPane) {
                    Logger.instance().Log(Logger.DEBUG, TAG, "onDrawerClosed - menu");
//                    setMenuDrawerVisible();
//                    setBalanceDrawerVisible();
                    _lastMenuCloseTimeMs = System.currentTimeMillis();
                }

                _drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Logger.instance().Log(Logger.DEBUG, TAG, "unlock drawer");
                        _drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }
                }, 500);

                setBalanceDrawerVisible();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                Logger.instance().Log(Logger.DEBUG, TAG, "onDrawerStateChanged");

                if (DrawerLayout.STATE_IDLE == newState) {
                    if (_drawerLayout.isDrawerOpen(_balanceDrawerPane)) {
                        if (null != _closeDrawerTimeout) {
                            _closeDrawerTimeout.removeCallbacks(_closeDrawerRunnable);
                        }

                      /*  _closeDrawerTimeout = new Handler();
                        _closeDrawerTimeout.postDelayed(_closeDrawerRunnable, 1200);*/
                    }
                }
            }
        });

//        setDrawerSlideMargin();
    }

    private void setDrawerSlideMargin() {
        try {
            Field mDragger = _drawerLayout.getClass().getDeclaredField("mLeftDragger");//mRightDragger or mLeftDragger based on Drawer Gravity
            mDragger.setAccessible(true);
            ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(_drawerLayout);

            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            int edge = mEdgeSize.getInt(draggerObj);

            mEdgeSize.setInt(draggerObj, edge * 2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onStoreClick(View view) {
        if (openingActivity && null != view)
            return;

        openingActivity = true;

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.store", "");
        Intent voteIntent = new Intent(this, VoteProductActivity.class);
        voteIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(voteIntent);
    }

    public void onProfileClick(View view) {
        if (openingActivity && null != view)
            return;

        openingActivity = true;

        if (isProfileUpdateNeeded()) {
            showProfileUpdateActivity(COMPLETE_PROFILE__PROFILE_CLICK);
        }
        else {
            ((BitwalkingApp) getApplication()).trackEvent("menu", "open.profile", "");
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    final static int COMPLETE_PROFILE__PROFILE_CLICK = 10;
    final static int COMPLETE_PROFILE__WALLET_CLICK = 11;
    final static int COMPLETE_PROFILE__EVENTS_CLICK = 12;
    final static int COMPLETE_PROFILE__BUY_SELL_CLICK = 13;
    int _loadScreenAfterProfileComplete = -1;

    private void showProfileUpdateActivity(int reqId) {
        startActivityForResult(new Intent(MainActivity.this, CompleteProfileActivity.class), reqId);
    }

    private boolean isProfileUpdateNeeded() {
        boolean updateNeeded = true;

        try {
            UserInfo userInfo = getCurrentUserInfo();
            Date dob = Globals.getUTCDateFormat().parse(userInfo.getMeInfo().dateOfBirth);

            if (null != userInfo.getMeInfo().country &&
                null != dob &&
                null != userInfo.getMeInfo().fullName) {
                updateNeeded = false;
            }
        } catch (Exception e) {
        }

        return updateNeeded;
    }

    @Override
    public void onBackPressed() {
        // First, close drawer
        if (_drawerLayout.isDrawerOpen(_balanceDrawerPane)) {
            _drawerLayout.closeDrawer(_balanceDrawerPane);
        }
        else if (_drawerLayout.isDrawerOpen(_menuDrawerPane)) {
            _drawerLayout.closeDrawer(_menuDrawerPane);
        }
        // Check if need to go to previous fragment or exit
        else {
            // Exit
            super.onBackPressed();

            ((BitwalkingApp)getApplication()).trackScreenView("today");
        }
    }

    private static final int UPDATE_STEPS_UI_MSG = 1;
    private static final int DEVICE_DETACH_UI_MSG = 2;
    private static final int STEPS_HISTORY_UPDATE_UI_MSG = 3;
    private static final int GOOGLE_API_CLIENT_ERROR_MSG = 4;
    private static final int NEW_NOTIFICATIONS_MSG = 5;

    private Handler _serviceMsgsHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_STEPS_UI_MSG:
                    BigDecimal today = (BigDecimal)msg.obj;
                    onStepsUpdate(today);
                    break;
                case DEVICE_DETACH_UI_MSG:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "remote.logout");
                            final String alertMessage = "This device no longer attached to your user. If you did not make this change or need assistance, please contact us at support@bitwalking.com";
                            logOut(alertMessage);
                        }});
                    break;
                case STEPS_HISTORY_UPDATE_UI_MSG:
                    break;
                case GOOGLE_API_CLIENT_ERROR_MSG:
                    buildFitnessClient();
                    stopGooglePlayService();
                    startGoogleApiService();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    };

    public void onStepsUpdate(BigDecimal today) {
        Logger.instance().Log(Logger.DEBUG, TAG, "onStepsUpdate: " + today);

        if (null != today)
            _today = today;

        if (null == _today)
            _today = new BigDecimal("0");

        refreshTodayAndBalanceUI();

        handleUserDataLoaded(_TODAY_LOADED_IDX);
    }

    private void refreshTodayAndBalanceUI() {
        BigDecimal balance = new BigDecimal("0");
        UserInfo userInfo = getCurrentUserInfo();
        if (null != userInfo) {
            if (null != userInfo.getBalanceInfo() && null != userInfo.getBalanceInfo().getBalance())
                balance = userInfo.getBalanceInfo().getBalance();
        }

        if (null == _today)
            _today = new BigDecimal("0");

        // Update balance
        ((TextView) findViewById(R.id.menu_balance)).setText(" " + Globals.bigDecimalToNiceString(balance));

        Logger.instance().Log(Logger.DEBUG, TAG, String.format("Got new balance and today info: %s %s",
                _today, balance));

        // Update today
        ((TextView) findViewById(R.id.main_user_today)).setText(" " + Globals.bigDecimalToNiceString(_today));
    }

    //endregion

    //region BW Service

    protected void startBitwalkingService() {
        if (!_boundToService && !_loggingOut/* && null != _userInfo*/) {

            if (null == getCurrentUserInfo()) {
                fixMigration();
            }
            else {
                Logger.instance().Log(Logger.DEBUG, TAG, "start bitwalking service");
//                bindToBwService(AppPreferences.getServiceInitInfo(MainActivity.this));
                Intent startIntent = new Intent(this, BwService.class);
                startIntent.setAction(Globals.START_SERVICE_ACTION);
                startService(startIntent);

                bindToBwService();
            }
        }
    }

    private BWServiceListener.Stub bwListener = new BWServiceListener.Stub() {
        @Override
        public void onTodayUpdate(String today) throws RemoteException {
            _serviceMsgsHandler.sendMessage(_serviceMsgsHandler.obtainMessage(UPDATE_STEPS_UI_MSG, new BigDecimal(today)));
        }

        @Override
        public void onVerifiedSteps() throws RemoteException {

        }

        @Override
        public void onDeviceDetached() throws RemoteException {
            _serviceMsgsHandler.sendMessage(_serviceMsgsHandler.obtainMessage(DEVICE_DETACH_UI_MSG));
        }

        @Override
        public void onGoogleApiClientError() {
            _serviceMsgsHandler.sendMessage(_serviceMsgsHandler.obtainMessage(GOOGLE_API_CLIENT_ERROR_MSG));
        }
    };

    //endregion

    //region Click Events

    private void setBalanceDrawerVisible() {
        Logger.instance().Log(Logger.DEBUG, TAG, "setBalanceDrawerVisible");
        _menuDrawerPane.setVisibility(View.GONE);
        _balanceDrawerPane.setVisibility(View.VISIBLE);
    }

    private boolean setMenuDrawerVisible() {
        if (_lastMenuCloseTimeMs + 500 < System.currentTimeMillis()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "setMenuDrawerVisible");
            _balanceDrawerPane.setVisibility(View.GONE);
            _menuDrawerPane.setVisibility(View.VISIBLE);

            return true;
        }

        return false;
    }

    public void onLogoClick(View v) {
        setBalanceDrawerVisible();
        try {
            _drawerLayout.openDrawer(_balanceDrawerPane);
            _drawerOpenedByButton = true;
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException("failed on logo click", e);
        }
    }

    public void onMenuClick(View v) {
        if (setMenuDrawerVisible()) {
            _drawerLayout.openDrawer(_menuDrawerPane);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!_drawerLayout.isDrawerOpen(_menuDrawerPane)) {
                                Logger.instance().Log(Logger.DEBUG, TAG, "retry opening menu");
                                onMenuClick(null);
                            }
                        }
                    });
                }
            }, 100);
        }
    }

    public static final int SHARE_TODAY_REQUEST = 4;
    public void onShareClick(View v) {
        if (_sharePressed)
            return;

        _sharePressed = true;
        _mydayLoading.setVisibility(View.VISIBLE);

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.share", "");

//        MainActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
                String title = "My Bitwalking Today";
                String downloadLink = Globals.getDownloadLink().toString();
                String text = String.format("Today I earned %s W$.\nJoin #bitwalking, the new global currency: %s",
                        ((TextView) findViewById(R.id.main_user_today)).getText(),
                        downloadLink);
                Bitmap screenshot = Globals.getScreenShot(MainActivity.this);

                new ShareScreenAsyncTask(MainActivity.this, screenshot).execute(title, text);
//            }
//        });
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("startActivityForResult failed, req=" + requestCode, e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Logger.instance().Log(Logger.DEBUG, TAG, "onActivityResult");

        switch (requestCode) {
            case SHARE_TODAY_REQUEST:
                _sharePressed = false;
                _mydayLoading.setVisibility(View.GONE);
                break;
            case REQUEST_RESOLVE_ERROR:
            case REQUEST_GOOGLE_PLAY_SERVICES:
                _resolvingError = false;
                if (resultCode == Activity.RESULT_OK) {
                    Logger.instance().Log(Logger.DEBUG, TAG, "REQUEST_RESOLVE_ERROR - result ok");
                    // Make sure the app is not already connected or attempting to connect
                    if (!_googleApiClient.isConnecting() &&
                            !_googleApiClient.isConnected()) {
                        _googleApiClient.connect();
                    }
                }
                else {
                    Logger.instance().Log(Logger.DEBUG, TAG, "REQUEST_RESOLVE_ERROR - result not ok");
                    // Go cannot run without the fitness permissions
                    Toast.makeText(MainActivity.this, "\'Bitwalking\' cannot run without fitness access permissions", Toast.LENGTH_LONG).show();
                }
                break;
            case COMPLETE_PROFILE__BUY_SELL_CLICK:
            case COMPLETE_PROFILE__EVENTS_CLICK:
            case COMPLETE_PROFILE__PROFILE_CLICK:
            case COMPLETE_PROFILE__WALLET_CLICK:
                if (resultCode == Activity.RESULT_OK)
                    _loadScreenAfterProfileComplete = requestCode;
                break;
            default: break;
        }
    }

    //endregion

    //region App Permissions

    public boolean checkPermissions() {
        return Globals.havePermission(this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_ACCESS_PERMISSION_REQUEST_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_ACCESS_PERMISSION_REQUEST_ID:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAll();
                }
                else {
                    // permission denied
                    Toast.makeText(this, "Location permission denied, \'Bitwalking\' shutting down ...", Toast.LENGTH_SHORT).show();
                    // Close app :)
                    shutdownApp();
                }
                break;
            default: break;
        }
    }

    private void shutdownApp() {
        Intent stopIntent = new Intent(MainActivity.this, BwService.class);
        stopIntent.setAction(Globals.STOP_SERVICE_ACTION);
        startService(stopIntent);

        this.finish();
    }

    //endregion

    //region Off the grid

    private static final float OFF_GRID_ALPHA = 0.3f;
    private static final float ON_GRID_ALPHA = 1.0f;
    private static final int GRID_ON_OFF_CHECK_INTERVAL = 30 * 1000;

    private volatile ScheduledFuture<?> _checkConnectionFuture;
    ScheduledThreadPoolExecutor _checkConnectionPeriodicExecutor;
    Runnable _checkGridOnOffTask = new Runnable() {
        public void run() {
            try {
                checkGridOnOff();
            } catch (Exception e) {
                BitwalkingApp.getInstance().trackException("Failed to check grid off", e);
            }
        }
    };

    private void checkGridOnOff() {
        ServerApi.checkSession(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                switch (code) {
                                    case 200: setOnGridView(); break;
                                    case 401: {
                                        if (_appPrefs.getForceLogout()) {
                                            ((BitwalkingApp)getApplication()).trackEvent("session", "logout.forced", "session.failure");
                                            logOut("Your session has expired. Please log in.");
                                        }
                                        break;
                                    }
                                    default: setOffGridView();
                                }
                            }
                        });
                    }
                });
    }

    private void setOnGridView() {

        // Today balance
        TextView todayBalanceText = (TextView) findViewById(R.id.main_user_today);
        todayBalanceText.setAlpha(ON_GRID_ALPHA);
        // Today title
        TextView todayTitleText = (TextView) findViewById(R.id.today_title);
        todayTitleText.setAlpha(ON_GRID_ALPHA);
        // Walking dollar sign
        TextView wDollarText = (TextView)findViewById(R.id.today_wdollar_sign);
        wDollarText.setAlpha(ON_GRID_ALPHA);
        // Off grid icon
        findViewById(R.id.off_grid_image_layout).setVisibility(View.GONE);
    }

    private void setOffGridView() {
        // Today balance
        TextView todayBalanceText = (TextView) findViewById(R.id.main_user_today);
        todayBalanceText.setAlpha(OFF_GRID_ALPHA);
        // Today title
        TextView todayTitleText = (TextView) findViewById(R.id.today_title);
        todayTitleText.setAlpha(OFF_GRID_ALPHA);
        // Walking dollar sign
        TextView wDollarText = (TextView)findViewById(R.id.today_wdollar_sign);
        wDollarText.setAlpha(OFF_GRID_ALPHA);
        // Off grid icon
        findViewById(R.id.off_grid_image_layout).setVisibility(View.VISIBLE);
    }


    //endregion

    //region Event

    public void onCurrentEventClick(View v) {
        if (openingActivity && null != v)
            return;

        openingActivity = true;

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.current.event", "");

        Intent intent = new Intent(MainActivity.this, EventSpecificActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EventsGlobals.EVENT_ID_KEY, getCurrentUserInfo().getCurrentEventObject().eventId);
        startActivity(intent);
    }

    private void openUri(String uri) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", uri);
        startActivity(intent);
    }

    private void refreshEventUi() {
        final UserInfo userInfo = getCurrentUserInfo();
        if (null == userInfo)
            return;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CurrentEventInfo currentEvent = userInfo.getCurrentEventObject();

                if (null != currentEvent && null != currentEvent.eventId) {
                    BigDecimal donationAmount = currentEvent.donation;
                    if (null == donationAmount)
                        donationAmount = new BigDecimal("0");

                    findViewById(R.id.main_event_donation_layout).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.main_event_donation_amount)).setText(
                            " " + Globals.bigDecimalToNiceString(donationAmount));
                }
                else {
                    findViewById(R.id.main_event_donation_layout).setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateCurrentEvent() {
        if (!Globals.EVENTS_ON)
            return;

        if (needToUpdateCurrentEvent()) {
            ServerApi.currentEvent(
                    AppPreferences.getUserId(MainActivity.this),
                    AppPreferences.getUserSecret(MainActivity.this),
                    new ServerApi.CurrentEventListener() {
                        @Override
                        public void onCurrentEvent(CurrentEventInfo eventInfo, int code) {
                            if (null != eventInfo || 404 == code) {
                                UserInfo userInfo = AppPreferences.getUserInfo(MainActivity.this);
                                userInfo.setCurrentEventObject((null != eventInfo) ? eventInfo : null);
                                AppPreferences.setUserInfo(MainActivity.this, userInfo);
                                refreshEventUi();
                            }
                        }
                    });
        }
    }

    private boolean needToUpdateCurrentEvent() {
        UserInfo userInfo = getCurrentUserInfo();

        if (null != userInfo.getCurrentEventObject()) {
            return true;
        }

        return false;
    }

    //endregion

    //region Balance

    private void updateBalance() {
        ServerApi.getBalance(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                new ServerApi.OnBalanceListener() {
                    @Override
                    public void onBalance(BalanceInfo balanceInfo) {
                        if (null != balanceInfo && null != balanceInfo.getBalance()) {
                            UserInfo userInfo = getCurrentUserInfo();
                            userInfo.initBalanceInfo(balanceInfo.getBalance());
                            AppPreferences.setUserInfo(MainActivity.this, userInfo);

                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    refreshTodayAndBalanceUI();
                                }
                            });
                        }
                    }
                });
    }

    //endregion

    //region Today

    private void updateToday() {
        if (null != _serviceApi) {
            try {
                _serviceApi.refreshToday();
            } catch (Exception e) {
                BitwalkingApp.getInstance().trackException(new Exception("updateToday: failed", e));
            }
        }
    }

    //endregion

    //region Avatar

    private void loadAvatar() {
        new AppPreferences(getBaseContext()).loadAvatarFromServer(new FinishAction(new FinishAction.OnFinishListener() {
            @Override
            public void onFail(int id) {
                handleUserDataLoaded(_PROFILE_IMAGE_LOADED_IDX);
            }

            @Override
            public void onFinish(int id) {
                refreshProfileImage();
                handleUserDataLoaded(_PROFILE_IMAGE_LOADED_IDX);
            }
        }));
    }

    //endregion

    //region First Data Load

    private static final int _PROFILE_IMAGE_LOADED_IDX = 0;
    private static final int _TODAY_LOADED_IDX = 1;
    ProgressDialog _loadingProgress;

    //region Loading timeout
    Handler _loadingTimeoutHandler;

    Runnable _loadingTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (dataLoaded) {
                Logger.instance().Log(Logger.DEBUG, TAG, "loading user data timed out");
                for (int i = 1; i < dataLoaded.length; ++i)
                    dataLoaded[i] = true;
                dataLoaded[0] = false;
            }

            handleUserDataLoaded(0);
        }
    };

    private void stopLoadingUserDataTimeout() {
        if (null != _loadingTimeoutHandler)
            _loadingTimeoutHandler.removeCallbacks(_loadingTimeoutRunnable);
        _loadingTimeoutHandler = null;
    }

    private void restartLoadingUserDataTimeout() {
        stopLoadingUserDataTimeout();
        _loadingTimeoutHandler = new Handler();
        _loadingTimeoutHandler.postDelayed(_loadingTimeoutRunnable, 10000);
    }

    //endregion

    Boolean dataLoaded[] = new Boolean[] {
            false, // profile image
//            false // today
    };

    void destroyProgress() {
        if (null != _loadingProgress && _loadingProgress.isShowing())
            _loadingProgress.dismiss();
    }

    private synchronized void handleUserDataLoaded(int idx) {
        if (idx < 0 || idx >= dataLoaded.length || dataLoaded[idx])
            return;

        boolean done = true;
        synchronized (dataLoaded) {
            dataLoaded[idx] = true;

            for (Boolean b : dataLoaded)
                done = done && b;
        }

        if (done) {
            stopLoadingUserDataTimeout();
            // done loading all data
            updateSlideMenuInfo();
            initUserData();
            refreshEventUi();

            destroyProgress();
            _loadingProgress = null;

//            buildFitnessClient();
//            startGoogleApiService();
        }
    }

    private void loadUserData() {
        _loadingProgress = new ProgressDialog(MainActivity.this);
        _loadingProgress.setMessage("Loading user data ...");
        _loadingProgress.setCancelable(false);
        _loadingProgress.show();

        synchronized (dataLoaded) {
            restartLoadingUserDataTimeout();
            for (int i = 0; i < dataLoaded.length; ++i)
                dataLoaded[i] = false;
        }

        new LoadUserDataTask().execute();
    }

    private class LoadUserDataTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Load profile
                loadAvatar();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private void migrationFailed(String reason) {
        BitwalkingApp.getInstance().trackException(reason, new Exception("Migration failed"));
        destroyProgress();
        logOut();
    }

    private void fixMigration() {
        _loadingProgress = new ProgressDialog(MainActivity.this);
        _loadingProgress.setMessage("Loading user data ...");
        _loadingProgress.setCancelable(false);
        _loadingProgress.show();

        ServerApi.getMe(
                AppPreferences.getUserId(MainActivity.this),
                AppPreferences.getUserSecret(MainActivity.this),
                new ServerApi.OnMeListener() {
                    @Override
                    public void onMe(MeInfo meInfo) {
                        if (null == meInfo) {
                            migrationFailed("meInfo is null");
                            return;
                        }

                        UserInfo userInfo = new UserInfo();
                        userInfo.setMeInfo(meInfo);
                        userInfo.initAuthInfo(AppPreferences.getUserSecret(MainActivity.this));
                        AppPreferences.setUserInfo(MainActivity.this, userInfo);

                        ServerApi.getBalance(
                                AppPreferences.getUserId(MainActivity.this),
                                AppPreferences.getUserSecret(MainActivity.this),
                                new ServerApi.OnBalanceListener() {
                                    @Override
                                    public void onBalance(BalanceInfo balanceInfo) {
                                        if (null == balanceInfo) {
                                            migrationFailed("balanceInfo is null");
                                            return;
                                        }

                                        UserInfo userInfo = getCurrentUserInfo();
                                        userInfo.initBalanceInfo(balanceInfo.getBalance());
                                        AppPreferences.setUserInfo(MainActivity.this, userInfo);
                                        startBitwalkingService();
                                        destroyProgress();
                                    }
                                });
                    }
                }
        );
    }

    //endregion





}
