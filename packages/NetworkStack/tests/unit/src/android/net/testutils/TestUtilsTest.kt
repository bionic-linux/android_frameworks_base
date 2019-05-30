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


package android.net.testutils

import android.os.HandlerThread
import com.android.testutils.waitForIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random
import kotlin.test.assertEquals

const val TIMEOUT_MS = 200

@RunWith(JUnit4::class)
class TestUtilsTest {
    @Test
    fun testWaitForIdle() {
        val handlerThread = HandlerThread("testHandler").apply { start() }

        val attempts = 50  // Causes the test to take about 200ms on bullhead-eng.

        // Tests that waitForIdle returns immediately if the service is already idle.
        repeat(attempts) {
            handlerThread.waitForIdle(TIMEOUT_MS)
        }

        // Tests that calling waitForIdle waits for messages to be processed.
        var memory = 0
        repeat(attempts) { i ->
            handlerThread.threadHandler.postDelayed(Random.nextInt(0, TIMEOUT_MS)) { memory += i }
            handlerThread.waitForIdle(TIMEOUT_MS)
            assertEquals(memory, i * (i + 1) / 2)
        }
    }
}
