package bitwalking.bitwalking.events.eventInfo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventInfoTextFragment extends EventInfoSectionFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        fillText(rootView);

        return rootView;
    }

    private void fillText(View rootView) {
        // Give icon
        rootView.findViewById(R.id.event_info_section_give_icon).setVisibility(View.GONE);
        // Sub title
        TextView subTitle = (TextView)rootView.findViewById(R.id.event_info_text_section_sub_title);
        subTitle.setText(_section.sectionSubTitle);
        // Text
        TextView text = (TextView)rootView.findViewById(R.id.event_info_text_section_text);
        text.setText(Html.fromHtml(_section.sectionText));
        // set links clickable inside TextView
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
