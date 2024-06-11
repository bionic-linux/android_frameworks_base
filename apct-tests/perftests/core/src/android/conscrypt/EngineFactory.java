/*
 * Copyright 2017 The Android Open Source Project
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
package android.conscrypt;

import javax.net.ssl.SSLEngine;

/**
 * Factory for {@link SSLEngine} instances.
 */
public enum EngineFactory {
  @SuppressWarnings("unused")
  CONSCRYPT(newConscryptFactories(false)),
  CONSCRYPT_ENGINE(newConscryptFactories(true));

  private final Factories factories;

  EngineFactory(Factories factories) {
    this.factories = factories;
  }

  @Override
  public ClientEndpoint newClient(ChannelType channelType, int port, String[] protocols,
      String[] ciphers) throws IOException {
    return new ClientEndpoint(
        factories.clientFactory, channelType, port, protocols, ciphers);
  }

  @Override
  public ServerEndpoint newServer(ChannelType channelType, int messageSize,
      String[] protocols, String[] ciphers) throws IOException {
    return new ServerEndpoint(factories.serverFactory, factories.serverSocketFactory,
        channelType, messageSize, protocols, ciphers);
  }

  private static final class Factories {
    final SSLSocketFactory clientFactory;
    final SSLSocketFactory serverFactory;
    final SSLServerSocketFactory serverSocketFactory;

    private Factories(SSLSocketFactory clientFactory, SSLSocketFactory serverFactory,
        SSLServerSocketFactory serverSocketFactory) {
      this.clientFactory = clientFactory;
      this.serverFactory = serverFactory;
      this.serverSocketFactory = serverSocketFactory;
    }
  }

  private static Factories newConscryptFactories(boolean useEngineSocket) {
    Provider provider = TestUtils.getConscryptProvider();
    SSLContext clientContext = TestUtils.newClientSslContext(provider);
    SSLContext serverContext = TestUtils.newServerSslContext(provider);
    final SSLSocketFactory clientFactory = clientContext.getSocketFactory();
    final SSLSocketFactory serverFactory = serverContext.getSocketFactory();
    final SSLServerSocketFactory serverSocketFactory = serverContext.getServerSocketFactory();
    TestUtils.setUseEngineSocket(clientFactory, useEngineSocket);
    TestUtils.setUseEngineSocket(serverFactory, useEngineSocket);
    TestUtils.setUseEngineSocket(serverSocketFactory, useEngineSocket);
    return new Factories(clientFactory, serverFactory, serverSocketFactory);
  }
}