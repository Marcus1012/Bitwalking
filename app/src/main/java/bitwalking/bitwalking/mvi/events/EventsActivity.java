package bitwalking.bitwalking.mvi.events;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.events.EventSpecificActivity;
import bitwalking.bitwalking.events.EventsGlobals;
import bitwalking.bitwalking.events.ManageImagesDownload;
import bitwalking.bitwalking.mvi.MviBtwActivity;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import bitwalking.bitwalking.util.Globals;
import io.reactivex.Observable;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * Created by Marcus on 9/9/16.
 */
public class EventsActivity extends MviBtwActivity<ViewEvents,PresenterEvents> implements ViewEvents{

    private static final String TAG = EventsActivity.class.getSimpleName();



    ArrayList<EventsListResponse.EventInfo> _events;
    EventsListResponse.EventInfo _featuredEvent;
    ManageImagesDownload _manageImages;
    ProgressDialog _progress;
    String _openEvent;

    private RecyclerView eventsView;
    private EventsAdapter adapter;
    private View emptyView;
    private View backView;

    @NonNull
    @Override
    public PresenterEvents createPresenter() {
        return new PresenterEvents();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.events_activity);

        eventsView = (RecyclerView)findViewById(R.id.eventsView);
        emptyView = findViewById(R.id.emptyView);
        _openEvent = getIntent().getStringExtra(Globals.BITWALKING_EVENT_ID);
        backView = findViewById(R.id.backView);
        initList();
       // new LoadEvents(this, this).execute();


        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        eventsView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == SCROLL_STATE_IDLE){
                    backView.animate().alpha(1f)
                            .setDuration(300)
                            .setInterpolator(new LinearInterpolator())
                            .start();
                }else {
                    backView.animate().alpha(0f)
                            .setDuration(300)
                            .setInterpolator(new LinearInterpolator())
                            .start();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    private void initList() {
        adapter = new EventsAdapter();
        eventsView.setLayoutManager(new LinearLayoutManager(this));
        eventsView.setAdapter(adapter);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        ((BitwalkingApp)getApplication()).trackScreenView("events");
    }

    @Override
    public void render(StateEvents state) {
       if (state.isLoading()){
           renderLoading();
       }else if (null!=state.getError()){
           renderError();
       }else if (null!=state.getEvents() && state.getEvents().size()>0){
           renderList(state.getEvents());
       }else {
           renderEmpty();
       }
    }

    private void renderLoading(){
        findViewById(R.id.events_loading_filter).setVisibility(View.VISIBLE);
        _progress = new ProgressDialog(EventsActivity.this);
        _progress.setMessage("Loading events ...");
        _progress.setCancelable(true);
        _progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onBackPressed();
            }
        });
        _progress.show();
    }

    private void renderList(List<EventsListResponse.EventInfo> events){
        adapter.setItems(events);
        adapter.notifyDataSetChanged();
        destroyProgress();
        hideFilter();
    }

    private void renderEmpty(){
        destroyProgress();
        hideFilter();
        emptyView.setVisibility(View.VISIBLE);
    }

    private void renderError(){
        _events = null;
        _manageImages = null;

        destroyProgress();
        hideFilter();

        Globals.showServerConnectionErrorMsg(EventsActivity.this);
        onBackPressed();
    }

    @Override
    public Observable<Context> loadFirstPageIntent() {
         return Observable.just(getApplicationContext()).doOnComplete(() -> Log.d(TAG,"firstPage completed"));
    }

    @Override
    protected void onBwServiceConnected() {

    }

    @Override
    protected void onBwServiceDisconnected() {

    }

    private void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }
/*
    @Override
    public void onEventsLoadError() {
        EventsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                _events = null;
                _manageImages = null;

                destroyProgress();
                hideFilter();

                Globals.showServerConnectionErrorMsg(EventsActivity.this);
                onBackPressed();
            }
        });
    }*/

  /*  @Override
    public void onEventsLoaded(final ArrayList<EventsListResponse.EventInfo> events, final ManageImagesDownload manageImages) {
        EventsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                _events = events;
                _manageImages = manageImages;
                adapter.setItems(events);
                destroyProgress();
                hideFilter();

                if (null != _events && _events.size() > 0) {

                } else {
                    setContentView(R.layout.events_static_activity);
                }
            }
        });
    }*/

    private void hideFilter() {
        Animation animation = AnimationUtils.loadAnimation(EventsActivity.this, R.anim.fade_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                findViewById(R.id.events_loading_filter).setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setStartOffset(0);
        findViewById(R.id.events_loading_filter).startAnimation(animation);
    }

    @Deprecated
    public void onFeaturedEventClick(View v) {
        onEventClick(_featuredEvent);
        ((BitwalkingApp)getApplication()).trackEvent("events", "open.event", "source.featured");
    }

    public void onEventClick(EventsListResponse.EventInfo clickedEvent) {
        if (null != clickedEvent) {
            Intent intent = new Intent(EventsActivity.this, EventSpecificActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(EventsGlobals.EVENT_ID_KEY, clickedEvent.id);
            startActivity(intent);
        }
    }

    @Deprecated
    private void fillFeaturedEvent() {
        _featuredEvent = _events.get(0);
        for (EventsListResponse.EventInfo e : _events) {
            if (e.featured.booleanValue()) {
                _featuredEvent = e;
                break;
            }
        }

      /*  ImageView image = (ImageView)findViewById(R.id.events_main_image);
        image.setImageBitmap(_manageImages.getImage(_featuredEvent.getBannerImageName(), _featuredEvent.images.banner));

        TextView title = (TextView)findViewById(R.id.events_main_title);
        title.setText(_featuredEvent.title);

        findViewById(R.id.events_featured_layout).setVisibility(View.VISIBLE);
        Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animation1.setStartOffset(0);
        findViewById(R.id.events_featured_layout).startAnimation(animation1);

        findViewById(R.id.events_upcoming_layout).setVisibility(View.VISIBLE);
        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animation2.setStartOffset(100);
        findViewById(R.id.events_upcoming_layout).startAnimation(animation2);
        Logger.instance().Log(Logger.VERB, TAG, String.format("animate featured event"));*/
    }

    //region Page Listen


    //endregion


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        /*
        #30
        EventsActivity.java line 97
        bitwalking.bitwalking.EventsActivity.onBackPressed
         */
        //No call for super(). Bug on API Level > 11.
        //http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa/10261438#10261438
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}
