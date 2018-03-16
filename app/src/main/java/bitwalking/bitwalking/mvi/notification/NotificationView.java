package bitwalking.bitwalking.mvi.notification;


import com.hannesdorfmann.mosby3.mvp.MvpView;

import io.reactivex.Observable;

public interface NotificationView extends MvpView{
    Observable<Boolean> loadFirstPageIntent();
    Observable<Boolean> loadNextPageIntent();
    Observable<Boolean> pullToRefreshIntent();
    void render(NotificationState state);
}
