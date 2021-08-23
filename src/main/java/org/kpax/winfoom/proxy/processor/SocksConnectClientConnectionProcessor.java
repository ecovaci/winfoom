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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HTTP;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.ClientConnection;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.StreamSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;

/**
 * Process a CONNECT request through a SOCKS proxy or no proxy (DIRECT).
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/16/2020
 */

@Slf4j
@ThreadSafe
@Component
class SocksConnectClientConnectionProcessor extends ClientConnectionProcessor {

    @Autowired
    private SystemConfig systemConfig;

    @Override
    void handleRequest(final ClientConnection clientConnection, final ProxyInfo proxyInfo)
            throws IOException {
        RequestLine requestLine = clientConnection.getRequestLine();
        HttpHost target = HttpHost.create(requestLine.getUri());
        Proxy proxy;
        if (proxyInfo.getType().isSocks()) {
            proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(proxyInfo.getProxyHost().getHostName(),
                            proxyInfo.getProxyHost().getPort()));
        } else {
            proxy = Proxy.NO_PROXY;
        }

        try (Socket socket = new Socket(proxy)) {
            systemConfig.configureSocket(socket);
            if (proxyInfo.getType().isSocks4()) {
                HttpUtils.setSocks4(socket);
            }
            logger.debug("Open connection");
            socket.connect(new InetSocketAddress(target.getHostName(), target.getPort()),
                    systemConfig.getSocketConnectTimeout() * 1000);

            logger.debug("Connected to {}", target);

            // Respond with 200 code
            clientConnection.write(String.format("%s 200 Connection established",
                    requestLine.getProtocolVersion()));
            clientConnection.write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER,
                    HttpUtils.getCurrentDate()));
            clientConnection.writeln();

            try {
                // The proxy facade mediates the full duplex communication
                // between the client and the remote proxy
                // This usually ends on connection reset, timeout or any other error
                duplex(StreamSource.from(socket), clientConnection);
            } catch (Exception e) {
                logger.error("Error on full duplex", e);
            }

        }
    }

    @Override
    void handleError(ClientConnection clientConnection,
                     ProxyInfo proxyInfo, Exception e) throws ProxyConnectException {
        if (e instanceof UnknownHostException) {
            clientConnection.writeErrorResponse(HttpStatus.SC_NOT_FOUND, e.getMessage());
        } else if (e instanceof SocketTimeoutException) {
            clientConnection.writeErrorResponse(HttpStatus.SC_GATEWAY_TIMEOUT, e.getMessage());
        } else if (e instanceof SocketException) {
            if (HttpUtils.isConnectionRefused((SocketException) e)
                    || HttpUtils.isConnectionTimeout((SocketException) e)) {
                throw new ProxyConnectException(e.getMessage(), e);
            } else if (HttpUtils.isSOCKSAuthenticationFailed((SocketException) e)) {
                clientConnection.writeProxyAuthRequiredErrorResponse();
            } else {
                clientConnection.writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            clientConnection.writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
