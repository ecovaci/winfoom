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
import org.apache.http.RequestLine;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;

import java.net.Socket;

/**
 * Responsible for handling client's connection.
 */
@Slf4j
public abstract class ClientConnectionHandler {

    protected final ProxyConfig proxyConfig;

    protected final SystemConfig systemConfig;

    public ClientConnectionHandler(ProxyConfig proxyConfig,
                                   SystemConfig systemConfig) {
        this.proxyConfig = proxyConfig;
        this.systemConfig = systemConfig;
    }

    /**
     * Create a {@link ClientConnection} instance then process it.
     *
     * @param socket the client's socket
     * @throws Exception
     */
    public void handleConnection(@NotNull final Socket socket) throws Exception {
        try (ClientConnection clientConnection = new ClientConnection(
                socket, proxyConfig, systemConfig)) {
            RequestLine requestLine = clientConnection.getRequestLine();
            log.debug("Handle request: {}", requestLine);
            clientConnection.prepare();
            processConnection(clientConnection);
            log.debug("Done handling request: {}", requestLine);
        }
    }

    abstract void processConnection(ClientConnection clientConnection) throws Exception;
}

