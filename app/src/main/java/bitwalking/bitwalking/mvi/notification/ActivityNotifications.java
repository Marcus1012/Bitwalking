package bitwalking.bitwalking.mvi.notification;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.hannesdorfmann.mosby3.mvi.MviActivity;

import bitwalking.bitwalking.R;
import io.reactivex.Observable;

/**
 * Created by Alexey on 02.06.2017.
 */

public class ActivityNotifications extends MviActivity<NotificationView,NotificationPresenter> implements NotificationView {
    public static final String TAG = "ActivityNotifications";

    ProgressBar loadingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_notifications_layout);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        loadingView = (ProgressBar)findViewById(R.id.loadingView);
    }

    @NonNull
    @Override
    public NotificationPresenter createPresenter() {
        return new NotificationPresenter();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
       // ((BitwalkingApp)getApplication()).trackScreenView("events");
    }
    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public Observable<Boolean> loadFirstPageIntent() {
         return Observable.just(true).doOnComplete(() -> Log.d(TAG,"firstPage completed"));
    }

    @Override
    public Observable<Boolean> loadNextPageIntent() {
        return null;
    }

    @Override
    public Observable<Boolean> pullToRefreshIntent() {
        return null;
    }

    @Override public void render(NotificationState state) {
      if (state.getError()!=null){
          renderError();
      }else if (state.isLoading()){
          renderLoading();
      }
    }

    private void renderError() {
        TransitionManager.beginDelayedTransition((ViewGroup) getWindow().getDecorView().getRootView());

    }

    private void renderData(){

    }

    private void renderEmpty(){

    }

    private void renderLoadMore(){

    }

    private void renderLoading(){
        TransitionManager.beginDelayedTransition((ViewGroup) getWindow().getDecorView().getRootView());
        loadingView.setVisibility(View.VISIBLE);
    }

}
