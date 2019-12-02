package android.telephony.data;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteSelectionDescriptor implements Parcelable{
    private static final String TAG = "RouteSelectionDescriptor";

    private int urspCount = 0;
    private int rsdCount = 0;
    private int id = 0;
    private String dnn = null;
    private String snssai = null;
    private int sscMode = 0;
    public String getDnn() {
        return dnn;
    }
    public void setDnn(String dnn) {
        this.dnn = dnn;
    }
    public String getSnssai() {
        return snssai;
    }
    public void setSnssai(String snssai) {
        this.snssai = snssai;
    }
    public int getSscMode() {
        return sscMode;
    }
    public void setSscMode(int sscMode) {
        this.sscMode = sscMode;
    }
    public int getUrspCount() {
        return urspCount;
    }
    public void setUrspCount(int urspCount) {
        this.urspCount = urspCount;
    }
    public int getRsdCount() {
        return rsdCount;
    }
    public void setRsdCount(int rsdCount) {
        this.rsdCount = rsdCount;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(urspCount);
        dest.writeInt(rsdCount);
        dest.writeInt(id);
        dest.writeString(dnn);
        dest.writeString(snssai);
        dest.writeInt(sscMode);
    }
    public RouteSelectionDescriptor() {
    }

    public RouteSelectionDescriptor(Parcel source) {
        this.urspCount = source.readInt();
        this.rsdCount = source.readInt();
        this.id = source.readInt();
        this.dnn = source.readString();
        this.snssai = source.readString();
        this.sscMode = source.readInt();
    }
    public static final Parcelable.Creator<RouteSelectionDescriptor> CREATOR =
            new Parcelable.Creator<RouteSelectionDescriptor>() {
        @Override
        public RouteSelectionDescriptor createFromParcel(Parcel source) {
            return new RouteSelectionDescriptor(source);
        }

        @Override
        public RouteSelectionDescriptor[] newArray(int size) {
            return new RouteSelectionDescriptor[size];
        }
    };
}
