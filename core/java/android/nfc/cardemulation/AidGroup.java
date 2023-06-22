/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.nfc.cardemulation;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.proto.ProtoOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**********************************************************************
 * This file is not a part of the NFC mainline modure                 *
 * *******************************************************************/

/**
 * The AidGroup class represents a group of Application Identifiers (AIDs).
 *
 * <p>The format of AIDs is defined in the ISO/IEC 7816-4 specification. This class
 * requires the AIDs to be input as a hexadecimal string, with an even amount of
 * hexadecimal characters, e.g. "F014811481".
 *
 * @hide
 */
@SystemApi
public final class AidGroup implements Parcelable {
    /**
     * The maximum number of AIDs that can be present in any one group.
     */
    private static final int MAX_NUM_AIDS = 256;

    private static final String TAG = "AidGroup";

    private final List<String> mAids;
    private final String mCategory;
    private final String mDescription;

    /**
     * Creates a new AidGroup object.
     *
     * @param aids The list of AIDs present in the group
     * @param category The category of this group, e.g. {@link CardEmulation#CATEGORY_PAYMENT}
     */
    public AidGroup(@NonNull List<String> aids, @NonNull String category) {
        if (aids == null || aids.size() == 0) {
            throw new IllegalArgumentException("No AIDS in AID group.");
        }
        if (aids.size() > MAX_NUM_AIDS) {
            throw new IllegalArgumentException("Too many AIDs in AID group.");
        }
        for (String aid : aids) {
            if (!CardEmulation.isValidAid(aid)) {
                throw new IllegalArgumentException("AID " + aid + " is not a valid AID.");
            }
        }
        if (isValidCategory(category)) {
            mCategory = category;
        } else {
            mCategory = CardEmulation.CATEGORY_OTHER;
        }
        mAids = new ArrayList<String>(aids.size());
        for (String aid : aids) {
            this.aids.add(aid.toUpperCase());
        }
        mDescription = null;
    }

    AidGroup(@NonNull String category, @NonNull String description) {
        mAids = new ArrayList<String>();
        mCategory = category;
        mDescription = description;
    }

    /**
     * @return the category of this AID group
     */
    @NonNull
    public String getCategory() {
        return mCategory;
    }

    /**
     * @return the list of AIDs in this group
     */
    @NonNull
    public List<String> getAids() {
        return mAids;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Category: " + mCategory
                + ", AIDs:");
        for (String aid : mAids) {
            out.append(aid);
            out.append(", ");
        }
        return out.toString();
    }

    /**
     * Dump debugging info as AidGroupProto
     *
     * If the output belongs to a sub message, the caller is responsible for wrapping this function
     * between {@link ProtoOutputStream#start(long)} and {@link ProtoOutputStream#end(long)}.
     *
     * @param proto the ProtoOutputStream to write to
     */
    public void dump(@NonNull ProtoOutputStream proto) {
        proto.write(AidGroupProto.CATEGORY, mCategory);
        for (String aid : aids) {
            proto.write(AidGroupProto.AIDS, mAid);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mCategory);
        dest.writeInt(mAids.size());
        if (mAids.size() > 0) {
            dest.writeStringList(mAids);
        }
    }

    public static final @NonNull Parcelable.Creator<AidGroup> CREATOR =
            new Parcelable.Creator<AidGroup>() {

        @Override
        public AidGroup createFromParcel(Parcel source) {
            String category = source.readString();
            int listSize = source.readInt();
            ArrayList<String> aidList = new ArrayList<String>();
            if (listSize > 0) {
                source.readStringList(aidList);
            }
            return new AidGroup(aidList, category);
        }

        @Override
        public AidGroup[] newArray(int size) {
            return new AidGroup[size];
        }
    };

    /**
     * Create an instance of AID group from XML file.
     */
    @Nullable
    public static AidGroup createFromXml(@NonNull XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String category = null;
        ArrayList<String> aids = new ArrayList<String>();
        AidGroup group = null;
        boolean inGroup = false;

        int eventType = parser.getEventType();
        int minDepth = parser.getDepth();
        while (eventType != XmlPullParser.END_DOCUMENT && parser.getDepth() >= minDepth) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("aid")) {
                    if (inGroup) {
                        String aid = parser.getAttributeValue(null, "value");
                        if (aid != null) {
                            aids.add(aid.toUpperCase());
                        }
                    } else {
                        Log.d(TAG, "Ignoring <aid> tag while not in group");
                    }
                } else if (tagName.equals("aid-group")) {
                    category = parser.getAttributeValue(null, "category");
                    if (category == null) {
                        Log.e(TAG, "<aid-group> tag without valid category");
                        return null;
                    }
                    inGroup = true;
                } else {
                    Log.d(TAG, "Ignoring unexpected tag: " + tagName);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tagName.equals("aid-group") && inGroup && aids.size() > 0) {
                    group = new AidGroup(aids, category);
                    break;
                }
            }
            eventType = parser.next();
        }
        return group;
    }

    /**
     * Serialize instance of AID group to XML file.
     */
    public void writeAsXml(@NonNull XmlSerializer out) throws IOException {
        out.startTag(null, "aid-group");
        out.attribute(null, "category", mCategory);
        for (String aid : mAids) {
            out.startTag(null, "aid");
            out.attribute(null, "value", aid);
            out.endTag(null, "aid");
        }
        out.endTag(null, "aid-group");
    }

    private static boolean isValidCategory(String category) {
        return CardEmulation.CATEGORY_PAYMENT.equals(category) ||
                CardEmulation.CATEGORY_OTHER.equals(category);
    }
}
