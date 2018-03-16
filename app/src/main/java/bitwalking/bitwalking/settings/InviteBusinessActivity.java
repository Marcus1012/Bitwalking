package bitwalking.bitwalking.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.BusinessInviteRequest;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/22/16.
 */
public class InviteBusinessActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = InviteBusinessActivity.class.getSimpleName();
    private static final int PLACE_PICKER_REQUEST_CODE = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 3;
    private static final int SEND_BUSINESS_SUGGESTION_ID = 4;

    private boolean _handlingClick = false;
    ProgressDialog _progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_business_activity);

        // Google Api Client
        _googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGoogleApiService();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        ((BitwalkingApp)getApplication()).trackScreenView("invite.business");
    }

    //region Google Places

    private GoogleApiClient _googleApiClient;
    private boolean _resolvingError = false;

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

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        String log = String.format("google api Failed - error [%d]: %s", result.getErrorCode(), result.getErrorMessage());
        Logger.instance().Log(Logger.DEBUG, TAG, log);
        Toast.makeText(InviteBusinessActivity.this, log, Toast.LENGTH_SHORT);
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
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.

                Toast.makeText(InviteBusinessActivity.this, "resolution error", Toast.LENGTH_SHORT);
                _googleApiClient.connect();
            }
        } else {
            Logger.instance().Log(Logger.DEBUG, TAG, "google api - cannot resolve");
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            GoogleApiAvailability.getInstance().showErrorDialogFragment(this, result.getErrorCode(), REQUEST_GOOGLE_PLAY_SERVICES);

            _resolvingError = true;
        }
    }

    public void onFindPlaceClick(View v) {
        if (_handlingClick)
            return;

        _handlingClick = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                _handlingClick = false;
            }
        }, 2000);

        _progress = new ProgressDialog(InviteBusinessActivity.this);
        _progress.setMessage("Loading places ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "Place Picker failed.");
            _progress.dismiss();
        }
    }

    private void sendBusinessSuggestionGoogleInfo(BusinessInfo businessInfo) {
        BusinessInviteRequest businessPayload = new BusinessInviteRequest(businessInfo);

        ServerApi.suggestBusiness(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                businessPayload,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        InviteBusinessActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (_progress.isShowing())
                                    _progress.dismiss();

                                if (200 == code) {
                                    String popupText = "Thanks for recommending Bitwalking to this business! Please invite more businesses to accept W$";
                                    Message popupMsg = _mainMsgHandler.obtainMessage(POPUP_MSG, popupText);
                                    popupMsg.sendToTarget();
                                }
                                else { // illegal parameter
                                    String toastText = "Failed to send business suggestion.";
                                    Message toastMsg = _mainMsgHandler.obtainMessage(TOAST_MSG, toastText);
                                    toastMsg.sendToTarget();
                                }
                            }
                        });
                    }
                });
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.instance().Log(Logger.DEBUG, TAG, "on activity result");

        // Pick Place
        if (requestCode == PLACE_PICKER_REQUEST_CODE) {
            destroyProgress();

            if (resultCode == RESULT_OK) {
                _progress = new ProgressDialog(InviteBusinessActivity.this);
                _progress.setMessage("Sending ...");
                _progress.setCancelable(false);
                _progress.show();

                try {
                    Place place = PlacePicker.getPlace(InviteBusinessActivity.this, data);

                    ArrayList<String> businessCategories = new ArrayList<>();
                    for (Integer c : place.getPlaceTypes())
                        businessCategories.add(String.valueOf(c));

                    BusinessInfo.BusinessLocation businessLocation = new BusinessInfo.BusinessLocation(
                            place.getLatLng().latitude, place.getLatLng().longitude);

                    BusinessInfo businessInfo = new BusinessInfo(
                            place.getName().toString(),
                            place.getId(),
                            (null != place.getAddress()) ? place.getAddress().toString() : null,
                            businessLocation,
                            (null != place.getPhoneNumber()) ? place.getPhoneNumber().toString() : null,
                            (null != place.getWebsiteUri()) ? place.getWebsiteUri().toString() : null,
                            businessCategories,
                            "GOOGLE_PLACES_API"
                    );

                    sendBusinessSuggestionGoogleInfo(businessInfo);
                }
                catch (Exception e) {
                    String msg = "Failed to send suggestion.";
                    Toast.makeText(InviteBusinessActivity.this, msg, Toast.LENGTH_SHORT).show();

                    destroyProgress();
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                String msg = "Failed to get selected place.";
                Toast.makeText(InviteBusinessActivity.this, msg, Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }

        // Google api client
        if (requestCode == REQUEST_RESOLVE_ERROR || requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
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
                Toast.makeText(InviteBusinessActivity.this, "Bitwalking failed to run Google Places Api", Toast.LENGTH_SHORT);
            }
        }
    }

    private static final int TOAST_MSG = 1;
    private static final int POPUP_MSG = 2;
    Handler _mainMsgHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case TOAST_MSG: {
                    Toast.makeText(InviteBusinessActivity.this, (String)message.obj, Toast.LENGTH_SHORT).show();
                    break;
                }
                case POPUP_MSG: {
                    showPopup((String)message.obj);
                    break;
                }
                default: break;
            }
        }
    };

    PopupWindow _popupWindow;
    public void showPopup(String text) {

        if (null != _popupWindow)
            _popupWindow.dismiss();

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.my_simple_show_text_popup, null);
        _popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        // Set text
        final TextView textView = (TextView)popupView.findViewById(R.id.simple_popup_text);
//        textView.setText(Html.fromHtml(text));
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (320 * scale + 0.5f);
        Globals.INSTANCE.fitText(textView, pixels, text);

        popupView.findViewById(R.id.simple_popup_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _popupWindow.dismiss();
            }
        });

        _popupWindow.setOutsideTouchable(true);
        _popupWindow.setTouchable(true);
        _popupWindow.setFocusable(true);
        _popupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        _popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -150);

        _popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    }

    //endregion

    public static class BusinessInfo {
        private String name;
        private String id;
        private String address;
        private String phone;
        private String uri;
        private BusinessLocation location;
        private List<String> categories;
        private String source;

        public BusinessInfo(String name, String id, String address, BusinessLocation location,
                            String phone, String uri, List<String> categories, String source) {
            this.name = name;
            this.id = id;
            this.address = address;
            this.phone = phone;
            this.uri = uri;
            this.location = location;
            this.categories = new ArrayList<>(categories);
            this.source = source;
        }

        public static class BusinessLocation {
            public double lat;
            public double lon;

            public BusinessLocation(double lat, double lon) {
                this.lat = lat;
                this.lon = lon;
            }
        }
    }
}
