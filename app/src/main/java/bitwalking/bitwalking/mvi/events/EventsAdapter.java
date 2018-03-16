package bitwalking.bitwalking.mvi.events;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.holders.EventViewHolder;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by alexey on 23.08.17.
 */

public class EventsAdapter extends RecyclerView.Adapter<EventViewHolder> {

    private List<EventsListResponse.EventInfo> items = new ArrayList<>();


    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_event_raw, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    public void setItems(List<EventsListResponse.EventInfo> items) {
        this.items = items;
    }
}

