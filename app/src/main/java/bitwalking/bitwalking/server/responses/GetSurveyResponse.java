package bitwalking.bitwalking.server.responses;

import java.util.ArrayList;

/**
 * Created by Marcus on 6/21/16.
 */
public class GetSurveyResponse extends BasicServerResponse {

    public GetSurveyResponse(BasicServerResponse base) {
        super(base);
    }

    public SurveyInfo getSurvey() {
        return getPayload(SurveyInfo.class);
    }

    public class SurveyInfo {
        public String id;
        public ArrayList<SurveyItem> items;

        public class SurveyItem {
            public String itemId;
            public String itemName;
            public String itemImageUri;

            public SurveyItem(String itemId, String itemName, String itemImageUri) {
                this.itemId = itemId;
                this.itemName = itemName;
                this.itemImageUri = itemImageUri;
            }
        }
    }
}