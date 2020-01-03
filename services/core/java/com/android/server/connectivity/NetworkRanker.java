/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.connectivity;

import static android.net.NetworkScore.LEGACY_SCORE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.net.NetworkSelectionSettings;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Comparator;

/**
 * A class that knows how to find the best network matching a request out of a list of networks.
 */
public class NetworkRanker {
    private final NetworkRequest mDefaultRequest;
    private NetworkSelectionSettings mSelectionSettings;

    public NetworkRanker(@NonNull final NetworkRequest defaultRequest,
            @NonNull final NetworkSelectionSettings selectionSettings) {
        mDefaultRequest = defaultRequest;
        mSelectionSettings = selectionSettings;
    }

    /**
     * Find the best network satisfying this request among the list of passed networks.
     */
    // Almost equivalent to Collections.max(nais), but allows returning null if no network
    // satisfies the request.
    @Nullable
    public NetworkAgentInfo getBestNetwork(@NonNull final NetworkRequest request,
            @NonNull final Collection<NetworkAgentInfo> nais) {
        NetworkAgentInfo bestNetwork = null;
        int bestScore = Integer.MIN_VALUE;
        for (final NetworkAgentInfo nai : nais) {
            if (!nai.satisfies(request)) continue;
            if (nai.getCurrentScore() > bestScore) {
                bestNetwork = nai;
                bestScore = nai.getCurrentScore();
            }
        }
        return bestNetwork;
    }

    /**
     * A setter for updating the NetworkSelectionSettings.
     */
    public void setNetworkSelectionSettings(
            @NonNull final NetworkSelectionSettings selectionSettings) {
        mSelectionSettings = selectionSettings;
    }

    /**
     * Compares two {@link NetworkScore} for the given {@link NetworkRequest}.
     *
     * @param  ns1 The first {@link NetworkScore} to compare.
     * @param  ns2 The second {@link NetworkScore} to compare.
     * @param  request The {@link NetworkRequest} which is trying to find the best
     *         {@link NetworkScore} to it.
     * @return A negative integer, zero, or a positive integer is corresponding to the first
     *         {@link NetworkScore} is less than, equal to, or greater than the second.
     *
     * TODO: Compares more fields of NetworkScore once all of them are ready.
     */
    @VisibleForTesting
    protected int compareNetworks(@NonNull NetworkScore ns1, @NonNull NetworkScore ns2,
            @NonNull NetworkRequest request) {
        return Integer.compare(ns1.getIntExtension(LEGACY_SCORE),
                ns2.getIntExtension(LEGACY_SCORE));
    }

    /**
     * Return comparator for comparing NetworkScore.
     */
    @NonNull
    public Comparator<NetworkScore> makeComparator(@NonNull NetworkRequest request) {
        return (ns1, ns2) -> compareNetworks(ns1, ns2, request);
    }
}
