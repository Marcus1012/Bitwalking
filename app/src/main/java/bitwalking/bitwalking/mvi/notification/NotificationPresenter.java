package bitwalking.bitwalking.mvi.notification;


import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;

public class NotificationPresenter extends MviBasePresenter<NotificationView,NotificationState>{
    @Override
    protected void bindIntents() {
       /* Observable<NotificationState> loadFirstPage = intent(NotificationView::loadFirstPageIntent)
                .doOnNext(ignored -> Log.d("NotificationPresenter","intent: load first page"))
                .flatMap(ignored -> feedLoader.loadFirstPage()
                        .map(items -> new FirstPageData(items))
                        .startWith(new PartialState.FirstPageLoading())
                        .onErrorReturn(PartialState.FirstPageError::new)
                        .subscribeOn(Schedulers.io()));


        Observable<PartialState> allIntents = Observable.merge(loadFirstPage, pullToRefresh);
        NotificationState initialState = ... ; // Show loading first page
        Observable<NotificationState> stateObservable = allIntents.scan(initialState, this::viewStateReducer);
        subscribeViewState(loadFirstPage,NotificationView::render);*/
    }



    private NotificationState viewStateReducer(NotificationState previousState, PartialState changes){
     return  changes.computeNewState(previousState);
    }


}
