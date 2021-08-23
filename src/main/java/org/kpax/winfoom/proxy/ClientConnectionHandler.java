/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.RequestLine;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.pac.PacScriptEvaluator;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Socket;

/**
 * Responsible for handling client's connection.
 */
@Slf4j
@ThreadSafe
@Component
public class ClientConnectionHandler implements StopListener {

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private PacScriptEvaluator pacScriptEvaluator;

    @Autowired
    private ConnectionProcessorSelector connectionProcessorSelector;

    /**
     * Supplier for ProxyInfo, manual case.
     */
    private final SingletonSupplier<ProxyInfo> proxyInfoSupplier = new SingletonSupplier<>(
            () -> new ProxyInfo(proxyConfig.getProxyType(), proxyConfig.getProxyType().isDirect() ? null :
                    new HttpHost(proxyConfig.getProxyHost(), proxyConfig.getProxyPort())));

    /**
     * Create a {@link ClientConnection} instance then process it.
     *
     * @param socket the client's socket
     * @throws Exception
     */
    public void handleConnection(@NotNull final Socket socket) throws Exception {
        try (ClientConnection clientConnection = proxyConfig.isAutoConfig() ?
                new PacClientConnection(socket, proxyConfig, systemConfig,
                        connectionProcessorSelector, pacScriptEvaluator) :
                new ManualProxyClientConnection(socket, proxyConfig, systemConfig,
                        connectionProcessorSelector, proxyInfoSupplier.get())
        ) {
            RequestLine requestLine = clientConnection.getRequestLine();
            logger.debug("Handle request: {}", requestLine);
            clientConnection.prepare();
            clientConnection.process();
            logger.debug("Done handling request: {}", requestLine);
        }
    }

    @Override
    public void onStop() {
        proxyInfoSupplier.reset();
    }
}
