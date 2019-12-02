package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class IPDescriptor implements Parcelable{
    private String ipv4Addr;
    private String ipv4AddrMask;
    private String ipv6Addr;
    private byte ipv6AddrPrefixLen;
    private byte protocol;
    private int startPort;
    private int endPort;
    private String spi;
    private String flowLabel;
    private byte tosField;
    private byte tosMask;

    public enum PROTOCOL {
        NONE,
        IP,
        IPv6,
        IPv4v6
    };
    public IPDescriptor() {
        clearAll();
    }

    private void clearAll() {
        this.ipv4Addr = null;
        this.ipv4AddrMask = null;
        this.ipv6Addr = null;
        this.ipv6AddrPrefixLen = 0;
        this.protocol = Integer.valueOf(PROTOCOL.NONE.ordinal()).byteValue();
        this.startPort = 0;
        this.endPort = 0;
        this.spi = null;
        this.flowLabel = null;
        this.tosField = 0;
        this.tosMask = 0;
    }

    public IPDescriptor(String ipv4Addr, String ipv4AddrMask, String ipv6Addr,
            byte ipv6AddrPrefixLen, String protocol, int startPort, int endPort,
            String spi, String flowLabel, byte tosField, byte tosMask) {
        this.ipv4Addr = ipv4Addr;
        this.ipv4AddrMask = ipv4AddrMask;
        this.ipv6Addr = ipv6Addr;
        this.ipv6AddrPrefixLen = ipv6AddrPrefixLen;
        this.protocol = convertToIntProtocol(protocol);
        this.startPort = startPort;
        this.endPort = endPort;
        this.spi = spi;
        this.flowLabel = flowLabel;
        this.tosField = tosField;
        this.tosMask = tosMask;
    }


    public String getIpv4Addr() {
        return ipv4Addr;
    }

    public void setIpv4Addr(String ipv4Addr) {
        this.ipv4Addr = ipv4Addr;
    }


    public String getIpv4AddrMask() {
        return ipv4AddrMask;
    }

    public void setIpv4AddrMask(String ipv4AddrMask) {
        this.ipv4AddrMask = ipv4AddrMask;
    }

    public String getIpv6Addr() {
        return ipv6Addr;
    }

    public void setIpv6Addr(String ipv6Addr) {
        this.ipv6Addr = ipv6Addr;
    }

    public byte getIpv6AddrPrefixLen() {
        return ipv6AddrPrefixLen;
    }

    public void setIpv6AddrPrefixLen(byte ipv6AddrPrefixLen) {
        this.ipv6AddrPrefixLen = ipv6AddrPrefixLen;
    }

    public byte getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = convertToIntProtocol(protocol);
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getSpi() {
        return spi;
    }

    public void setSpi(String spi) {
        this.spi = spi;
    }

    public String getFlowLabel() {
        return flowLabel;
    }

    public void setFlowLabel(String flowLabel) {
        this.flowLabel = flowLabel;
    }

    public byte getTosField() {
        return tosField;
    }

    public void setTosField(byte tosField) {
        this.tosField = tosField;
    }

    public byte getTosMask() {
        return tosMask;
    }

    public void setTosMask(byte tosMask) {
        this.tosMask = tosMask;
    }

    public int describeContents() {
        return 0;
    }

    private byte convertToIntProtocol(String str) {
        PROTOCOL value = PROTOCOL.NONE;
        if (str.equals("IPv4")) {
            value = PROTOCOL.IP;
        } else if (str.equals("IPv6")){
            value = PROTOCOL.IPv6;
        } else if (str.equals("IPv4v6")){
            value = PROTOCOL.IPv4v6;
        }
        return Integer.valueOf(value.ordinal()).byteValue();
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ipv4Addr);
        dest.writeString(ipv4AddrMask);
        dest.writeString(ipv6Addr);
        dest.writeByte(ipv6AddrPrefixLen);
        dest.writeByte(protocol);
        dest.writeInt(startPort);
        dest.writeInt(endPort);
        dest.writeString(spi);
        dest.writeString(flowLabel);
        dest.writeByte(tosField);
        dest.writeByte(tosMask);
    }
    public IPDescriptor(Parcel source) {
        this.ipv4Addr = source.readString();
        this.ipv4AddrMask = source.readString();
        this.ipv6Addr = source.readString();
        this.ipv6AddrPrefixLen = source.readByte();
        this.protocol = source.readByte();
        this.startPort = source.readInt();
        this.endPort = source.readInt();
        this.spi = source.readString();
        this.flowLabel = source.readString();
        this.tosField = source.readByte();
        this.tosMask = source.readByte();
    }
    public static final Parcelable.Creator<IPDescriptor> CREATOR =
            new Parcelable.Creator<IPDescriptor>() {
        @Override
        public IPDescriptor createFromParcel(Parcel source) {
            return new IPDescriptor(source);
        }

        @Override
        public IPDescriptor[] newArray(int size) {
            return new IPDescriptor[size];
        }
    };
    @Override
    public String toString() {
        return "IPDescriptor [ipv4Addr=" + ipv4Addr + ", ipv4AddrMask="
                + ipv4AddrMask + ", ipv6Addr=" + ipv6Addr
                + ", ipv6AddrPrefixLen=" + ipv6AddrPrefixLen + ", protocol="
                + protocol + ", startPort=" + startPort + ", endPort="
                + endPort + ", spi=" + spi + ", flowLabel=" + flowLabel
                + ", tosField=" + tosField + ", tosMask=" + tosMask + "]";
    }

}

