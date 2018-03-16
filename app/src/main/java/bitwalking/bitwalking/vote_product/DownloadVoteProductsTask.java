package bitwalking.bitwalking.vote_product;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.google.gson.Gson;

import java.net.URL;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.GetSurveyResponse;
import bitwalking.bitwalking.server.responses.UserVoteResponse;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 4/12/16.
 */
public class DownloadVoteProductsTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = DownloadVoteProductsTask.class.getSimpleName();

    Context _context;
    Gson _gson = null;
    VoteProductLocal _voteLocal;
    OnVoteReady _voteReadyListener;

    public DownloadVoteProductsTask(Context context, OnVoteReady voteReadyListener) {
        _context = context;
        _voteReadyListener = voteReadyListener;

        _gson = new Gson();
        _voteLocal = new VoteProductLocal(_context);
    }

    protected Boolean doInBackground(Void... indexes) {
        getProducts();
        return true;
    }

    private boolean downloadAndStoreImage(String url, String name) {
        Bitmap image = null;
        try {
            Logger.instance().Log(Logger.VERB, "Download Image", url);
            image = BitmapFactory.decodeStream(
                    new URL(url).openConnection().getInputStream());
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.VERB, "ImagesAdapter", "Failed to load image url, set default");
            e.printStackTrace();
        }

        if (null != image)
            _voteLocal.storeImage(name, image);

        return (null != image);
    }

    //region Vote - Server

    private void getProducts() {
        ServerApi.getSurvey(
                AppPreferences.getUserId(_context),
                AppPreferences.getUserSecret(_context),
                new ServerApi.SurveyListener() {
                    @Override
                    public void onSurvey(GetSurveyResponse.SurveyInfo surveyInfo, int code) {
                        if (444 == code) {
                            if (null != _voteReadyListener)
                                _voteReadyListener.onVoteLoadError();
                        }
                        else {
                            if (null != surveyInfo) {
                                boolean newVote = handleSurveyResponse(surveyInfo);
                                getUserVote(surveyInfo, newVote);
                            }
                        }
                    }
                });
    }

    private void getUserVote(final GetSurveyResponse.SurveyInfo survey, final boolean newVote) {
        if (!newVote) {
            if (null != _voteReadyListener) {
                String userChoiceId = _voteLocal.getUserChoiceId();
                _voteReadyListener.onVoteReady(survey, userChoiceId, newVote);
            }
            return;
        }

        ServerApi.getVote(
                AppPreferences.getUserId(_context),
                AppPreferences.getUserSecret(_context),
                survey.id,
                new ServerApi.VoteListener() {
                    @Override
                    public void onVote(UserVoteResponse.VoteInfo voteInfo) {
                        if (null != voteInfo) {
                            _voteLocal.setUserChoice(voteInfo.itemId);

                            if (null != _voteReadyListener)
                                _voteReadyListener.onVoteReady(survey, voteInfo.itemId, newVote);
                        }
                    }
                });
    }

    private boolean handleSurveyResponse(GetSurveyResponse.SurveyInfo survey) {
        GetSurveyResponse.SurveyInfo currSurvey = _voteLocal.getVote();
        boolean isNewVote = true;

        if (currSurvey == null || !survey.id.contentEquals(currSurvey.id)) {
            // new
            _voteLocal.setVote(survey);
        }
        else {
            // same
            isNewVote = false;
        }

        // Get images result
        if (null != survey.items) {
            for (GetSurveyResponse.SurveyInfo.SurveyItem p : survey.items) {
                // Check if image missing
                if (isNewVote || !_voteLocal.imageExists(p.itemName))
                    // Download
                    downloadAndStoreImage(p.itemImageUri, p.itemName);
            }
        }
        else {
            Logger.instance().Log(Logger.ERROR, TAG, "vote itemName null");
        }

        if (isNewVote)
            _voteLocal.setIsNewVote(isNewVote);

        return isNewVote;
    }

    //endregion

}