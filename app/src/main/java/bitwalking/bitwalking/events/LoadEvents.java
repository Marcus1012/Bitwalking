package bitwalking.bitwalking.events;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by Marcus on 9/27/16.
 */
public class LoadEvents extends AsyncTask<Void, Void, Void> {

    Context _context;
    ManageImagesDownload _manageImages;
    OnEventsListener _EventsReadyListener;
    ArrayList<EventsListResponse.EventInfo> _events;

    public LoadEvents(Context context, OnEventsListener listener) {
        _context = context;
        _manageImages = new ManageImagesDownload(_context, EventsGlobals.EVENTS_IMAGES_FOLDER);
        _EventsReadyListener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        getEvents();

        return null;
    }

    private void getEvents() {
        ServerApi.getEvents(
                AppPreferences.getUserId(_context),
                AppPreferences.getUserSecret(_context),
                new ServerApi.EventsListener() {
                    @Override
                    public void onEvents(ArrayList<EventsListResponse.EventInfo> eventsInfo, int code) {
                        if (444 == code) {
                            if (null != _EventsReadyListener)
                                _EventsReadyListener.onEventsLoadError();
                        }
                        else {
                            _events = eventsInfo;
                            loadEventsData();
                        }
                    }
                });
    }

    private void loadEventsData() {
        if (null != _events && _events.size() > 0) {

            // Sort events by start date
            Collections.sort(_events);

            // Download images
            // todo: for now we will download all images, later maybe change it
            for (EventsListResponse.EventInfo event : _events) {
                downloadEventImages(event);
            }
        }

        if (null != _EventsReadyListener)
            _EventsReadyListener.onEventsLoaded(_events, _manageImages);
    }

    private void downloadEventImages(EventsListResponse.EventInfo event) {
        _manageImages.getImage(event.getBannerImageName(), event.images.banner);
        _manageImages.getImage(event.getListImageName(), event.images.list);
        _manageImages.getImage(event.getSponsrImageName(), event.images.sponsor);
    }

    public interface OnEventsListener {
        void onEventsLoaded(ArrayList<EventsListResponse.EventInfo> events, ManageImagesDownload manageImages);
        void onEventsLoadError();
    }
}
