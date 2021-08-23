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
import org.apache.http.*;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthState;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteInfo.LayerType;
import org.apache.http.conn.routing.RouteInfo.TunnelType;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.auth.HttpAuthenticator;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.execchain.TunnelRefusedException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.*;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.kpax.winfoom.util.functional.ProxySingletonSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.Socket;

/**
 * Establish a tunnel via a HTTP proxy.<br>
 * It is an adaptation of {@link org.apache.http.impl.client.ProxyClient}
 *
 * @author Eugen Covaci
 */
@Slf4j
@ThreadSafe
@Component
public class TunnelConnection {

    @Autowired
    private ProxySingletonSupplier<CredentialsProvider> credentialsProviderSupplier;

    @Autowired
    private ProxySingletonSupplier<Registry<AuthSchemeProvider>> authSchemeRegistrySupplier;

    @Autowired
    private SystemConfig systemConfig;

    private HttpProcessor httpProcessor;
    private HttpRequestExecutor requestExec;
    private ProxyAuthenticationStrategy proxyAuthStrategy;
    private HttpAuthenticator authenticator;

    @PostConstruct
    void init() {
        this.httpProcessor = new ImmutableHttpProcessor(new RequestTargetHost(),
                new RequestClientConnControl(), new RequestUserAgent());
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new ProxyAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
    }

    public Tunnel open(final HttpHost proxy, final HttpHost target,
                       final ProtocolVersion protocolVersion)
            throws IOException, HttpException {
        Args.notNull(proxy, "Proxy host");
        Args.notNull(target, "Target host");

        AuthState proxyAuthState = new AuthState();

        HttpHost host = target;
        if (host.getPort() <= 0) {
            host = new HttpHost(host.getHostName(), 80, host.getSchemeName());
        }
        final HttpRoute route = new HttpRoute(host, RequestConfig.DEFAULT.getLocalAddress(),
                proxy, false, TunnelType.TUNNELLED, LayerType.PLAIN);
        final ManagedHttpClientConnection connection = ManagedHttpClientConnectionFactory.INSTANCE.create(route,
                ConnectionConfig.DEFAULT);
        final HttpContext context = new BasicHttpContext();
        final HttpRequest connect = new BasicHttpRequest(HttpUtils.HTTP_CONNECT, host.toHostString(), protocolVersion);

        // Populate the execution context
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, connect);
        context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
        context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, proxyAuthState);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credentialsProviderSupplier.get());
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, RequestConfig.DEFAULT);
        context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, authSchemeRegistrySupplier.get());

        requestExec.preProcess(connect, httpProcessor, context);

        HttpResponse response;
        while (true) {
            if (!connection.isOpen()) {
                Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                systemConfig.configureSocket(socket);
                connection.bind(socket);
            }

            authenticator.generateAuthResponse(connect, proxyAuthState, context);
            response = requestExec.execute(connect, connection, context);

            final int status = response.getStatusLine().getStatusCode();
            logger.debug("Tunnel status code: {}", status);
            if (status < HttpStatus.SC_OK) {
                throw new HttpException("Unexpected response to CONNECT request: " + response.getStatusLine());
            }

            if (authenticator.isAuthenticationRequested(
                    proxy, response, proxyAuthStrategy, proxyAuthState, context)) {
                if (authenticator.handleAuthChallenge(
                        proxy, response, proxyAuthStrategy, proxyAuthState, context)) {
                    // Retry request
                    if (DefaultConnectionReuseStrategy.INSTANCE.keepAlive(response, context)) {
                        // Consume response content
                        logger.debug("Now consume entity");
                        EntityUtils.consume(response.getEntity());
                    } else {
                        logger.debug("Close tunnel connection");
                        InputOutputs.close(connection);
                    }
                    // discard previous auth header
                    connect.removeHeaders(AUTH.PROXY_AUTH_RESP);
                } else {
                    break;
                }
            } else {
                break;
            }

        }

        final int status = response.getStatusLine().getStatusCode();
        logger.debug("Tunnel final status code: {}", status);

        if (status > HttpUtils.MAX_HTTP_SUCCESS_CODE) { // Error case

            // Buffer response content
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntity(entity));
            }
            logger.debug("Close tunnel connection");
            InputOutputs.close(connection);
            throw new TunnelRefusedException("CONNECT refused by proxy: " + response.getStatusLine(), response);
        }

        return new Tunnel(connection, response);
    }

}
