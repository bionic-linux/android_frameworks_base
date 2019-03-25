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

package android.net;

import android.annotation.NonNull;
import android.content.Context;

import com.android.internal.annotations.VisibleForTesting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Manager class used to communicate with the ip memory store service in the network stack,
 * which is running in a separate module.
 * @hide
*/
public class IpMemoryStore extends IpMemoryStoreClient {
    @NonNull private final CompletableFuture<IIpMemoryStore> mService;
    @NonNull private final AtomicReference<CompletableFuture<IIpMemoryStore>> mTailNode =
            new AtomicReference<>();

    public IpMemoryStore(@NonNull final Context context) {
        super(context);
        mService = new CompletableFuture<>();
        getNetworkStackClient().fetchIpMemoryStore(
                new IIpMemoryStoreCallbacks.Stub() {
                    @Override
                    public void onIpMemoryStoreFetched(final IIpMemoryStore memoryStore) {
                        mService.complete(memoryStore);
                    }
                });
    }

    /*
     *  leverage AtomicReference and getAndUpdate to make guarantees for the order
     *  which the IpMemoryStore service APIs should be called in. This API enqueues
     *  each API call in order. And Calling CompletionStage#thenAccept on an already
     *  completed CompletableFuture immediately to achieve synchronization.
     *
     *  Previously CompletableFuture#get() will be blocked until the CompletableFuture
     *  #complete() occurs. Making sure that calling CompletableFuture#thenAccept on a
     *  completed CompletableFuture immediately will avoid the memory leak due to a
     *  mistakn reference to the old return value of AtomicReference#getAndUpdate.
     */
    protected void enqueue(Consumer<IIpMemoryStore> cb) throws ExecutionException {
        mTailNode.getAndUpdate(futureStore -> {
            futureStore.thenAccept(cb);
            return futureStore;
        });
    }

    @VisibleForTesting
    protected NetworkStackClient getNetworkStackClient() {
        return NetworkStackClient.getInstance();
    }

    /** Gets an instance of the memory store */
    @NonNull
    public static IpMemoryStore getMemoryStore(final Context context) {
        return new IpMemoryStore(context);
    }
}
