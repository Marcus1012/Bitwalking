package bitwalking.bitwalking.mvi.registration;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alexey on 29.08.17.
 */

public class StateRegistration implements Parcelable {
    private final int step;
    private final String first,last,email,pass;

    StateRegistration(int step,
                      String first,
                      String last,
                      String email,
                      String pass){

        this.step = step;
        this.first = first;
        this.last = last;
        this.email = email;
        this.pass = pass;

    }

    public int getStep() {
        return step;
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public String getEmail() {
        return email;
    }

    public String getPass() {
        return pass;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.step);
        dest.writeString(this.first);
        dest.writeString(this.last);
        dest.writeString(this.email);
        dest.writeString(this.pass);
    }

    protected StateRegistration(Parcel in) {
        this.step = in.readInt();
        this.first = in.readString();
        this.last = in.readString();
        this.email = in.readString();
        this.pass = in.readString();
    }

    public static final Parcelable.Creator<StateRegistration> CREATOR = new Parcelable.Creator<StateRegistration>() {
        @Override
        public StateRegistration createFromParcel(Parcel source) {
            return new StateRegistration(source);
        }

        @Override
        public StateRegistration[] newArray(int size) {
            return new StateRegistration[size];
        }
    };
}
