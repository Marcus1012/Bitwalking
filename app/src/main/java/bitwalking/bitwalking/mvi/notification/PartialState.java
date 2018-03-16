package bitwalking.bitwalking.mvi.notification;


interface  PartialState {
    NotificationState computeNewState(NotificationState previousState);
}

class FirstPageLoading implements PartialState{

    public FirstPageLoading(boolean loading){

    }

    @Override
    public NotificationState computeNewState(NotificationState previousState) {
        return null;
    }
}

class FirstPageData implements PartialState{

    public FirstPageData(Object object){

    }

    @Override
    public NotificationState computeNewState(NotificationState previousState) {
        return null;
    }
}

/*class NextPageLoaded implements PartialState {
  //  private final List<feeditem> itemsOfNextPage;

    @Override
    public NotificationState computeNewState(NotificationState previousState) {
        List<feeditem> data = new ArrayList<>();
        data.addAll(previousState.getData());
        data.addAll(itemsOfNextPage);
        return previousState.builder()
                .data(data)
                .nextPageLoading(false)
                .nextPageError(null)
                .build();
    }
}*/
