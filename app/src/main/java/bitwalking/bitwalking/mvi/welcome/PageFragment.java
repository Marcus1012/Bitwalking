package bitwalking.bitwalking.mvi.welcome;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bitwalking.bitwalking.R;

/**
 * Created by User on 15.08.2017.
 */

public class PageFragment extends Fragment {

    static final String TEXT1 = "TEXT1";
    static final String TEXT2 = "TEXT2";
    private String text1,text2;
    private TextView text1View,text2View;


    static PageFragment newInstance(String text1,String text2) {
        PageFragment pageFragment = new PageFragment();
        Bundle arguments = new Bundle();
        arguments.putString(TEXT1, text1);
        arguments.putString(TEXT2, text2);
        pageFragment.setArguments(arguments);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        text1 = getArguments().getString(TEXT1);
        text2 = getArguments().getString(TEXT2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_fragment_text, null);
        text1View = (TextView)view.findViewById(R.id.text1View);
        text2View = (TextView)view.findViewById(R.id.text2View);

        text1View.setText(text1);
        text2View.setText(text2);

        return view;
    }
}
