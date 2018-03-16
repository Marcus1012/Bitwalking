package bitwalking.bitwalking.events;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 9/27/16.
 */
public class EventsListAdapter extends BaseAdapter {

    private static String TAG = EventsListAdapter.class.getSimpleName();
    private Context _context;
    private ManageImagesDownload _manageImages;
    private ArrayList<EventsListResponse.EventInfo> _events;
    boolean[] animationStates;
    Bitmap[] eventsListImages;

    public EventsListAdapter(Context context, ManageImagesDownload manageImages, ArrayList<EventsListResponse.EventInfo> events){
        this._context = context;
        this._manageImages = manageImages;
        this._events = new ArrayList<>();
        boolean ignoreFeatured = false;

        for (EventsListResponse.EventInfo e : events)
            if (ignoreFeatured && e.featured.booleanValue())
                ignoreFeatured = false;
            else
                _events.add(e);

        // init animation state
        animationStates = new boolean[_events.size()];
        for (int i = 0; i < animationStates.length; ++i)
            animationStates[i] = false;

        eventsListImages = new Bitmap[_events.size()];
        for (int i = 0; i < eventsListImages.length; ++i)
            eventsListImages[i] = _manageImages.getImage(_events.get(i).getListImageName(), _events.get(i).images.list);
    }

    @Override
    public int getCount() {
        return _events.size();
    }

    @Override
    public Object getItem(int position) {
        return _events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class EventListViewHolder {
        public ImageView listImage;
        public TextView categoryText;
        public TextView eventTitle;
        public TextView startTimeText;
        public EventsListResponse.EventInfo eventInfo;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get current menu item
        EventsListResponse.EventInfo currentEvent = _events.get(position);
        EventListViewHolder holder;

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    _context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = mInflater.inflate(R.layout.event_item_layout, null);

            holder = new EventListViewHolder();
            holder.listImage = (ImageView)convertView.findViewById(R.id.event_list_image);
            holder.categoryText = (TextView) convertView.findViewById(R.id.event_list_category);
            holder.eventTitle = (TextView)convertView.findViewById(R.id.event_list_title);
            holder.startTimeText = (TextView) convertView.findViewById(R.id.event_list_start_time);
            holder.eventInfo = currentEvent;
            convertView.setTag(holder);

            if (!animationStates[position]) {
                Logger.instance().Log(Logger.VERB, TAG, String.format("animate list event  %d", position));

                animationStates[position] = true;
                Animation animation = AnimationUtils.loadAnimation(_context, R.anim.fade_in);
                animation.setStartOffset(200 + (position + 1) * 200);
                convertView.startAnimation(animation);
            }
        }
        else {
            holder = (EventListViewHolder)convertView.getTag();
        }

        // event image

//        listImage.setImageBitmap(_manageImages.getImage(currentEvent.getListImageName(), currentEvent.images.list));
        if (null == eventsListImages[position])
            eventsListImages[position] = _manageImages.getImage(currentEvent.getListImageName(), currentEvent.images.list);

        holder.listImage.setImageBitmap(eventsListImages[position]);

        // event category
        if (currentEvent.categories.size() > 0) {

            String category = currentEvent.categories.get(0);
            for (int i = 1; i < currentEvent.categories.size(); ++i)
                category += ", " + currentEvent.categories.get(i);
            holder.categoryText.setText(category);
        }

        // event title
        holder.eventTitle.setText(currentEvent.title);

        // event start time
        try {
            Date eventStartDate = Globals.getUTCDateFormat().parse(currentEvent.startTime);
            if (eventStartDate.before(new Date())) {
                Date eventEndDate = Globals.getUTCDateFormat().parse(currentEvent.endTime);
                if (eventEndDate.before(new Date())) {
                    holder.startTimeText.setText("Ended");
                }
                else {
                    holder.startTimeText.setText("Ongoing");
                }
            }
            else {
                holder.startTimeText.setText("Starts in " + Globals.getTimeDifferenceInNiceString(new Date(), eventStartDate));
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        return convertView;
    }

}