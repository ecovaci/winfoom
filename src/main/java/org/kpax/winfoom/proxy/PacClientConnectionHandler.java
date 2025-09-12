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
import org.apache.http.HttpStatus;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.pac.PacScriptEvaluator;
import org.kpax.winfoom.proxy.processor.ClientConnectionProcessor;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;
import org.kpax.winfoom.util.HttpUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * Responsible for handling client's connection.
 */
@Slf4j
@ThreadSafe
@Component
class PacClientConnectionHandler extends ClientConnectionHandler {

    private final PacScriptEvaluator pacScriptEvaluator;

    private final ConnectionProcessorSelector connectionProcessorSelector;

    public PacClientConnectionHandler(ProxyConfig proxyConfig,
                                      SystemConfig systemConfig,
                                      PacScriptEvaluator pacScriptEvaluator,
                                      ConnectionProcessorSelector connectionProcessorSelector) {
        super(proxyConfig, systemConfig);
        this.pacScriptEvaluator = pacScriptEvaluator;
        this.connectionProcessorSelector = connectionProcessorSelector;
    }

    @Override
    void processConnection(ClientConnection clientConnection) throws Exception {
        URI requestUri = clientConnection.getRequestUri();
        log.debug("Extracted URI from request {}", requestUri);

        List<ProxyInfo> activeProxies = getActiveProxies(clientConnection, requestUri);

        if (activeProxies.isEmpty()) {
            clientConnection.writeBadGatewayResponse("Proxy Auto Config error: no available proxy server");
            throw new IllegalStateException("All proxy servers are blacklisted!");
        }

        processClientConnection(clientConnection, activeProxies);
    }

    private void processClientConnection(ClientConnection clientConnection, List<ProxyInfo> activeProxies) {
        for (Iterator<ProxyInfo> itr = activeProxies.iterator(); itr.hasNext(); ) {
            ProxyInfo proxy = itr.next();
            ClientConnectionProcessor connectionProcessor = connectionProcessorSelector.select(clientConnection.isConnect(),
                    proxy.getType().isHttp());
            log.debug("Process connection for proxy {} using connectionProcessor: {}", proxy, connectionProcessor);
            try {
                connectionProcessor.process(clientConnection, proxy);
                break;
            } catch (ProxyConnectException e) {
                log.debug("Proxy connect error", e);
                if (itr.hasNext()) {
                    log.debug("Failed to connect to proxy: {}", proxy);
                } else {
                    log.debug("Failed to connect to proxy: {}, send the error response", proxy);
                    // Cannot connect to the remote proxy,
                    // commit a response with 502 error code
                    clientConnection.writeBadGatewayResponse(e.getMessage());
                }
            }
        }
    }

    private List<ProxyInfo> getActiveProxies(ClientConnection clientConnection, URI requestUri) throws Exception {
        List<ProxyInfo> activeProxies;
        try {
            activeProxies = pacScriptEvaluator.findProxyForURL(requestUri);
            log.debug("activeProxies: {}", activeProxies);
        } catch (Exception e) {
            clientConnection.writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpUtils.reasonPhraseForPac(e));
            throw e;
        }
        return activeProxies;
    }
}
