/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * The local proxy server.
 *
 * @author Eugen Covaci
 */
@RequiredArgsConstructor
@Slf4j
@ThreadSafe
@Order(0)
@Component
class LocalProxyServer implements StopListener {

    private final SystemConfig systemConfig;

    private final ProxyConfig proxyConfig;

    private final ProxyExecutorService executorService;

    private final ClientConnectionHandler clientConnectionHandler;

    private ServerSocket serverSocket;

    /**
     * Start the local proxy server.
     * <p>This means:
     * <ul>
     * <li>Opens a {@link ServerSocket} on a local port, then listen for connections</li>
     * <li>When a connection arrives, it delegates the handling to the {@link ClientConnectionHandler}, on a new
     * thread.</li>
     * </ul>
     *
     * @throws Exception
     */
    synchronized void start() throws IOException {
        Assert.isTrue(serverSocket == null || serverSocket.isClosed(),
                "There is an active ServerSocket instance that needs to be closed before creating another one");
        log.info("Start local proxy server with userConfig {}", proxyConfig);
        try {
            serverSocket = new ServerSocket(proxyConfig.getLocalPort(),
                    systemConfig.getServerSocketBacklog());
            executorService.submit(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        systemConfig.configureSocket(socket);
                        executorService.submit(() -> {
                            try {
                                clientConnectionHandler.handleConnection(socket);
                            } catch (Exception e) {
                                log.debug("Error on handling connection", e);
                            } finally {
                                InputOutputs.close(socket);
                            }
                        });
                    } catch (SocketException e) {

                        // The ServerSocket has been closed, exit the while loop
                        if (HttpUtils.isSocketClosed(e)) {
                            break;
                        }

                        // Get connection interrupted error whenever stop the server socket,
                        // there is no reason to debug it
                        if (!HttpUtils.isConnectionInterrupted(e)) {
                            log.debug("Socket error on getting connection", e);
                        }
                    } catch (Exception e) {
                        log.debug("Generic error on getting connection", e);
                    }
                }
            });
            log.info("Server started, listening on port: " + proxyConfig.getLocalPort());
        } catch (Exception e) {
            // Cleanup on exception
            close();
            throw e;
        }
    }

    @Override
    public synchronized void onStop() {
        log.info("Close the local proxy server");
        InputOutputs.close(serverSocket);
    }

}
