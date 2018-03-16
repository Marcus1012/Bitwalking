package bitwalking.bitwalking.mvi.events;

import android.content.Context;

import com.hannesdorfmann.mosby3.mvp.MvpView;

import io.reactivex.Observable;

/**
 * Created by alexey on 25.08.17.
 */

public interface ViewEvents extends MvpView {
    void render(StateEvents state);
    Observable<Context> loadFirstPageIntent();
}
