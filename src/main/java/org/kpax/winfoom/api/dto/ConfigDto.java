/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemContext;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.util.HttpUtils;
import org.springframework.util.Assert;

/**
 * The config DTO for API server.
 */
@Getter
@Setter
@ToString
public class ConfigDto {

    private ProxyConfig.Type proxyType;
    private Boolean useCurrentCredentials;
    private String proxyUsername;
    private String proxyPassword;
    private String proxyPacFileLocation;
    private Integer blacklistTimeout;
    private String proxyHost;
    private Integer proxyPort;
    private Integer localPort;
    private String proxyTestUrl;

    private ProxyConfig.HttpAuthProtocol httpAuthProtocol;
    private ProxyConfig.HttpAuthProtocol pacHttpAuthProtocol;


    public void validate() throws InvalidProxySettingsException {
        if (proxyHost != null || proxyPort != null || useCurrentCredentials != null) {
            Assert.state(proxyType != null, "proxyType must be specified when proxyHost or proxyPort or useCurrentCredentials are provided");
            Assert.state(proxyType != ProxyConfig.Type.DIRECT, "When proxyType is DIRECT, none of proxyHost, proxyPort or useCurrentCredentials can be provided");
        }

        if (proxyPort != null) {
            if (!HttpUtils.isValidPort(proxyPort)) {
                throw new InvalidProxySettingsException("Invalid proxyPort, allowed range: 1 - 65535");
            }
        }

        if (localPort != null) {
            if (!HttpUtils.isValidPort(localPort)) {
                throw new InvalidProxySettingsException("Invalid localPort, allowed range: 1 - 65535");
            }
        }

        if (useCurrentCredentials != null) {
            Assert.state(proxyType == ProxyConfig.Type.HTTP, "proxyType must be HTTP when useCurrentCredentials is provided");
            if (useCurrentCredentials && !SystemContext.IS_OS_WINDOWS) {
                throw new InvalidProxySettingsException("The field useCurrentCredentials is only allowed on Windows OS");
            }
        }
    }

}
