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
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * It manages the HTTP connection pooling mechanism.
 * <p>Only used for non-CONNECT HTTP requests.
 */
@Slf4j
@ThreadSafe
@Order(1)
@Component
class ConnectionPoolingManager implements StopListener {

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private ProxyController proxyController;

    @Autowired
    private SocksConnectionSocketFactory socksConnectionSocketFactory;

    @Autowired
    private Socks4ConnectionSocketFactory socks4ConnectionSocketFactory;

    /**
     * For HTTP proxy type
     */
    private final SingletonSupplier<PoolingHttpClientConnectionManager> httpSupplier =
            new SingletonSupplier<>(() -> createConnectionManager(null));

    /**
     * For SOCKS5 proxy type
     */
    private final SingletonSupplier<PoolingHttpClientConnectionManager> socks5Supplier =
            new SingletonSupplier<>(() -> createSocksConnectionManager(false));

    /**
     * For SOCKS4 proxy type
     */
    private final SingletonSupplier<PoolingHttpClientConnectionManager> socks4Supplier =
            new SingletonSupplier<>(() -> createSocksConnectionManager(true));

    private final List<SingletonSupplier<PoolingHttpClientConnectionManager>> poolingHttpSuppliers =
            Arrays.asList(httpSupplier, socks5Supplier, socks4Supplier);

    /**
     * Lazy getter for HTTP proxy.
     *
     * @return the existent {@link PoolingHttpClientConnectionManager} instance or a new one if {@code null}.
     */
    HttpClientConnectionManager getHttpConnectionManager() {
        return httpSupplier.get();
    }

    /**
     * Lazy getter for SOCKS5 proxy.
     *
     * @return the existent {@link PoolingHttpClientConnectionManager} instance or a new one if {@code null}.
     */
    HttpClientConnectionManager getSocks5ConnectionManager() {
        return socks5Supplier.get();
    }

    /**
     * Lazy getter for SOCKS proxy.
     *
     * @param isSocks4 {@code true} iff the proxy type is SOCKS v4
     * @return the existent {@link PoolingHttpClientConnectionManager} instance or a new one if {@code null}.
     */
    HttpClientConnectionManager getSocksConnectionManager(boolean isSocks4) {
        return isSocks4 ? socks4Supplier.get() : socks5Supplier.get();
    }

    /**
     * Lazy getter for SOCKS4 proxy.
     *
     * @return the existent {@link PoolingHttpClientConnectionManager} instance or a new one if {@code null}.
     */
    HttpClientConnectionManager getSocks4ConnectionManager() {
        return socks4Supplier.get();
    }

    /**
     * A job that closes the idle/expired HTTP connections.
     */
    @Scheduled(fixedRateString = "#{systemConfig.connectionManagerCleanInterval * 1000}")
    void cleanUpConnectionManager() {
        if (proxyController.isRunning()) {
            logger.debug("Execute connection manager pool clean up task");
            poolingHttpSuppliers.stream().filter(SingletonSupplier::hasValue).forEach((connectionManagerSupplier) -> {
                try {
                    PoolingHttpClientConnectionManager connectionManager = connectionManagerSupplier.get();
                    connectionManager.closeExpiredConnections();
                    connectionManager.closeIdleConnections(systemConfig.getConnectionManagerIdleTimeout(),
                            TimeUnit.SECONDS);
                    if (logger.isDebugEnabled()) {
                        logger.debug("PoolingHttpClientConnectionManager statistics {}",
                                connectionManager.getTotalStats());
                    }
                } catch (Exception e) {
                    logger.debug("Error on cleaning connection pool", e);
                }
            });
        }
    }

    /**
     * Create a generic {@link PoolingHttpClientConnectionManager}
     *
     * @param socketFactoryRegistry the {@link Registry} instance used to configure the
     *                              {@link PoolingHttpClientConnectionManager}.
     * @return the new {@link PoolingHttpClientConnectionManager} instance.
     * @throws IllegalStateException when the proxy is not started.
     */
    private PoolingHttpClientConnectionManager createConnectionManager(Registry<ConnectionSocketFactory> socketFactoryRegistry) {
        Assert.state(proxyController.isRunning(), "Cannot create connectionManagers: ConnectionPoolingManager is not started");
        PoolingHttpClientConnectionManager connectionManager = socketFactoryRegistry != null
                ? new PoolingHttpClientConnectionManager(socketFactoryRegistry) :
                new PoolingHttpClientConnectionManager();
        logger.info("Configure connection manager");
        if (systemConfig.getMaxConnections() != null) {
            connectionManager.setMaxTotal(systemConfig.getMaxConnections());
        }
        if (systemConfig.getMaxConnectionsPerRoute() != null) {
            connectionManager.setDefaultMaxPerRoute(systemConfig.getMaxConnectionsPerRoute());
        }
        return connectionManager;
    }

    /**
     * Create a SOCKS {@link PoolingHttpClientConnectionManager}
     *
     * @param isSocks4 whether the SOCKS version is {@code 4} or not.
     * @return the new {@link PoolingHttpClientConnectionManager} instance.
     * @throws IllegalStateException when the proxy is not started.
     */
    private PoolingHttpClientConnectionManager createSocksConnectionManager(boolean isSocks4) {
        ConnectionSocketFactory connectionSocketFactory = isSocks4
                ? socks4ConnectionSocketFactory : socksConnectionSocketFactory;
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", connectionSocketFactory)
                .register("https", connectionSocketFactory)
                .build();
        return createConnectionManager(socketFactoryRegistry);
    }

    @Override
    public void onStop() {
        logger.debug("Close all active connection managers and reset the suppliers");
        poolingHttpSuppliers.stream().filter(SingletonSupplier::hasValue).
                forEach(SingletonSupplier::reset);
    }
}
