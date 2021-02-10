/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net.vcn;

/** @hide */
public abstract class VcnGatewayError implements Parcelable {
    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {}

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<VcnGatewayError> CREATOR =
            new Creator<VcnGatewayError>() {
                public VcnGatewayError createFromParcel(Parcel in) {
                    return new VcnGatewayError();
                }

                public VcnGatewayError[] newArray(int size) {
                    return new VcnGatewayError[size];
                }
            };

    public static class VcnGatewayAuthError extends VcnGatewayError {
        public final String msg;

        public VcnGatewayAuthError(String msg) {
            this.msg = msg;
        }

        /** {@inheritDoc} */
        @Override
        public int describeContents() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(msg);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<VcnGatewayAuthError> CREATOR =
                new Creator<VcnGatewayAuthError>() {
                    public VcnGatewayAuthError createFromParcel(Parcel in) {
                        return new VcnGatewayAuthError(in.readString());
                    }

                    public VcnGatewayAuthError[] newArray(int size) {
                        return new VcnGatewayAuthError[size];
                    }
                };
    }

    public static class VcnGatewayIkeError extends VcnGatewayError {
        public final int error;

        public VcnGatewayIkeError(int error) {
            this.error = error;
        }

        /** {@inheritDoc} */
        @Override
        public int describeContents() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(error);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<VcnGatewayIkeError> CREATOR =
                new Creator<VcnGatewayIkeError>() {
                    public VcnGatewayIkeError createFromParcel(Parcel in) {
                        return new VcnGatewayIkeError(in.readInt());
                    }

                    public VcnGatewayIkeError[] newArray(int size) {
                        return new VcnGatewayIkeError[size];
                    }
                };
    }
}
