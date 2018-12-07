/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.ipmemorystore;

import static org.junit.Assert.assertEquals;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParcelableTests {
    @Test
    public void testNetworkAttributesParceling() throws Exception {
        final NetworkAttributes.Builder builder = new NetworkAttributes.Builder();
        NetworkAttributes in = builder.build();
        assertEquals(in, parcelingRoundTrip(in));

        builder.setAssignedAddress(InetAddress.getByName("1.2.3.4"));
        // groupHint stays null this time around
        builder.setDnsAddresses(Collections.emptyList());
        builder.setMtu(18);
        in = builder.build();
        assertEquals(in, parcelingRoundTrip(in));

        builder.setAssignedAddress(InetAddress.getByName("0A1C:2E40:480A::1CA6"));
        builder.setGroupHint("groupHint");
        builder.setDnsAddresses(Arrays.asList(
                InetAddress.getByName("ACA1:652B:0911:DE8F:1200:115E:913B:AA2A"),
                InetAddress.getByName("6.7.8.9")));
        builder.setMtu(1_000_000);
        in = builder.build();
        assertEquals(in, parcelingRoundTrip(in));

        builder.setMtu(null);
        in = builder.build();
        assertEquals(in, parcelingRoundTrip(in));
    }

    @Test
    public void testPrivateDataParceling() throws Exception {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[] {89, 111, 108, 111});
        final PrivateData in = new PrivateData.Builder().setByteBuffer(bb).build();
        final PrivateData out = parcelingRoundTrip(in);
        assertEquals(in, out);
        assertEquals(in.data, out.data);
    }

    @Test
    public void testSameL3NetworkResponseParceling() throws Exception {
        // Ugly hack to help testing without exposing methods.
        // SameL3NetworkResponse does not have a builder and has a package private
        // constructor, which can't be invoked by tests because the class loader for
        // tests is not the same. Making this @VisibleForTesting public opens it for
        // all frameworks clients which is a very bad idea. Therefore use this
        // weird trick to create (Java will hate you). I apologize to anyone who
        // touches the parceling code and has to update this :( That's still the
        // lesser of the two evils.
        final Parcel p = Parcel.obtain();
        p.writeString("key 1");
        p.writeString("key 2");
        p.writeFloat(0.43f);
        p.setDataPosition(0);

        final SameL3NetworkResponse in = SameL3NetworkResponse.CREATOR.createFromParcel(p);
        p.recycle();
        assertEquals("key 1", in.l2Key1);
        assertEquals("key 2", in.l2Key2);
        assertEquals(0.43f, in.confidence, 0.01f /* delta */);

        final SameL3NetworkResponse out = parcelingRoundTrip(in);

        assertEquals(in, out);
        assertEquals(in.l2Key1, out.l2Key1);
        assertEquals(in.l2Key2, out.l2Key2);
        assertEquals(in.confidence, out.confidence, 0.01f /* delta */);
    }

    private <T extends Parcelable> T parcelingRoundTrip(final T in) throws Exception {
        final Parcel p = Parcel.obtain();
        in.writeToParcel(p, /* flags */ 0);
        p.setDataPosition(0);
        final byte[] marshalledData = p.marshall();
        p.recycle();

        final Parcel q = Parcel.obtain();
        q.unmarshall(marshalledData, 0, marshalledData.length);
        q.setDataPosition(0);

        final Parcelable.Creator<T> creator = (Parcelable.Creator<T>)
                in.getClass().getField("CREATOR").get(null); // static object, so null receiver
        final T unmarshalled = (T) creator.createFromParcel(q);
        q.recycle();
        return unmarshalled;
    }
}
