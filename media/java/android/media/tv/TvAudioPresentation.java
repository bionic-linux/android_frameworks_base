/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.media.tv;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.icu.util.ULocale;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * The TvAudioPresentation class encapsulates the information that describes an audio presentation
 * which is available in next generation audio content.
 *
 * Used by {@link TvInputManager} {@link TvInputManager#getAudioPresentations()} and
 * {@link TvInputManager#selectAudioPresentation(int presentationId, int programId)} to query
 * available presentations and to select an audio presentation, respectively.
 *
 * A list of available audio presentations in a media source can be queried using
 * {@link TvInputManager#getAudioPresentations()}. This list can be presented to a user for
 * selection.
 * A TvAudioPresentation information can be passed to an offloaded audio decoder via
 * {@link TvInputManager#selectAudioPresentation(int presentationId, int programId)} to request
 * decoding of the selected presentation. An audio stream may contain multiple presentations that
 * differ by language, accessibility, end point mastering and dialogue enhancement. An audio
 * presentation may also have a set of description labels in different languages to help the user
 * make an informed selection.
 */
public final class TvAudioPresentation implements Parcelable {

    private final int mPresentationId;
    private final int mProgramId;
    private final ULocale mLanguage;

    /** @hide */
    @IntDef(
        value = {
            MASTERING_NOT_INDICATED,
            MASTERED_FOR_STEREO,
            MASTERED_FOR_SURROUND,
            MASTERED_FOR_3D,
            MASTERED_FOR_HEADPHONE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MasteringIndicationType {}
    private @MasteringIndicationType int mMasteringIndication;
    private boolean mAudioDescriptionAvailable;
    private boolean mSpokenSubtitlesAvailable;
    private boolean mDialogueEnhancementAvailable;
    private Map<ULocale, CharSequence> mLabels;

    /**
     * No preferred reproduction channel layout.
     *
     * @see Builder#setMasteringIndication(int)
     */
    public static final int MASTERING_NOT_INDICATED         = 0;
    /**
     * Stereo speaker layout.
     *
     * @see Builder#setMasteringIndication(int)
     */
    public static final int MASTERED_FOR_STEREO             = 1;
    /**
     * Two-dimensional (e.g. 5.1) speaker layout.
     *
     * @see Builder#setMasteringIndication(int)
     */
    public static final int MASTERED_FOR_SURROUND           = 2;
    /**
     * Three-dimensional (e.g. 5.1.2) speaker layout.
     *
     * @see Builder#setMasteringIndication(int)
     */
    public static final int MASTERED_FOR_3D                 = 3;
    /**
     * Prerendered for headphone playback.
     *
     * @see Builder#setMasteringIndication(int)
     */
    public static final int MASTERED_FOR_HEADPHONE          = 4;

    /**
     * Unknown audio presentation ID, this indicates audio presentation ID is not selected.
     */
    public static final int PRESENTATION_ID_UNKNOWN = -1;

    /**
     * Unknown audio program ID, this indicates audio program ID is not selected.
     */
    public static final int PROGRAM_ID_UNKNOWN = -1;

    /**
     * This allows an application developer to construct an TvAudioPresentation object with all the
     * parameters.
     * The rest of the metadata is informative only so as to distinguish features
     * of different presentations.
     * @param presentationId Presentation ID to be decoded by a next generation audio decoder.
     * @param programId Program ID to be decoded by a next generation audio decoder.
     * @param language Locale corresponding to ISO 639-1/639-2 language code.
     * @param masteringIndication One of {@link TvAudioPresentation#MASTERING_NOT_INDICATED},
     *     {@link TvAudioPresentation#MASTERED_FOR_STEREO},
     *     {@link TvAudioPresentation#MASTERED_FOR_SURROUND},
     *     {@link TvAudioPresentation#MASTERED_FOR_3D},
     *     {@link TvAudioPresentation#MASTERED_FOR_HEADPHONE}.
     * @param audioDescriptionAvailable Audio description for the visually impaired.
     * @param spokenSubtitlesAvailable Spoken subtitles for the visually impaired.
     * @param dialogueEnhancementAvailable Dialogue enhancement.
     * @param labels Text label indexed by its locale corresponding to the language code.
     */
    private TvAudioPresentation(int presentationId,
                             int programId,
                             @NonNull ULocale language,
                             @MasteringIndicationType int masteringIndication,
                             boolean audioDescriptionAvailable,
                             boolean spokenSubtitlesAvailable,
                             boolean dialogueEnhancementAvailable,
                             @NonNull Map<ULocale, CharSequence> labels) {
        mPresentationId = presentationId;
        mProgramId = programId;
        mLanguage = language;
        mMasteringIndication = masteringIndication;
        mAudioDescriptionAvailable = audioDescriptionAvailable;
        mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
        mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
        mLabels = new HashMap<ULocale, CharSequence>(labels);
    }

    private TvAudioPresentation(@NonNull Parcel in) {
        mPresentationId = in.readInt();
        mProgramId = in.readInt();
        mLanguage = new ULocale(in.readString());
        mMasteringIndication = in.readInt();
        mAudioDescriptionAvailable = in.readBoolean();
        mSpokenSubtitlesAvailable = in.readBoolean();
        mDialogueEnhancementAvailable = in.readBoolean();
        Map<ULocale, CharSequence> localeLabels = new HashMap<ULocale, CharSequence>();
        in.readMap(localeLabels, CharSequence.class.getClassLoader());
        mLabels = localeLabels;
    }

    /**
     * Returns presentation ID used by the framework to select an audio presentation rendered by a
     * decoder. Presentation ID is typically sequential, but does not have to be.
     */
    public int getPresentationId() {
        return mPresentationId;
    }

    /**
     * Returns program ID used by the framework to select an audio presentation rendered by a
     * decoder. Program ID can be used to further uniquely identify the presentation to a decoder.
     */
    public int getProgramId() {
        return mProgramId;
    }

    /**
     * @return a map of available text labels for this presentation. Each label is indexed by its
     * locale corresponding to the language code as specified by ISO 639-2. Either ISO 639-2/B
     * or ISO 639-2/T could be used.
     */
    @NonNull
    public Map<Locale, String> getLabels() {
        Map<Locale, String> localeLabels = new HashMap<Locale, String>(mLabels.size());
        for (Map.Entry<ULocale, CharSequence> entry : mLabels.entrySet()) {
            localeLabels.put(entry.getKey().toLocale(), entry.getValue().toString());
        }
        return localeLabels;
    }

    /**
     * @return the ULocale corresponding to audio presentation's ISO 639-1/639-2 language code.
     */
    @NonNull
    public ULocale getLocale() {
        return mLanguage;
    }

    /**
     * @return the mastering indication of the audio presentation.
     * See {@link TvAudioPresentation#MASTERING_NOT_INDICATED},
     *     {@link TvAudioPresentation#MASTERED_FOR_STEREO},
     *     {@link TvAudioPresentation#MASTERED_FOR_SURROUND},
     *     {@link TvAudioPresentation#MASTERED_FOR_3D},
     *     {@link TvAudioPresentation#MASTERED_FOR_HEADPHONE}
     */
    @MasteringIndicationType
    public int getMasteringIndication() {
        return mMasteringIndication;
    }

    /**
     * Indicates whether an audio description for the visually impaired is available.
     * @return {@code true} if audio description is available.
     */
    public boolean hasAudioDescription() {
        return mAudioDescriptionAvailable;
    }

    /**
     * Indicates whether spoken subtitles for the visually impaired are available.
     * @return {@code true} if spoken subtitles are available.
     */
    public boolean hasSpokenSubtitles() {
        return mSpokenSubtitlesAvailable;
    }

    /**
     * Indicates whether dialogue enhancement is available.
     * @return {@code true} if dialogue enhancement is available.
     */
    public boolean hasDialogueEnhancement() {
        return mDialogueEnhancementAvailable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TvAudioPresentation)) {
            return false;
        }
        TvAudioPresentation obj = (TvAudioPresentation) o;
        return mPresentationId == obj.getPresentationId()
                && mProgramId == obj.getProgramId()
                && mLanguage.equals(obj.getLocale())
                && mMasteringIndication == obj.getMasteringIndication()
                && mAudioDescriptionAvailable == obj.hasAudioDescription()
                && mSpokenSubtitlesAvailable == obj.hasSpokenSubtitles()
                && mDialogueEnhancementAvailable == obj.hasDialogueEnhancement()
                && mLabels.equals(obj.getLabels());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPresentationId,
                mProgramId,
                mLanguage.hashCode(),
                mMasteringIndication,
                mAudioDescriptionAvailable,
                mSpokenSubtitlesAvailable,
                mDialogueEnhancementAvailable,
                mLabels.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName() + " ");
        sb.append("{ presentation id=" + mPresentationId);
        sb.append(", program id=" + mProgramId);
        sb.append(", language=" + mLanguage);
        sb.append(", labels=" + mLabels);
        sb.append(", mastering indication=" + mMasteringIndication);
        sb.append(", audio description=" + mAudioDescriptionAvailable);
        sb.append(", spoken subtitles=" + mSpokenSubtitlesAvailable);
        sb.append(", dialogue enhancement=" + mDialogueEnhancementAvailable);
        sb.append(" }");
        return sb.toString();
    }


    public static final class Builder {
        private final int mPresentationId;
        private int mProgramId = PROGRAM_ID_UNKNOWN;
        private ULocale mLanguage = new ULocale("");
        private int mMasteringIndication = MASTERING_NOT_INDICATED;
        private boolean mAudioDescriptionAvailable = false;
        private boolean mSpokenSubtitlesAvailable = false;
        private boolean mDialogueEnhancementAvailable = false;
        private Map<ULocale, CharSequence> mLabels = new HashMap<ULocale, CharSequence>();

        /**
         * Create a {@link Builder}. Any field that should be included in the
         * {@link TvAudioPresentation} must be added.
         *
         * @param presentationId the presentation ID of this audio presentation
         */
        public Builder(int presentationId) {
            mPresentationId = presentationId;
        }

        /**
         * Sets the program ID to which this audio presentation refers.
         *
         * @param programId The program ID to be decoded.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setProgramId(int programId) {
            mProgramId = programId;
            return this;
        }

        /**
         * Sets the language information of the audio presentation.
         *
         * @param language Locale corresponding to ISO 639-1/639-2 language code.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setLocale(@NonNull ULocale language) {
            mLanguage = language;
            return this;
        }

        /**
         * Sets the mastering indication.
         *
         * @param masteringIndication Input to set mastering indication.
         * @throws IllegalArgumentException if the mastering indication is not any of
         * {@link TvAudioPresentation#MASTERING_NOT_INDICATED},
         * {@link TvAudioPresentation#MASTERED_FOR_STEREO},
         * {@link TvAudioPresentation#MASTERED_FOR_SURROUND},
         * {@link TvAudioPresentation#MASTERED_FOR_3D},
         * or {@link TvAudioPresentation#MASTERED_FOR_HEADPHONE}
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setMasteringIndication(
                @MasteringIndicationType int masteringIndication) {
            if (masteringIndication != MASTERING_NOT_INDICATED
                    && masteringIndication != MASTERED_FOR_STEREO
                    && masteringIndication != MASTERED_FOR_SURROUND
                    && masteringIndication != MASTERED_FOR_3D
                    && masteringIndication != MASTERED_FOR_HEADPHONE) {
                throw new IllegalArgumentException("Unknown mastering indication: "
                                                        + masteringIndication);
            }
            mMasteringIndication = masteringIndication;
            return this;
        }

        /**
         * Sets locale / text label pairs describing the presentation.
         *
         * @param labels Text label indexed by its locale corresponding to the language code.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setLabels(@NonNull Map<ULocale, CharSequence> labels) {
            mLabels = new HashMap<ULocale, CharSequence>(labels);
            return this;
        }

        /**
         * Indicate whether the presentation contains audio description for the visually impaired.
         *
         * @param audioDescriptionAvailable Set to true if audio description for the visually
         * impaired is available.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setHasAudioDescription(boolean audioDescriptionAvailable) {
            mAudioDescriptionAvailable = audioDescriptionAvailable;
            return this;
        }

        /**
         * Indicate whether the presentation contains spoken subtitles for the visually impaired.
         *
         * @param spokenSubtitlesAvailable Set to true if spoken subtitles for the visually
         * impaired is available.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setHasSpokenSubtitles(boolean spokenSubtitlesAvailable) {
            mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
            return this;
        }

        /**
         * Indicate whether the presentation supports dialogue enhancement.
         *
         * @param dialogueEnhancementAvailable Set to true if dialogue enhancement is available.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull Builder setHasDialogueEnhancement(boolean dialogueEnhancementAvailable) {
            mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
            return this;
        }

        /**
         * Creates a {@link TvAudioPresentation} instance with the specified fields.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        public @NonNull TvAudioPresentation build() {
            return new TvAudioPresentation(mPresentationId, mProgramId,
                                           mLanguage, mMasteringIndication,
                                           mAudioDescriptionAvailable, mSpokenSubtitlesAvailable,
                                           mDialogueEnhancementAvailable, mLabels);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }


    /**
     * Used to package this object into a {@link Parcel}.
     *
     * @param dest The {@link Parcel} to be written.
     * @param flags The flags used for parceling.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(getPresentationId());
        dest.writeInt(getProgramId());
        dest.writeString(getLocale().toLanguageTag());
        dest.writeInt(getMasteringIndication());
        dest.writeInt(hasAudioDescription() ? 1 : 0);
        dest.writeInt(hasSpokenSubtitles() ? 1 : 0);
        dest.writeInt(hasDialogueEnhancement() ? 1 : 0);

        dest.writeInt(getLabels().size());
        for (Map.Entry<Locale, String> entry : getLabels().entrySet()) {
            dest.writeString(entry.getKey().toString());
            dest.writeString(entry.getValue());
        }
    }

    @NonNull
    public static final Parcelable.Creator<TvAudioPresentation> CREATOR =
        new Parcelable.Creator<TvAudioPresentation>() {
            @Override
            public TvAudioPresentation createFromParcel(@NonNull Parcel in) {
                return new TvAudioPresentation(in);
            }

            @Override
            public TvAudioPresentation[] newArray(int size) {
                return new TvAudioPresentation[size];
            }
    };
}
