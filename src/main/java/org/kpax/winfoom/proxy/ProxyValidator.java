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
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responsible with proxy config validation.
 */
@Slf4j
@ThreadSafe
@Component
public class ProxyValidator {

    @Autowired
    private ProxyConfig proxyConfig;

    /**
     * Test the proxy settings by issuing a request through the proxy facade.
     *
     * @throws IOException
     * @throws InvalidProxySettingsException
     */
    public void testProxy() throws IOException, InvalidProxySettingsException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpHost target = HttpHost.create(proxyConfig.getProxyTestUrl());
            HttpHost proxy = new HttpHost("localhost", proxyConfig.getLocalPort());
            RequestConfig config = RequestConfig.custom()
                    .setProxy(proxy)
                    .setCircularRedirectsAllowed(true)
                    .build();
            HttpGet request = new HttpGet("/");
            request.setConfig(config);
            logger.info("Executing request " + request.getRequestLine() + " to " + target + " via " + proxy);
            try (CloseableHttpResponse response = httpClient.execute(target, request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("statusCode={}", statusCode);
                if (statusCode > HttpUtils.MAX_HTTP_SUCCESS_CODE) {
                    if (statusCode == HttpStatus.SC_BAD_GATEWAY) {
                        throw new InvalidProxySettingsException("Wrong proxy host/port");
                    } else if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
                        throw new InvalidProxySettingsException("Cannot connect to the provided test URL");
                    } else if (statusCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                        throw new InvalidProxySettingsException("Wrong user/password");
                    } else {
                        throw new InvalidProxySettingsException("Validation failed with statusCode: " + statusCode +
                                " reason: " + response.getStatusLine().getReasonPhrase());
                    }
                }
            }
        }
    }

}
