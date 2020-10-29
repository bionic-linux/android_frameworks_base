/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.provider;

import static android.provider.SimPhonebookContract.EntityFiles.EF_ADN;
import static android.provider.SimPhonebookContract.EntityFiles.EF_FDN;
import static android.provider.SimPhonebookContract.EntityFiles.EF_SDN;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SubscriptionInfo;
import android.util.AndroidRuntimeException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * The contract between the provider of contacts on the device's SIM cards and applications.
 * Contains definitions of the supported URIs and columns.
 *
 * <p>This content provider does not support any of the QUERY_ARG_SQL* bundle arguments. An
 * IllegalArgumentException will be thrown if these are included.
 */
public final class SimPhonebookContract {

    private SimPhonebookContract() {}

    /** The authority for the icc provider. */
    public static final String AUTHORITY = "com.android.simphonebook";

    /** The content:// style uri to the authority for the icc provider. */
    @NonNull
    public static final Uri AUTHORITY_URI = Uri.parse("content://com.android.simphonebook");

    /** Constants for the contacts on a SIM card. */
    public static final class SimContacts {

        private SimContacts() {}

        /**
         * The subscription ID of the SIM the record is from.
         *
         * @see SubscriptionInfo#getSubscriptionId()
         */
        public static final String SUBSCRIPTION_ID = "subscription_id";

        /**
         * The type of the entity file the contact record is from.
         *
         * @see EntityFiles#EF_ADN
         * @see EntityFiles#EF_FDN
         * @see EntityFiles#EF_SDN
         */
        public static final String ENTITY_FILE_TYPE = "entity_file_type";

        /**
         * The offset of the record in the entity file that contains it.
         *
         * <p>This can be used to access individual SIM contact records by appending it to the
         * entity
         * file URIs but it is not like a normal database ID because it is not auto-incrementing and
         * it
         * is not unique across SIM cards or entity files. Hence, care should taken when using it
         * to
         * ensure that is applied to the correct SIM and EF.
         *
         * <p>It is recommended to use {@link ContentResolver#applyBatch(String, ArrayList)} with
         * an
         * assertion on the existing values of name and phone number of the record being modified
         * when performing updates and deletions to validate that the intended data is being
         * modified.
         */
        public static final String RECORD_EF_INDEX = "record_ef_index";

        /** The name of the contact. */
        public static final String NAME = "name";

        /** The phone number of the contact. */
        public static final String PHONE_NUMBER = "number";

        /** The MIME type of a CONTENT_URI subdirectory of a single SIM contact. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/sim-contact";

        /** The MIME type of CONTENT_URI providing a directory of SIM contacts. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/sim-contact";

        /** Returns the content Uri for the specified entity file on the specified SIM. */
        @NonNull
        public static Uri getContentUri(int subscriptionId, @EntityFiles.EfType int efType) {
            return buildContentUri(subscriptionId, efType).build();
        }

        /**
         * Content Uri for contacts stored in the ADN file of the specified SIM card.
         *
         * @see EntityFiles#EF_ADN
         */
        @NonNull
        public static Uri getAdnUri(int subscriptionId) {
            return getContentUri(subscriptionId, EF_ADN);
        }

        /**
         * Content Uri for contacts stored in the FDN file of the specified SIM card.
         *
         * @see EntityFiles#EF_FDN
         */
        @NonNull
        public static Uri getFdnUri(int subscriptionId) {
            return getContentUri(subscriptionId, EF_FDN);
        }

        /**
         * Content Uri for contacts stored in the SDN file of the specified SIM card.
         *
         * @see EntityFiles#EF_SDN
         */
        @NonNull
        public static Uri getSdnUri(int subscriptionId) {
            return getContentUri(subscriptionId, EF_SDN);
        }

        /** Content Uri for the specific SIM contact with the provided {@link #RECORD_EF_INDEX}. */
        @NonNull
        public static Uri getItemUri(int subscriptionId, int efType, int recordEfIndex) {
            return buildContentUri(subscriptionId, efType)
                    .appendPath(String.valueOf(recordEfIndex))
                    .build();
        }

        private static Uri.Builder buildContentUri(
                int subscriptionId, @EntityFiles.EfType int efType) {
            return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath("subid")
                    .appendPath(String.valueOf(subscriptionId))
                    .appendPath(getEfUriPath(efType));
        }

        private static String getEfUriPath(@EntityFiles.EfType int efType) {
            switch (efType) {
                case EF_ADN:
                    return "adn";
                case EF_FDN:
                    return "fdn";
                case EF_SDN:
                    return "sdn";
                default:
                    throw new IllegalArgumentException("Unsupported EfType " + efType);
            }
        }
    }

    /**
     * Constants for metadata about the contact operations supported by the entify files of the SIM
     * cards in the phone.
     */
    public final static class EntityFiles {

        private EntityFiles() {}

        /** {@link SubscriptionInfo#getSimSlotIndex()} of the SIM for this row. */
        public static final String SLOT_INDEX = "slot_index";

        /** {@link SubscriptionInfo#getSubscriptionId()} of the SIM for this row. */
        public static final String SUBSCRIPTION_ID = "subscription_id";

        /**
         * The entity file type for this row.
         *
         * @see EntityFiles#EF_ADN
         * @see EntityFiles#EF_FDN
         * @see EntityFiles#EF_SDN
         */
        public static final String EF_TYPE = "ef_type";

        /** The maximum number of contacts supported by the entity file. */
        public static final String MAX_CONTACTS = "max_contacts";

        /** The maximum length supported for the name of a contact in the entity file. */
        public static final String NAME_MAX_LENGTH = "name_max_length";

        /** The maximum length supported for the phone number of a contact in the entity file. */
        public static final String PHONE_NUMBER_MAX_LENGTH = "phone_number_max_length";

        /**
         * Annotation for the valid entity file types.
         *
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                prefix = {"EF"},
                value = {EF_UNKNOWN, EF_ADN, EF_FDN, EF_SDN})
        public @interface EfType {
        }

        /**
         *
         */
        public static final int EF_UNKNOWN = 1;

        /**
         * Type for accessing contacts in the "abbreviated dialing number" (ADN) entity file on the
         * SIM.
         *
         * <p>ADN contacts are typically user created contacts.
         */
        public static final int EF_ADN = 1;

        /**
         * Type for accessing contacts in the "fixed dialing number" (FDN) entity file on the SIM.
         *
         * <p>FDN numbers are the numbers that are allowed to dialed for outbound calls when FDN is
         * enabled.
         *
         * <p>FDN contacts cannot be modified by applications. Hence, insert, update and
         * delete methods operating on this Uri will throw UnsupportedOperationException
         */
        public static final int EF_FDN = 2;

        /**
         * Type for accessing contacts in the "service dialing number" (SDN) entity file on the
         * SIM.
         *
         * <p>Typically SDNs are preset numbers provided by the carrier for common operations (e.g.
         * voicemail, check balance, etc).
         *
         * <p>SDN contacts cannot be modified by applications. Hence, insert, update and delete
         * methods
         * operating on this Uri will throw UnsupportedOperationException
         */
        public static final int EF_SDN = 3;

        /** The MIME type of CONTENT_URI providing a directory of ADN-like entity files. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/sim-entity-file";

        /** The MIME type of a CONTENT_URI subdirectory of a single ADN-like entity file. */
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/sim-entity-file";

        /** Content URI for the ADN-like entity files available on the device. */
        @NonNull
        public static final Uri CONTENT_URI = AUTHORITY_URI
                .buildUpon()
                .appendPath("entity_files").build();
    }

    /**
     * Custom exception verifying that this can be thrown across a content provider API boundary
     */
    public static final class SimPhonebookException extends AndroidRuntimeException implements
            Parcelable {
        /**
         * Annotation for the errors that may occur when performing SIM phonebook operations.
         *
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                prefix = {"ERROR"},
                value = {
                        ERROR_UNKNOWN,
                        ERROR_NONE,
                        ERROR_MISSING_SIM,
                        ERROR_UNSUPPORTED_EF,
                        ERROR_EF_FULL,
                        ERROR_NAME_TOO_LONG,
                        ERROR_PHONE_NUMBER_TOO_LONG
                })
        public @interface ErrorCode {
        }

        /** An unknown error occurred during the operation. */
        public static final int ERROR_UNKNOWN = 0;

        /** There was no error in an operation. */
        public static final int ERROR_NONE = 1;

        /**
         * The SIM card that this phonebook is on is not present (e.g. because the user removed it
         * or it
         * is unavailable).
         */
        public static final int ERROR_MISSING_SIM = 2;

        /**
         * The entity file type used for an operation isn't supported by the SIM card for this
         * phonebook.
         */
        public static final int ERROR_UNSUPPORTED_EF = 3;

        /**
         * An insert or update operation could not be performed because the target EF on the SIM
         * phonebook
         * already has the maximum number of contacts it can support.
         */
        public static final int ERROR_EF_FULL = 4;

        /**
         * An insert or update operation could not be performed because the value provided for
         * {@link
         * SimContacts#NAME} was longer than the maximum length supported by the target EF.
         */
        public static final int ERROR_NAME_TOO_LONG = 5;

        /**
         * An insert or update operation could not be performed because the value provided for
         * {@link
         * SimContacts#PHONE_NUMBER} was longer than the maximum length supported by the target EF.
         */
        public static final int ERROR_PHONE_NUMBER_TOO_LONG = 6;

        private final int errorCode;

        public SimPhonebookException(int errorCode) {
            this(errorCode, null);
        }

        public SimPhonebookException(int errorCode, @Nullable String message) {
            this(errorCode, message, null);
        }

        public SimPhonebookException(int errorCode, @Nullable String message,
                @Nullable Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        private SimPhonebookException(@NonNull Parcel in) {
            this(in.readInt());
        }


        /** Returns an error code indicating the reason for the failure. */
        @ErrorCode
        public int getErrorCode() {
            return errorCode;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeLong(errorCode);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @NonNull
        public static String getErrorName(@ErrorCode int errorCode) {
            switch (errorCode) {
                case ERROR_UNKNOWN:
                    return "ERROR_UNKNOWN";
                case ERROR_NONE:
                    return "ERROR_NONE";
                case ERROR_MISSING_SIM:
                    return "ERROR_MISSING_SIM";
                case ERROR_UNSUPPORTED_EF:
                    return "ERROR_UNSUPPORTED_EF";
                case ERROR_EF_FULL:
                    return "ERROR_EF_FULL";
                case ERROR_NAME_TOO_LONG:
                    return "ERROR_NAME_TOO_LONG";
                case ERROR_PHONE_NUMBER_TOO_LONG:
                    return "ERROR_PHONE_NUMBER_TOO_LONG";
                default:
                    return "UNRECOGNIZED";
            }
        }

        @NonNull
        public static final Creator<SimPhonebookException> CREATOR =
                new Creator<SimPhonebookException>() {
                    @Override
                    public SimPhonebookException createFromParcel(@NonNull Parcel in) {
                        return new SimPhonebookException(in);
                    }

                    @NonNull
                    @Override
                    public SimPhonebookException[] newArray(int size) {
                        return new SimPhonebookException[size];
                    }
                };
    }
}
