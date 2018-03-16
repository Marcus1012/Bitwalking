package bitwalking.bitwalking.vote_product;

import android.graphics.Bitmap;

import java.util.BitSet;

/**
 * Created by Marcus on 4/12/16.
 */
public class VoteProductDrawableInfo {

    private String _id;
    private String _name;
    private Bitmap _image;
    private boolean _selected;

    public VoteProductDrawableInfo() {

    }

    public String getId() { return _id; }
    public void setId(String id) { _id = id; }

    public String getName() { return _name; }
    public void setName(String name) { _name = name; }

    public Bitmap getImage() { return _image; }
    public void setImage(Bitmap image) { _image = image; }

    public boolean isSelected() { return _selected; }
    public void setSelected(boolean selected) { _selected = selected; }
}
