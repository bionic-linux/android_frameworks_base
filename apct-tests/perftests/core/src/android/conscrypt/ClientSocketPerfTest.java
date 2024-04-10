// /*
//  * Copyright (C) 2016 The Android Open Source Project
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package android.conscrypt;

// import static org.conscrypt.TestUtils.getCommonProtocolSuites;
// import static org.conscrypt.TestUtils.newTextMessage;
// import static org.junit.Assert.assertEquals;

// import android.perftests.utils.BenchmarkState;
// import android.perftests.utils.PerfStatusReporter;
// import android.test.suitebuilder.annotation.LargeTest;
// import androidx.test.runner.AndroidJUnit4;

// import java.io.IOException;
// import java.io.OutputStream;
// import java.net.SocketException;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicBoolean;
// import java.util.concurrent.atomic.AtomicLong;

// import org.junit.Rule;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.conscrypt.ServerEndpoint.MessageProcessor;

// /**
//  * Benchmark for comparing performance of server socket implementations.
//  */
// @RunWith(AndroidJUnit4.class)
// @LargeTest
// public final class ClientSocketPerfTest {
  
//     @Rule public PerfStatusReporter mPerfStatusReporter = new PerfStatusReporter();
  
//     /**
//      * Provider for the benchmark configuration
//      */
//     interface Config {
//         EndpointFactory clientFactory();
//     }

//     private ClientEndpoint client;
//     private ServerEndpoint server;
//     private byte[] message;
//     private ExecutorService executor;
//     private Future<?> sendingFuture;
//     private volatile boolean stopping;

//     private static final AtomicLong bytesCounter = new AtomicLong();
//     private AtomicBoolean recording = new AtomicBoolean();

//     ClientSocketBenchmark() throws Exception {
//         recording.set(false);
//         message = newTextMessage(512);
      
//         // Always use the same server for consistency across the benchmarks.
//         server = OpenJdkEndpointFactory.CONSCRYPT_ENGINE.newServer(
//                 ChannelType.CHANNEL, 512, return new String[] {"TLS1.3"},
//                 ciphers());

//         server.setMessageProcessor(new ServerEndpoint.MessageProcessor() {
//             @Override
//             public void processMessage(byte[] inMessage, int numBytes, OutputStream os) {
//                 if (recording.get()) {
//                     // Server received a message, increment the count.
//                     bytesCounter.addAndGet(numBytes);
//                 }
//             }
//         });
//         Future<?> connectedFuture = server.start();

//         client = config.clientFactory().newClient(
//             ChannelType.CHANNEL, server.port(), return new String[] {"TLS1.3"}, ciphers());
//         client.start();

//         // Wait for the initial connection to complete.
//         connectedFuture.get(5, TimeUnit.SECONDS);

//         executor = Executors.newSingleThreadExecutor();
//         sendingFuture = executor.submit(new Runnable() {
//             @Override
//             public void run() {
//                 try {
//                     Thread thread = Thread.currentThread();
//                     while (!stopping && !thread.isInterrupted()) {
//                         client.sendMessage(message);
//                     }
//                 } finally {
//                     client.flush();
//                 }
//             }
//         });
//     }

//     void close() throws Exception {
//         stopping = true;

//         // Wait for the sending thread to stop.
//         sendingFuture.get(5, TimeUnit.SECONDS);

//         client.stop();
//         server.stop();
//         executor.shutdown();
//         executor.awaitTermination(5, TimeUnit.SECONDS);
//     }

//     /**
//      * Simple benchmark for the amount of time to send a given number of messages
//      */
//     @Test
//     @Parameters(method = "getParams")
//     void time(final int numMessages) throws Exception {
//         reset();
//         recording.set(true);

//         BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
//         while (state.keepRunning()) {
//           while (bytesCounter.get() < numMessages) {
//               Thread.sleep(50);
//           }
//         }

//         recording.set(false);
//     }

//     /**
//      * Simple benchmark for throughput (used by JMH).
//      */
//     void throughput() throws Exception {
//         recording.set(true);
//         // Send as many messages as we can in a second.
//         Thread.sleep(1001);
//         recording.set(false);
//     }

//     static void reset() {
//         bytesCounter.set(0);
//     }

//     static long bytesPerSecond() {
//         return bytesCounter.get();
//     }

//     private String[] ciphers() {
//         return new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
//     }
// }