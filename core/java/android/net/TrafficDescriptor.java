package android.net;

import java.util.ArrayList;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Rlog;

public class TrafficDescriptor implements Parcelable{
    private static final String TAG = "TrafficDescriptor";
    /**
     * the system OS id
     */
    private String osId;

    /**
     * the application id
     */
    private String appId;

    /**
     * Non-IP descriptor, for non-IP traffic
     */
    private String nonIPDescriptor;

    /**
     * IP 3 tuples, destination IP address or IPv6 network prefix,
     * destination port number, protocol ID of the protocol above IP
     */
    private IPDescriptor ipDescriptor;

    /**
     * destination FQDN(s)
     */
    private String domainDescriptor;

    /**
     * data network name
     */
    private ArrayList<String> dnnList;

    /**
     * network capability
     */
    private ArrayList<Byte> capability;

    //indicate the capability bits which modem need
    private enum NetCapability {
        RESERVED (0x0),
        IMS(0x01),
        MMS(0x02),
        SUPL(0x04),
        INTERNET(0x08);

        private int value;
        NetCapability(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    public TrafficDescriptor() {
        clearAll();
    }

    public TrafficDescriptor(String osId, String appId, String nonIPDescriptor,
            IPDescriptor ipDescriptor, String domainDescriptor, ArrayList<String> dnnList,
            ArrayList<Byte> capability) {
        this.osId = osId;
        this.appId = appId;
        this.nonIPDescriptor = nonIPDescriptor;
        this.ipDescriptor = ipDescriptor;
        this.domainDescriptor = domainDescriptor;
        this.dnnList = dnnList;
        this.capability = capability;
    }

    public TrafficDescriptor(TrafficDescriptor td) {
        this.osId = td.getOSId();
        this.appId = td.getAppId();
        this.nonIPDescriptor = td.getNonIPDescriptor();
        this.ipDescriptor = td.getIpDescriptor();
        this.domainDescriptor = td.getDomainDescriptor();
        this.dnnList = td.getDnn();
        this.capability = td.getCapability();
    }

    private void clearAll() {
        this.osId = null;
        this.appId = null;
        this.nonIPDescriptor = null;
        this.ipDescriptor = new IPDescriptor();
        this.domainDescriptor = null;
        this.dnnList = new ArrayList<String>();
        this.capability = new ArrayList<Byte>();
    }

    public String getOSId() {
        return osId;
    }

    public void setOSId(String osId) {
        this.osId = osId;
    }

    public String getAppId() {
        return osId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getNonIPDescriptor() {
        return nonIPDescriptor;
    }

    public void setNonIPDescriptor(String nonIPDescriptor) {
        this.nonIPDescriptor = nonIPDescriptor;
    }

    public IPDescriptor getIpDescriptor() {
        return ipDescriptor;
    }

    public void setIpDescriptor(IPDescriptor ipDescriptor) {
        this.ipDescriptor = ipDescriptor;
    }

    public String getDomainDescriptor() {
        return domainDescriptor;
    }

    public void setDomainDescriptor(String domainDescriptor) {
        this.domainDescriptor = domainDescriptor;
    }

    public ArrayList<String> getDnn() {
        return dnnList;
    }

    public void addDnn(String dnn) {
        dnnList.add(dnn);
    }

    public int describeContents() {
        return 0;
    }

    public ArrayList<Byte> getCapability() {
        return capability;
    }

    public void updateCapability(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_IMS)) {
            capability.add(Integer.valueOf(NetCapability.IMS.ordinal()).byteValue());
        }
        if (networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_MMS)) {
            capability.add(Integer.valueOf(NetCapability.MMS.ordinal()).byteValue());
        }
        if (networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_SUPL)) {
            capability.add(Integer.valueOf(NetCapability.SUPL.ordinal()).byteValue());
        }
        if (networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            capability.add(Integer.valueOf(NetCapability.INTERNET.ordinal()).byteValue());
        }
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(osId);
        dest.writeString(appId);
        dest.writeString(nonIPDescriptor);
        dest.writeParcelable((Parcelable) ipDescriptor, flags);
        dest.writeString(domainDescriptor);
        dest.writeList(dnnList);
        dest.writeList(capability);
    }
    public TrafficDescriptor(Parcel source) {
        this.osId = source.readString();
        this.appId = source.readString();
        this.nonIPDescriptor = source.readString();
        this.ipDescriptor = source.readParcelable(null);
        this.domainDescriptor = source.readString();
        this.dnnList = (ArrayList<String>)source.readArrayList(null);
        this.capability = (ArrayList<Byte>)source.readArrayList(null);
    }
    public static final Parcelable.Creator<TrafficDescriptor> CREATOR =
            new Parcelable.Creator<TrafficDescriptor>() {
        @Override
        public TrafficDescriptor createFromParcel(Parcel source) {
            return new TrafficDescriptor(source);
        }

        @Override
        public TrafficDescriptor[] newArray(int size) {
            return new TrafficDescriptor[size];
        }
    };
    private void log(String str) {
        Rlog.d(TAG, str);
    }
    public String toString() {
        return "TrafficDescriptor [ osId=" + osId + " appId=" + appId +
                " domainDescriptor=" + domainDescriptor +
                " dnnList= " + dnnList + " capability=" + capability +
                ", [" + ipDescriptor.toString() + "] ]";
    }
}

