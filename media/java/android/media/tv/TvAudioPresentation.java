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
import android.media.AudioPresentation;

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
public final class TvAudioPresentation extends AudioPresentation implements Parcelable {

    private AudioPresentation mAudioPresentation = null;

    private TvAudioPresentation(@NonNull Builder builder) {
        this.mAudioPresentation = builder.mAudioPresentation;
    }

    /**
     * Returns presentation ID used by the framework to select an audio presentation rendered by a
     * decoder. Presentation ID is typically sequential, but does not have to be.
     */
    @Override
    public int getPresentationId() {
        return mAudioPresentation.getPresentationId();
    }

    /**
     * Returns program ID used by the framework to select an audio presentation rendered by a
     * decoder. Program ID can be used to further uniquely identify the presentation to a decoder.
     */
    @Override
    public int getProgramId() {
        return mAudioPresentation.getProgramId();
    }

    /**
     * @return a map of available text labels for this presentation. Each label is indexed by its
     * locale corresponding to the language code as specified by ISO 639-2. Either ISO 639-2/B
     * or ISO 639-2/T could be used.
     */
    @Override
    @NonNull
    public Map<Locale, String> getLabels() {
        return mAudioPresentation.getLabels();
    }

    /**
     * @return a map of available text labels for this presentation. Each label is indexed by its
     * locale corresponding to the language code as specified by ISO 639-2. Either ISO 639-2/B
     * or ISO 639-2/T could be used.
     */
    @Override
    @NonNull
    public Map<ULocale, String> getULabels() {
        return mAudioPresentation.getULabels();
    }

    /**
     * @return the locale corresponding to audio presentation's ISO 639-1/639-2 language code.
     */
    @Override
    @NonNull
    public ULocale getULocale() {
        return mAudioPresentation.getULocale();
    }

    /**
     * @return the mastering indication of the audio presentation.
     * See {@link AudioPresentation#MASTERING_NOT_INDICATED},
     *     {@link AudioPresentation#MASTERED_FOR_STEREO},
     *     {@link AudioPresentation#MASTERED_FOR_SURROUND},
     *     {@link AudioPresentation#MASTERED_FOR_3D},
     *     {@link AudioPresentation#MASTERED_FOR_HEADPHONE}
     */
    @Override
    @MasteringIndicationType
    public int getMasteringIndication() {
        return mAudioPresentation.getMasteringIndication();
    }

    /**
     * Indicates whether an audio description for the visually impaired is available.
     * @return {@code true} if audio description is available.
     */
    @Override
    public boolean hasAudioDescription() {
        return mAudioPresentation.hasAudioDescription();
    }

    /**
     * Indicates whether spoken subtitles for the visually impaired are available.
     * @return {@code true} if spoken subtitles are available.
     */
    @Override
    public boolean hasSpokenSubtitles() {
        return mAudioPresentation.hasSpokenSubtitles();
    }

    /**
     * Indicates whether dialogue enhancement is available.
     * @return {@code true} if dialogue enhancement is available.
     */
    @Override
    public boolean hasDialogueEnhancement() {
        return mAudioPresentation.hasDialogueEnhancement();
    }


    public static class Builder extends AudioPresentation.Builder {
        private AudioPresentation mAudioPresentation = null;
        /**
         * Create a {@link Builder}. Any field that should be included in the
         * {@link TvAudioPresentation} must be added.
         *
         * @param presentationId the presentation ID of this audio presentation
         */
        public Builder(int presentationId) {
            super(presentationId);
        }

        /**
         * Sets the program ID to which this audio presentation refers.
         *
         * @param programId The program ID to be decoded.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull Builder setProgramId(int programId) {
            super.setProgramId(programId);
            return this;
        }

        /**
         * Sets the language information of the audio presentation.
         *
         * @param language Locale corresponding to ISO 639-1/639-2 language code.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull Builder setLocale(@NonNull ULocale language) {
            super.setLocale(language);
            return this;
        }

        /**
         * Sets the mastering indication.
         *
         * @param masteringIndication Input to set mastering indication.
         * @throws IllegalArgumentException if the mastering indication is not any of
         * {@link AudioPresentation#MASTERING_NOT_INDICATED},
         * {@link AudioPresentation#MASTERED_FOR_STEREO},
         * {@link AudioPresentation#MASTERED_FOR_SURROUND},
         * {@link AudioPresentation#MASTERED_FOR_3D},
         * or {@link AudioPresentation#MASTERED_FOR_HEADPHONE}
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull Builder setMasteringIndication(
                @MasteringIndicationType int masteringIndication) {
            super.setMasteringIndication(masteringIndication);
            return this;
        }

        /**
         * Sets locale / text label pairs describing the presentation.
         *
         * @param labels Text label indexed by its locale corresponding to the language code.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull Builder setLabels(@NonNull Map<ULocale, CharSequence> labels) {
            super.setLabels(labels);
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
        @Override
        public @NonNull Builder setHasAudioDescription(boolean audioDescriptionAvailable) {
            super.setHasAudioDescription(audioDescriptionAvailable);
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
        @Override
        public @NonNull Builder setHasSpokenSubtitles(boolean spokenSubtitlesAvailable) {
            super.setHasSpokenSubtitles(spokenSubtitlesAvailable);
            return this;
        }

        /**
         * Indicate whether the presentation supports dialogue enhancement.
         *
         * @param dialogueEnhancementAvailable Set to true if dialogue enhancement is available.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull Builder setHasDialogueEnhancement(boolean dialogueEnhancementAvailable) {
            super.setHasDialogueEnhancement(dialogueEnhancementAvailable);
            return this;
        }

        /**
         * Creates a {@link TvAudioPresentation} instance with the specified fields.
         *
         * @return The new {@link TvAudioPresentation} instance
         */
        @Override
        public @NonNull TvAudioPresentation build() {
            mAudioPresentation = super.build();
            return new TvAudioPresentation(this);
        }
    }

    private TvAudioPresentation(Parcel in) {
        final int presentationId = in.readInt();
        final int programId = in.readInt();
        final ULocale language = new ULocale(in.readString());
        final int masteringIndication = in.readInt();
        final boolean audioDescriptionAvailable = in.readInt() == 0 ? false : true;
        final boolean spokenSubtitlesAvailable = in.readInt() == 0 ? false : true;
        final boolean dialogueEnhancementAvailable = in.readInt() == 0 ? false : true;

        Map<ULocale, CharSequence> labels = new HashMap<ULocale, CharSequence>();
        for (int i = in.readInt(); i > 0; i--) {
            labels.put(new ULocale(in.readString()), in.readString());
        }
        mAudioPresentation = (new AudioPresentation.Builder(presentationId)
                .setProgramId(programId)
                .setLocale(language)
                .setLabels(labels)
                .setMasteringIndication(masteringIndication)
                .setHasAudioDescription(audioDescriptionAvailable)
                .setHasSpokenSubtitles(spokenSubtitlesAvailable)
                .setHasDialogueEnhancement(dialogueEnhancementAvailable)).build();
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
        dest.writeInt(mAudioPresentation.getPresentationId());
        dest.writeInt(mAudioPresentation.getProgramId());
        dest.writeString(mAudioPresentation.getLocale().toLanguageTag());
        dest.writeInt(mAudioPresentation.getMasteringIndication());
        dest.writeInt(mAudioPresentation.hasAudioDescription() ? 1 : 0);
        dest.writeInt(mAudioPresentation.hasSpokenSubtitles() ? 1 : 0);
        dest.writeInt(mAudioPresentation.hasDialogueEnhancement() ? 1 : 0);

        dest.writeInt(mAudioPresentation.getLabels().size());
        for (Map.Entry<Locale, String> entry : mAudioPresentation.getLabels().entrySet()) {
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
