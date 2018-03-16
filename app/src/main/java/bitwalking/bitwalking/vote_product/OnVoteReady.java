package bitwalking.bitwalking.vote_product;

import bitwalking.bitwalking.server.responses.GetSurveyResponse;

/**
 * Created by Marcus on 4/13/16.
 */
public interface OnVoteReady {
    void onVoteReady(GetSurveyResponse.SurveyInfo survey, String userChoice, boolean newVote);
    void onVoteLoadError();
}
