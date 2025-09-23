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
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.proxy.processor.ClientConnectionProcessor;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.stereotype.Component;

/**
 * Responsible for handling client's connection.
 */
@Slf4j
@ThreadSafe
@Component
class ManualClientConnectionHandler extends ClientConnectionHandler implements StopListener {

    private final ConnectionProcessorSelector connectionProcessorSelector;

    /**
     * Supplier for ProxyInfo, manual case.
     */
    private final SingletonSupplier<ProxyInfo> proxyInfoSupplier;

    public ManualClientConnectionHandler(ProxyConfig proxyConfig,
                                         SystemConfig systemConfig,
                                         ConnectionProcessorSelector connectionProcessorSelector) {
        super(proxyConfig, systemConfig);
        this.connectionProcessorSelector = connectionProcessorSelector;
        this.proxyInfoSupplier = new SingletonSupplier<>(
                () -> new ProxyInfo(proxyConfig.getProxyType(), proxyConfig.getProxyType().isDirect() ? null :
                        new HttpHost(proxyConfig.getProxyHost(), proxyConfig.getProxyPort())));
    }

    @Override
    void processConnection(ClientConnection clientConnection) {
        ClientConnectionProcessor connectionProcessor = connectionProcessorSelector.select(clientConnection.isConnect(),
                proxyInfoSupplier.get().getType().isHttp());
        try {
            connectionProcessor.process(clientConnection, proxyInfoSupplier.get());
        } catch (ProxyConnectException e) {
            log.debug("Failed to connect to proxy: {}, send the error response", proxyInfoSupplier.get());
            // Cannot connect to the remote proxy,
            // commit a response with 502 error code
            clientConnection.writeBadGatewayResponse(e.getMessage());
        }
    }

    @Override
    public void onStop() {
        proxyInfoSupplier.reset();
    }
}
