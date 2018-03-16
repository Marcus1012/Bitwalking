package bitwalking.bitwalking.vote_product;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;

import bitwalking.bitwalking.server.responses.GetSurveyResponse;
import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 4/12/16.
 */
public class VoteProductLocal {

    private static final String VOTE_PRODUCT_PREFERENCES = "VoteProductPreferences";
    SharedPreferences _voteProductPrefs;
    Context _context;
    Gson _gson;

    public VoteProductLocal(Context context) {
        _context = context;
        _voteProductPrefs = _context.getSharedPreferences(VOTE_PRODUCT_PREFERENCES, Context.MODE_PRIVATE);

        _imagesFolder = createProductsImagesFolder();
        _gson = new Gson();
    }

    //region Images

    final String IMAGES_FOLDER = "products_images";
    File _imagesFolder;

    public void storeImage(String productName, Bitmap image) {
        String imageName = String.format("%s.bmp", productName);
        File imageFile = new File(_imagesFolder.getPath() + "/" + imageName);

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteImage(String productName) {
        String imageName = String.format("%s.bmp", productName);
        File imageFile = new File(_imagesFolder.getPath() + "/" + imageName);

        if (imageFile.exists())
            imageFile.delete();
    }

    /*
     * This method will return the banner out of downloaded banner and not out of the market list
     */
    public Bitmap getStoredImage(String productName) {
        Bitmap image = null;

        String imageName = String.format("%s.bmp", productName);
        File imageFile = new File(_imagesFolder.getPath() + "/" + imageName);

        if (imageFile.exists()) {
            try {
                image = BitmapFactory.decodeStream(new FileInputStream(imageFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return image;
    }

    public boolean imageExists(String productName) {
        String imageName = String.format("%s.bmp", productName);
        File imageFile = new File(_imagesFolder.getPath() + "/" + imageName);

        return imageFile.exists();
    }

    public File createProductsImagesFolder() {
        File myDir = new File(_context.getFilesDir(), IMAGES_FOLDER);
        if (!myDir.exists())
            myDir.mkdirs();

        return myDir;
    }

    private static final String VOTE_KEY = "Survey.Info";
    private static final String VOTE_USER_CHOICE_ID_KEY = "Survey.UserChoiceId";
    private static final String VOTE_IS_NEW = "Vote.IsNew";

    // ---------- vote info
    public void setVote(GetSurveyResponse.SurveyInfo vote) {
        storeValueInMarketPreferences(VOTE_KEY, _gson.toJson(vote));
    }

    public GetSurveyResponse.SurveyInfo getVote() {
        GetSurveyResponse.SurveyInfo vote = null;

        try {
            vote = _gson.fromJson(getValueFromMarketPreferences(VOTE_KEY), GetSurveyResponse.SurveyInfo.class);
        }
        catch (Exception e) {
        }

        return vote;
    }

    // ---------- user choice + timestamp
    public void setUserChoice(String choiceId) {
        storeValueInMarketPreferences(VOTE_USER_CHOICE_ID_KEY, choiceId);
//        storeValueInMarketPreferences(VOTE_USER_CHOICE_TIMESTAMP_KEY, Globals.getFullDateFormat().format(new Date()));
    }

    public String getUserChoiceId() {
        return getValueFromMarketPreferences(VOTE_USER_CHOICE_ID_KEY);
    }

    // ----------- help method
    private Date getTimestamp(String key) {
        Date timestamp = null;

        try {
            String temp = getValueFromMarketPreferences(key);
            timestamp = Globals.getFullDateFormat().parse(temp);
        }
        catch (Exception e) {
            // ignore
        }

        return timestamp;
    }

    // ---------- vote info
    public void setIsNewVote(boolean isNewVote) {
        storeValueInMarketPreferences(VOTE_IS_NEW, String.valueOf(isNewVote));
    }

    public boolean getIsNewVote() {
        String isNewString = getValueFromMarketPreferences(VOTE_IS_NEW);
        if (null == isNewString)
            isNewString = String.valueOf(false);
        Boolean isNew = Boolean.parseBoolean(isNewString);
        return isNew;
    }

    //endregion

    //region Common

    private void storeValueInMarketPreferences(String key, String value) {
        Globals.setPreferencesKey(_voteProductPrefs, key, value);
    }

    private String getValueFromMarketPreferences(String key) {
        return Globals.getPreferencesKey(_voteProductPrefs, key);
    }

    //endregion
}
