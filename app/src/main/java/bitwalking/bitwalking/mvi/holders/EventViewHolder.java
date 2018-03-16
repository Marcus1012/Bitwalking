package bitwalking.bitwalking.mvi.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by alexey on 23.08.17.
 */

public class EventViewHolder extends RecyclerView.ViewHolder {

    private TextView textView;
    private ImageView backView;

    public EventViewHolder(View itemView) {
        super(itemView);
        textView = (TextView)itemView.findViewById(R.id.textView);
        backView = (ImageView)itemView.findViewById(R.id.backView);
    }
    public void bind(EventsListResponse.EventInfo item){
        textView.setText(item.title);
           Glide.with(backView.getContext())
                .load(item.images.banner)
                .into(backView);
    }




}