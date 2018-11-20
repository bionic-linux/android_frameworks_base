package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public class PhoneAccountSuggestion implements Parcelable {
    public static final int REASON_NONE = 0;
    public static final int REASON_INTRA_CARRIER = 1;
    public static final int REASON_FREQUENT = 2;
    public static final int REASON_USER_SET = 3;
    public static final int REASON_OTHER = 4;

    private PhoneAccountHandle mHandle;
    private int mReason;
    private boolean mShouldAutoSelect;

    public PhoneAccountSuggestion(PhoneAccountHandle handle, int reason,
            boolean shouldAutoSelect) {
        this.mHandle = handle;
        this.mReason = reason;
        this.mShouldAutoSelect = shouldAutoSelect;
    }

    protected PhoneAccountSuggestion(Parcel in) {
        mHandle = in.readParcelable(PhoneAccountHandle.class.getClassLoader());
        mReason = in.readInt();
        mShouldAutoSelect = in.readByte() != 0;
    }

    public static final Creator<PhoneAccountSuggestion> CREATOR =
            new Creator<PhoneAccountSuggestion>() {
                @Override
                public PhoneAccountSuggestion createFromParcel(Parcel in) {
                    return new PhoneAccountSuggestion(in);
                }

                @Override
                public PhoneAccountSuggestion[] newArray(int size) {
                    return new PhoneAccountSuggestion[size];
                }
            };

    public PhoneAccountHandle getHandle() {
        return mHandle;
    }

    public int getReason() {
        return mReason;
    }

    public boolean shouldAutoSelect() {
        return mShouldAutoSelect;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mHandle, flags);
        dest.writeInt(mReason);
        dest.writeByte((byte) (mShouldAutoSelect ? 1 : 0));
    }
}
