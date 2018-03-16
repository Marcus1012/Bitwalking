package bitwalking.bitwalking.settings;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.BoringLayout;

import com.google.gson.Gson;

import java.text.ParseException;
import java.util.Date;

import bitwalking.bitwalking.user_info.TelephoneInfo;
import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 12/10/15.
 */
public class ProfileDetails implements Parcelable{
    public Bitmap image;
    public String fullName;
    public Date birthday;
    public String country;
    public TelephoneInfo phone;
    public String email;
    public boolean canEdit;

    public ProfileDetails(Bitmap image, String fullName, Date birthday, String country, TelephoneInfo phone, String email, boolean canEdit) {
        this.image = image;
        this.fullName = fullName;
        this.birthday = birthday;
        this.country = country;
        this.phone = phone;
        this.email = email;
        this.canEdit = canEdit;
    }

    public ProfileDetails(Parcel p) {
        this.fullName = p.readString();
        try {
            this.birthday = Globals.getDateOfBirthDisplayFormat().parse(p.readString());
        }
        catch (ParseException e) {}

        this.country = p.readString();
        this.phone = new Gson().fromJson(p.readString(), TelephoneInfo.class);
        this.email = p.readString();
        this.canEdit = Boolean.parseBoolean(p.readString());
        this.image = Bitmap.CREATOR.createFromParcel(p);
    }

    public static final Creator<ProfileDetails> CREATOR = new Creator<ProfileDetails>() {
        @Override
        public ProfileDetails createFromParcel(Parcel in) {
            return new ProfileDetails(in);
        }

        @Override
        public ProfileDetails[] newArray(int size) {
            return new ProfileDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fullName);
        dest.writeString(Globals.getDateOfBirthDisplayFormat().format(this.birthday));
        dest.writeString(this.country);
        dest.writeString(new Gson().toJson(this.phone));
        dest.writeString(this.email);
        dest.writeString(String.valueOf(this.canEdit));
        this.image.writeToParcel(dest, flags);
    }
}
