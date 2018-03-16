package bitwalking.bitwalking.mvi.events;

import android.content.Context;

import com.google.gson.Gson;
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;

import java.util.Collections;
import java.util.List;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by alexey on 25.08.17.
 */

public class PresenterEvents extends MviBasePresenter<ViewEvents,StateEvents> {

    private Gson gson=new Gson();

    @Override
    protected void bindIntents() {
       Observable<StateEvents> loadFirstPage = intent(ViewEvents::loadFirstPageIntent)
                .flatMap(this::loadPage)
                .map(items -> new StateEvents(false, items,null) )
                .startWith(new StateEvents(true, Collections.emptyList(),null))
                .onErrorReturn(error -> new StateEvents(false, Collections.emptyList(), error))
                .observeOn(AndroidSchedulers.mainThread());

        subscribeViewState(loadFirstPage, ViewEvents::render);
    }


    private Observable<List<EventsListResponse.EventInfo>> loadPage(Context _context){
      return Observable.create(subscriber->{
         if (!subscriber.isDisposed()) {
             ServerApi.getEvents(
                     AppPreferences.getUserId(_context),
                     AppPreferences.getUserSecret(_context),
                     (eventsInfo, code) -> {
                         if (!subscriber.isDisposed()) {
                             if (444 == code) {
                                 subscriber.onError(new Exception("Request exception 444"));
                             } else {
                                 subscriber.onNext(eventsInfo);
                                 subscriber.onComplete();
                             }
                         }
                     });
         }
      });
    }
}
