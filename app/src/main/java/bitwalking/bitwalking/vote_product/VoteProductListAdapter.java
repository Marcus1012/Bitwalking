package bitwalking.bitwalking.vote_product;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.util.DolceVitaTextView;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 4/12/16.
 */
public class VoteProductListAdapter extends BaseAdapter {

    private static String TAG = "VoteProductListAdapter";
    private Context context;
    private ArrayList<VoteProductDrawableInfo> _products;
    boolean[] animationStates;

    public VoteProductListAdapter(Context context, ArrayList<VoteProductDrawableInfo> products){
        this.context = context;
        this._products = products;

        // init animation state
        animationStates = new boolean[_products.size()];
        for (int i = 0; i < animationStates.length; ++i)
            animationStates[i] = false;
    }

    @Override
    public int getCount() {
        return _products.size();
    }

    @Override
    public Object getItem(int position) {
        return _products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get current menu item
        VoteProductDrawableInfo currentProduct = _products.get(position);

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = mInflater.inflate(R.layout.vote_product_item, null);

            if (!animationStates[position]) {
                Logger.instance().Log(Logger.VERB, TAG, String.format("animate view %d", position));

                animationStates[position] = true;
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                animation.setStartOffset(position * 200);
                convertView.startAnimation(animation);
            }
        }

        DolceVitaTextView txtName = (DolceVitaTextView) convertView.findViewById(R.id.vote_product_item_text);
        ImageView imgImage = (ImageView) convertView.findViewById(R.id.vote_product_item_image);
        ImageView imgSelected = (ImageView) convertView.findViewById(R.id.vote_product_item_select_image);

        // Set itemName name
        txtName.setText(currentProduct.getName());
        txtName.setTypeface(null, Typeface.BOLD);
//        if (currentProduct.isSelected()) {
//            txtName.setBackgroundColor(Color.BLACK);
//            txtName.setTextColor(Color.WHITE);
//        }
//        else {
//            txtName.setBackgroundColor(Color.TRANSPARENT);
//            txtName.setTextColor(Color.BLACK);
//        }

        // Set itemName image
        imgImage.setImageBitmap(currentProduct.getImage());
        // Set itemName select state
        imgSelected.setImageResource(
                currentProduct.isSelected() ? R.drawable.vote_product_selected_v : R.drawable.vote_product_empty_v);

        convertView.setTag(currentProduct.getName());

        Logger.instance().Log(Logger.VERB, TAG, String.format("Get itemName '%s'", currentProduct.getName()));

        return convertView;
    }

}
