package bitwalking.bitwalking.events;

import android.content.Context;
import android.os.AsyncTask;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by Marcus on 9/28/16.
 */
public class LoadSpecificEvent extends AsyncTask<Void, Void, Void> {

    Context _context;
    ManageImagesDownload _manageImages;
    OnEventInfoListener _eventInfoReadyListener;
    String _eventId;
    EventsListResponse.EventInfo _eventInfo;

    public LoadSpecificEvent(String eventId, Context context, OnEventInfoListener listener) {
        _eventId = eventId;
        _context = context;
        _manageImages = new ManageImagesDownload(_context, EventsGlobals.EVENTS_IMAGES_FOLDER);
        _eventInfoReadyListener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        getEvent();

        return null;
    }

    private void getEvent() {
        ServerApi.getEvent(
                AppPreferences.getUserId(_context),
                AppPreferences.getUserSecret(_context),
                _eventId,
                new ServerApi.EventListener() {
                    @Override
                    public void onEvent(EventsListResponse.EventInfo eventInfo) {
                        _eventInfo = eventInfo;
                        loadEventInfo();
                    }
                });
    }

    private void loadEventInfo() {
        if (null == _eventInfo)
            return;

        // Download images
        // todo: for now we will download all images, later maybe change it
        downloadEventImages(_eventInfo);

        if (null != _eventInfoReadyListener)
            _eventInfoReadyListener.onEventInfoLoaded(_eventInfo, _manageImages);
    }

    private void downloadEventImages(EventsListResponse.EventInfo event) {
        _manageImages.getImage(event.getBannerImageName(), event.images.banner);
        _manageImages.getImage(event.getListImageName(), event.images.list);
        _manageImages.getImage(event.getSponsrImageName(), event.images.sponsor);
    }

    public interface OnEventInfoListener {
        void onEventInfoLoaded(EventsListResponse.EventInfo eventInfo, ManageImagesDownload manageImages);
    }
}
