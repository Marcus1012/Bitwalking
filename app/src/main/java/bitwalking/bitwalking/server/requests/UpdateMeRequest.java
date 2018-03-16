package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/21/16.
 */
public class UpdateMeRequest extends JsonRequest {
    private String country;
    private String fullName;
    private String userName;
    private String dateOfBirth;

    public UpdateMeRequest(String country, String fullName, String userName, String dateOfBirth) {
        this.country = country;
        this.fullName = fullName;
        this.userName = userName;
        this.dateOfBirth = dateOfBirth;
    }
}
