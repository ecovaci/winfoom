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

package org.kpax.winfoom.proxy.processor;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.ProxyAuthorizationException;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.ClientConnection;
import org.kpax.winfoom.proxy.ProxyBlacklist;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.kpax.winfoom.util.StreamSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Process a {@link ClientConnection} with a certain {@link ProxyInfo}.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/13/2020
 */
@RequiredArgsConstructor
@Slf4j
public abstract class ClientConnectionProcessor {

    private final ExecutorService executorService;

    protected final ProxyConfig proxyConfig;

    protected final ProxyBlacklist proxyBlacklist;

    /**
     * Process the client's connection. That is:<br>
     * <ul>
     * <li>Prepare the client's request to make a remote HTTP request through the proxy or direct.</li>
     * <li>Make the remote HTTP request.</li>
     * <li>Give back to the client the resulted response (commit the response).</li>
     * </ul>
     * <p><b>Note: This method must not commit the response if doesn't return normally.</b></p>
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        The {@link ProxyInfo} used to make the remote HTTP request.
     * @throws HttpException if a HTTP exception has occurred
     * @throws IOException   if an input/output error occurs
     */
    abstract void handleRequest(@NotNull final ClientConnection clientConnection,
                                @NotNull final ProxyInfo proxyInfo)
            throws IOException, HttpException, ProxyAuthorizationException;

    /**
     * Handle the exception thrown by {@link #handleRequest(ClientConnection, ProxyInfo)} method.
     * <p><b>Note: This method must either commit the response or throw a {@link ProxyConnectException}</b></p>
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        the {@link ProxyInfo} used to make the remote HTTP request.
     * @param e                the exception thrown by {@link #handleRequest(ClientConnection, ProxyInfo)} method
     * @throws ProxyConnectException
     */
    abstract void handleError(@NotNull final ClientConnection clientConnection,
                              @NotNull final ProxyInfo proxyInfo,
                              @NotNull final Exception e)
            throws ProxyConnectException;

    /**
     * Simultaneously transfer bytes between two sources in a mutually independent manner.
     *
     * @param firstSource  The first source.
     * @param secondSource The second source.
     */
    void duplex(@NotNull final StreamSource firstSource,
                @NotNull final StreamSource secondSource) {
        logger.debug("Start full duplex communication");
        Future<?> secondToFirst = executorService.submit(
                () -> {
                    try {
                        secondSource.getInputStream().transferTo(firstSource.getOutputStream());
                    } catch (SocketTimeoutException e) {
                        logger.debug("Timeout exception on executing second to first transfer: {}", e.getMessage());
                    } catch (SocketException e) {
                        logger.debug("Socket exception on executing second to first transfer: {}", e.getMessage());
                    } catch (Exception e) {
                        logger.debug("Error on executing second to first transfer", e);
                    }
                });
        try {
            firstSource.getInputStream().transferTo(secondSource.getOutputStream());
        } catch (SocketTimeoutException e) {
            logger.debug("Timeout exception on executing first to second transfer: {}", e.getMessage());
        } catch (SocketException e) {
            logger.debug("Socket exception on executing first to second transfer: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Error on executing first to second transfer", e);
        }
        if (!secondToFirst.isDone()) {
            // Wait for the async transfer to finish
            try {
                secondToFirst.get();
            } catch (ExecutionException e) {// Normally, we shouldn't get here
                logger.debug("Error on executing second to first transfer", e.getCause());
            } catch (InterruptedException e) {
                logger.debug("Transfer from second to first interrupted: {}", e.getMessage());
            } catch (CancellationException e) {
                logger.debug("Transfer from second to first cancelled: {}", e.getMessage());
            }
        }
        logger.debug("End full duplex communication");
    }


    /**
     * Call the {@link #handleRequest(ClientConnection, ProxyInfo)} method
     * then {@link #handleError(ClientConnection, ProxyInfo, Exception)} method
     * if an exception occurs.
     * Also, blacklist the autoconfig proxy on {@link ProxyConnectException}.
     * <p>If it returns normally, the response will be considered committed.</p>
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        the {@link ProxyInfo} used to make the remote HTTP request.
     * @throws ProxyConnectException
     */
    public final void process(@NotNull final ClientConnection clientConnection,
                              @NotNull final ProxyInfo proxyInfo)
            throws ProxyConnectException {
        logger.debug("Process {} for {}", clientConnection, proxyInfo);
        try {
            handleRequest(clientConnection, proxyInfo);
        } catch (Exception e) {
            logger.debug("Error on handling request", e);
            try {
                handleError(clientConnection, proxyInfo, e);
            } catch (ProxyConnectException pce) {
                if (proxyConfig.isAutoConfig()) {
                    proxyBlacklist.blacklist(proxyInfo);
                }
                throw pce;
            }
        }
    }

}
