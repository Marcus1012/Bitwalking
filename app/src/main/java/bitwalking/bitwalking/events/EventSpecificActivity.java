package bitwalking.bitwalking.events;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.events.eventInfo.EventInfoTabPager;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.activityes.WebActivity;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.JoinEventRequest;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import bitwalking.bitwalking.steps.telephony_info.TelephonyData;
import bitwalking.bitwalking.steps.telephony_info.TelephonyInfoManager;
import bitwalking.bitwalking.user_info.CurrentEventInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.PhoneLocation;
import bitwalking.bitwalking.util.PhoneNetwork;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventSpecificActivity extends FragmentActivity implements LoadSpecificEvent.OnEventInfoListener {
    private static final String TAG = EventSpecificActivity.class.getSimpleName();
    private static int SHARE_EVENT_REQ_ID = 1;

    private String _eventId;
    private EventsListResponse.EventInfo _eventInfo;
    private ManageImagesDownload _manageImages;
    private EventInfoTabPager _eventInfoPagerAdapter;
    private ViewPager _eventInfoPager;
    private LinearLayout _tabsLayout;
    private ImageView _mainImage;
    private TextView _mainTitle;
    private TextView _mainDate;
    private ImageView _heartImage;

    ProgressDialog _progress;

    private boolean _sharePressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_info_layout);

        _mainImage = (ImageView)findViewById(R.id.event_main_image);
        _mainTitle = (TextView)findViewById(R.id.event_info_title);
        _mainDate = (TextView)findViewById(R.id.event_info_date);
        _heartImage = (ImageView)findViewById(R.id.event_info_heart);
        findViewById(R.id.event_info_loading).setVisibility(View.VISIBLE);

        _eventId = getIntent().getStringExtra(EventsGlobals.EVENT_ID_KEY);
        loadEvent(_eventId);

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    public void onShareEventClick(View v) {
        if (_sharePressed)
            return;

        _sharePressed = true;
        findViewById(R.id.event_info_loading).setVisibility(View.VISIBLE);

        ((BitwalkingApp)getApplication()).trackEvent("event", "open.share", "id." + _eventId);

        EventSpecificActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String title = (null != _eventInfo.share && null != _eventInfo.share.title) ?
                            _eventInfo.share.title :
                            _eventInfo.title;
                    String text = (null != _eventInfo.share && null != _eventInfo.share.text) ?
                            _eventInfo.share.text :
                            String.format("Read more about: %s", _eventInfo.links.externalUri);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, title);
                    intent.putExtra(Intent.EXTRA_TEXT, text);
                    startActivityForResult(Intent.createChooser(intent, title), SHARE_EVENT_REQ_ID);
                }
                catch (Exception e) {
                    BitwalkingApp.getInstance().trackException(new Exception("failed to share event " + _eventId, e));
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHARE_EVENT_REQ_ID) {
            if (resultCode == RESULT_OK) {
                ((BitwalkingApp)getApplication()).trackEvent("event", "share", "success");
            } else {
                ((BitwalkingApp)getApplication()).trackEvent("event", "share", "cancel");
            }

            _sharePressed = false;
            findViewById(R.id.event_info_loading).setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        ((BitwalkingApp)getApplication()).trackScreenView("event");
    }

    private void loadEvent(String eventId) {
        new LoadSpecificEvent(eventId, this, this).execute();
    }

    public void onJoinEventClick(View v) {
        if (_eventInfo.status == EventsListResponse.EventStatus.completed ||
            _eventInfo.status == EventsListResponse.EventStatus.finished)
            openEventDashboard();
        else if (null == _eventInfo.me.joinTimestamp)
            askUserToJoin();
    }

    private void openEventDashboard() {
        ((BitwalkingApp)getApplication()).trackEvent("event", "external.link", "event_site");

        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", _eventInfo.links.externalUri);
        startActivity(intent);
    }

    private void askUserToJoin() {
        final String failedMessage = "You are about to join this event. All the W$ generated by you during the event period will be donated to the event cause. Are you ready to proceed?";
        AlertDialog.Builder builder = new AlertDialog.Builder(EventSpecificActivity.this);
        builder.setMessage(failedMessage)
                .setCancelable(true)
                .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (checkPermissions())
                            tryJoinEvent();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((BitwalkingApp)getApplication()).trackEvent("events", "join", "cancel");
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private Location getUserLocation() {
        Location location = new Location("no_location");
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                // Get from GPS
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    // Get alternative location
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                }
            }
        }

        return location;
    }

    private static final int LOCATION_ACCESS_PERMISSION_REQUEST_ID = 1;
    public boolean checkPermissions() {
        return Globals.havePermission(this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_ACCESS_PERMISSION_REQUEST_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_ACCESS_PERMISSION_REQUEST_ID:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    tryJoinEvent();
                }
                else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Bitwalking app needs to know your location to join event", Toast.LENGTH_SHORT).show();
                }
                break;
            default: break;
        }
    }

    private boolean isLocationOn() {
        boolean gps_enabled = false;
        boolean network_enabled = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to get location for event join");
            ex.printStackTrace();
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to get network for event join");
            ex.printStackTrace();
        }

        return gps_enabled || network_enabled;
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void tryJoinEvent() {
        if (!isLocationOn()) {
            Globals.showSimpleAlertMessage(EventSpecificActivity.this, "Cannot join event", "Location is required to join an event", "Dismiss");
            return;
        }

        // Showing progress dialog
        _progress = new ProgressDialog(EventSpecificActivity.this);
        _progress.setMessage("Joining event ...");
        _progress.setCancelable(false);
        _progress.show();

        JoinEventRequest.JoinEventLocation joinLocation = new JoinEventRequest.JoinEventLocation();
        try {
            Location location = getUserLocation();
            PhoneLocation phoneLocation = new PhoneLocation();
            phoneLocation.lat = location.getLatitude();
            phoneLocation.lon = location.getLongitude();
            joinLocation.location = phoneLocation;
        }
        catch (Exception e) {}

        try {
            PhoneNetwork phoneNetwork = new PhoneNetwork();
            TelephonyInfoManager telephonyManager = new TelephonyInfoManager(EventSpecificActivity.this);
            TelephonyData telephonyData = telephonyManager.getTelephonyInfo();
            phoneNetwork.cellId = String.valueOf(telephonyData.cellId);
            phoneNetwork.lac = String.valueOf(telephonyData.lac);
            phoneNetwork.mcc = String.valueOf(telephonyData.mcc);
            phoneNetwork.mnc = String.valueOf(telephonyData.mnc);
            joinLocation.network = phoneNetwork;
        }
        catch (Exception e) {}

        Logger.instance().Log(Logger.VERB, TAG, new GsonBuilder().setPrettyPrinting().create().toJson(joinLocation));

        sendJoinRequest(joinLocation);
    }

    private void sendJoinRequest(JoinEventRequest.JoinEventLocation joinLocation) {
        ServerApi.EventJoinInfo joinInfo = new ServerApi.EventJoinInfo();
        joinInfo.id = _eventId;
        joinInfo.location = joinLocation;

        ServerApi.joinEvent(
                AppPreferences.getUserId(EventSpecificActivity.this),
                AppPreferences.getUserSecret(EventSpecificActivity.this),
                joinInfo,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        EventSpecificActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                destroyProgress();

                                String toastMsg = "";
                                String alertMsg = "";
                                switch (code) {
                                    case 200:
                                        Toast.makeText(EventSpecificActivity.this, "Thanks for joining!", Toast.LENGTH_SHORT).show();
                                        _heartImage.setImageResource(R.drawable.event_heart_filled);
                                        _eventInfo.me.joinTimestamp = Globals.getUTCDateFormat().format(new Date());
                                        BitwalkingApp.getInstance().trackEvent("event", "join", "success");

                                        try {
                                            UserInfo userInfo = AppPreferences.getUserInfo(EventSpecificActivity.this);
                                            userInfo.setCurrentEventObject(new CurrentEventInfo());
                                            userInfo.getCurrentEventObject().eventId = _eventId;
                                            AppPreferences.setUserInfo(EventSpecificActivity.this, userInfo);
                                        } catch (Exception e) {
                                            Logger.instance().Log(Logger.ERROR, TAG, "failed to set current event object");
                                        }

                                        return;
                                    case 404: toastMsg = "Event not found"; break;
                                    case 4091: // made up code
                                        alertMsg = "You can't join because you're already signed on to another event at the same time";
                                        break;
                                    case 4092: // made up code
                                        toastMsg = "Cannot join event right now";
                                        break;
                                    case 412: {
                                        String geo = "";
                                        for (EventsListResponse.EventInfo.EventGeography eg : _eventInfo.geography) {
                                            if (eg.type != EventsListResponse.EventGeographyType.worldwide) {
                                                if (geo.isEmpty())
                                                    geo += eg.name;
                                                else
                                                    geo += " / " + eg.name;
                                            }
                                        }
                                        alertMsg = "You must be in " + geo + " to join this event";
                                        break;
                                    }
                                    default:
                                        break;
                                }

                                if (!toastMsg.isEmpty())
                                    Toast.makeText(EventSpecificActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                if (!alertMsg.isEmpty())
                                    Globals.showSimpleAlertMessage(EventSpecificActivity.this, "Join Failed", alertMsg, "Dismiss");

                                BitwalkingApp.getInstance().trackEvent("event", "join", "failure");
                            }
                        });
                    }
                });
    }

    @Override
    public void onEventInfoLoaded(EventsListResponse.EventInfo eventInfo, ManageImagesDownload manageImages) {
        _manageImages = manageImages;
        _eventInfo = eventInfo;
        EventSpecificActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildEventInfo();
                refreshInfo();
                findViewById(R.id.event_info_loading).setVisibility(View.GONE);
            }
        });
    }

    private void refreshInfo() {
        try {
            if (_eventInfo.extendedInformation.size() > 0) {
                // Load first section
                handleTabClick(_eventInfo.extendedInformation.get(0).sectionTitle);
            }

            // Load image
            _mainImage.setImageBitmap(_manageImages.getImage(_eventInfo.getBannerImageName(), _eventInfo.images.banner));
            // Load title
            _mainTitle.setText(_eventInfo.title);
            // Load date
            _mainDate.setText(convertEventPeriodDates(_eventInfo.startTime, _eventInfo.endTime));
            // Load heart image
            if (null != _eventInfo.me.joinTimestamp)
                _heartImage.setImageResource(R.drawable.event_heart_filled);
            else
                _heartImage.setImageResource(R.drawable.event_heart_empty);

            findViewById(R.id.event_info_top_layout).setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            animation.setStartOffset(0);
            findViewById(R.id.event_info_top_layout).startAnimation(animation);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("failed to refresh event info, id=" + _eventId, e));
        }
    }

    private String convertEventPeriodDates(String startUtc, String endUtc) {
        String eventPeriod = "";

        try {
            Date startDate = Globals.getUTCDateFormat().parse(startUtc);
            Date endDate = Globals.getUTCDateFormat().parse(endUtc);

            DateFormat monthDateFormatter = new SimpleDateFormat("MMM", Locale.ENGLISH);
            DateFormat dayDateFormatter = new SimpleDateFormat("dd", Locale.ENGLISH);

            String startMonth = monthDateFormatter.format(startDate);
            String endMonth = monthDateFormatter.format(endDate);

            if (startMonth.contentEquals(endMonth))
                eventPeriod = String.format("%s %s-%s", startMonth, dayDateFormatter.format(startDate), dayDateFormatter.format(endDate));
            else
                eventPeriod = String.format("%s %s - %s %s", startMonth, dayDateFormatter.format(startDate), endMonth, dayDateFormatter.format(endDate));
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        return eventPeriod;
    }

    private void buildEventInfo() {
        if (null == _eventInfo) {
            BitwalkingApp.getInstance().trackException(new Exception("_eventInfo is null, buildEventInfo"));
            return;
        }

        // Build sections titles
        _tabsLayout = (LinearLayout)findViewById(R.id.event_info_sections_tabs_layout);
        ArrayList<String> sectionsName = new ArrayList<>();
        for (EventsListResponse.EventSection sec : _eventInfo.extendedInformation)
            sectionsName.add(sec.sectionTitle);
        buildEventInfoTabs(_tabsLayout, sectionsName);

        // Build pager
        _eventInfoPagerAdapter = new EventInfoTabPager(getSupportFragmentManager(), _eventInfo);
        _eventInfoPager = (ViewPager) findViewById(R.id.event_info_sections_pager);
        _eventInfoPager.setAdapter(_eventInfoPagerAdapter);
        _eventInfoPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (_eventInfoPagerAdapter.getCount() > 0)
                    updateSelectedTab(_eventInfoPagerAdapter.getSectionName(position));
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void buildEventInfoTabs(LinearLayout ll, ArrayList<String> tabsNames) {

        for (String name : tabsNames) {

            RelativeLayout relativeLayout = new RelativeLayout(ll.getContext());
            relativeLayout.setOnClickListener(_onTabClick);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    1f);
            relativeLayout.setTag(name);

            // Creating a new TextView
            TextView tv = new TextView(ll.getContext());
            tv.setText(name);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setTextColor(0xFF000000);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setTag(name);
            tv.setOnClickListener(_onTabClick);

            // Defining the layout parameters of the TextView
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);

            // Setting the parameters on the TextView
            tv.setLayoutParams(lp);

            relativeLayout.addView(tv);

            ll.addView(relativeLayout, llp);
        }

        ll.setWeightSum(tabsNames.size());
    }

    private int updateSelectedTab(String sectionTitle) {
        int sectionIdx = -1;
        // Reset all tabs alpha
        for (int i = 0; i < _tabsLayout.getChildCount(); ++i) {
            View child = _tabsLayout.getChildAt(i); // RelativeLayout
            child = ((RelativeLayout)child).getChildAt(0); // TextView
            String childTitle = ((TextView)child).getText().toString();
            if (childTitle.contentEquals(sectionTitle)) {
                child.setAlpha(0.43f);
                sectionIdx = i;
            }
            else {
                child.setAlpha(1f);
            }
        }

        if (sectionIdx < 0) {
            // select first
            sectionIdx = 0;
            View child = _tabsLayout.getChildAt(0); // RelativeLayout
            child = ((RelativeLayout)child).getChildAt(0); // TextView
            child.setAlpha(0.43f);
        }

        return sectionIdx;
    }

    private void handleTabClick(String sectionTitle) {
        int sectionIdx = updateSelectedTab(sectionTitle);

        // Load selected section
        Logger.instance().Log(Logger.DEBUG, TAG, String.format("set section %s index %d", sectionTitle, sectionIdx));
        _eventInfoPager.setCurrentItem(sectionIdx);
    }

    View.OnClickListener _onTabClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            handleTabClick((String)v.getTag());
        }
    };
}
