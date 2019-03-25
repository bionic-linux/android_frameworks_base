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
    @NonNull private final AtomicReference<CompletableFuture<IIpMemoryStore>> mTailNode;

    public IpMemoryStore(@NonNull final Context context) {
        super(context);
        mService = new CompletableFuture<>();
        mTailNode = new AtomicReference<CompletableFuture<IIpMemoryStore>>(mService);
        getNetworkStackClient().fetchIpMemoryStore(
                new IIpMemoryStoreCallbacks.Stub() {
                    @Override
                    public void onIpMemoryStoreFetched(@NonNull final IIpMemoryStore memoryStore) {
                        mService.complete(memoryStore);
                    }
                });
    }

    /*
     *  This API enqueues the requests for IpMemoryStore service running in a separate
     *  thread before the IpMemoryStore is not ready. And once the IpMemoryStore service
     *  gets ready, this API would be synchronous call.
     *
     *  Leveraging AtomicReference and getAndUpdate to make guarantees for the requests
     *  order. Calling CompletionStage#thenAccept on an already completed CompletableFuture
     *  immediately to achieve synchronization.
     *
     *  Meanwhile ensure that calling CompletableFuture#thenAccept on one completed
     *  CompletableFuture immediately will avoid the memory leak due to a mistaken reference
     *  to the old return value of AtomicReference#getAndUpdate.
     */
    @Override
    protected void runWhenServiceReady(Consumer<IIpMemoryStore> cb) throws ExecutionException {
        mTailNode.getAndUpdate(future -> future.handle((store, exception) -> {
            cb.accept(store);
            return store;
        }));
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
