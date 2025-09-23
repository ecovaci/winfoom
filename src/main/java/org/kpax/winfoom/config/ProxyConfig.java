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

package org.kpax.winfoom.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.kpax.winfoom.api.json.Mask;
import org.kpax.winfoom.api.json.Views;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.proxy.ProxyType;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.jna.IEProxyConfig;
import org.kpax.winfoom.util.jna.WinHttpHelpers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

/**
 * The proxy facade configuration.
 *
 * @author Eugen Covaci
 */
@ToString(doNotUseGetters = true)
@Slf4j
@JsonPropertyOrder({"proxyType", "proxyHost", "proxyPort", "proxyUsername", "proxyPassword", "proxyStorePassword",
        "proxyPacFileLocation", "blacklistTimeout",
        "localPort", "proxyTestUrl", "autostart", "autodetect"})
@Component
@PropertySource(value = "file:./config/proxy.properties", ignoreResourceNotFound = true)
public class ProxyConfig {

    @Value("${app.version}")
    private String appVersion;

    @Setter
    @Value("${api.port:9999}")
    private Integer apiPort;

    /**
     * default admin:winfoom, base64 encoded
     */
    @Getter
    @Value("${api.userPassword:YWRtaW46d2luZm9vbQ==}")
    private String apiToken;


    @Setter
    @Value("${proxy.type:DIRECT}")
    private Type proxyType;

    @Setter
    @Value("${local.port:3129}")
    private Integer localPort;

    @Value("${proxy.http.host:}")
    private String proxyHttpHost;

    @Value("${proxy.socks5.host:}")
    private String proxySocks5Host;

    @Value("${proxy.socks4.host:}")
    private String proxySocks4Host;

    @Value("${proxy.http.port:0}")
    private Integer proxyHttpPort;

    @Value("${proxy.socks5.port:0}")
    private Integer proxySocks5Port;

    @Value("${proxy.socks4.port:0}")
    private Integer proxySocks4Port;

    @Setter
    @Value("${proxy.test.url:http://example.com}")
    private String proxyTestUrl;

    @Getter
    @Value("${proxy.socks5.username:#{null}}")
    private String proxySocks5Username;

    @Getter
    @Value("${proxy.socks5.password:#{null}}")
    private String proxySocks5Password;

    /**
     * DOMAIN\\username or username
     */
    @Getter
    @Value("${proxy.http.username:#{null}}")
    private String proxyHttpUsername;

    @Getter
    @Value("${proxy.http.password:#{null}}")
    private String proxyHttpPassword;

    @Setter
    @Value("${proxy.http.win.useCurrentCredentials:true}")
    private boolean useCurrentCredentials;

    @Setter
    @Value("${proxy.pac.fileLocation:#{null}}")
    private String proxyPacFileLocation;

    @Setter
    @Value("${blacklist.timeout:30}")// minutes
    private Integer blacklistTimeout;

    @Getter
    @Value("${proxy.pac.username:#{null}}")
    private String proxyPacUsername;

    @Getter
    @Value("${proxy.pac.password:#{null}}")
    private String proxyPacPassword;

    @Setter
    @Value("${pac.http.auth.protocol:#{null}}")
    private HttpAuthProtocol pacHttpAuthProtocol;

    @Setter
    @Value("${autostart:false}")
    private boolean autostart;

    @Setter
    @Value("${autodetect:false}")
    private boolean autodetect;

    @Setter
    @Value("${http.auth.protocol:#{null}}")
    private HttpAuthProtocol httpAuthProtocol;

    @Getter
    private Path tempDirectory;

    @PostConstruct
    public void init() throws IOException {
        log.info("Check config directory");
        File configFile = new File("./config/proxy.properties");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdir();
            configFile.createNewFile();
        }

        log.info("Check temp directory");
        tempDirectory = Paths.get("./out/temp");

        if (!Files.exists(tempDirectory)) {
            log.info("Create temp directory {}", tempDirectory);
            Files.createDirectories(tempDirectory);
        } else if (!Files.isDirectory(tempDirectory)) {
            throw new IllegalStateException(
                    String.format("The file [%s] should be a directory, not a regular file", tempDirectory));
        } else {
            log.info("Using temp directory {}", tempDirectory);
        }
    }

    public boolean isAutoDetectNeeded() {
        return autodetect ||
                ((proxyType.isHttp() || proxyType.isSocks()) && StringUtils.isEmpty(getProxyHost())) ||
                (proxyType.isPac() && StringUtils.isEmpty(proxyPacFileLocation));
    }

    public void validate() throws InvalidProxySettingsException {
        if (proxyType.isHttp() || proxyType.isSocks()) {
            if (StringUtils.isEmpty(getProxyHost())) {
                throw new InvalidProxySettingsException("Missing proxy host");
            }
            if (!HttpUtils.isValidPort(getProxyPort())) {
                throw new InvalidProxySettingsException("Invalid proxy port");
            }
            if (proxyType.isHttp() && !isHttpAuthAutoMode()) {
                if (httpAuthProtocol == null) {
                    throw new InvalidProxySettingsException("Missing HTTP proxy authentication protocol");
                }

                if (StringUtils.isEmpty(proxyHttpUsername)) {
                    throw new InvalidProxySettingsException("Missing proxy username");
                } else {
                    int backslashIndex = proxyHttpUsername.indexOf('\\');

                    // Check whether it begins or ends with '\' character
                    if (backslashIndex == 0 ||
                            backslashIndex == proxyHttpUsername.length() - 1) {
                        throw new InvalidProxySettingsException("The proxy username is invalid");
                    }
                }

                if (StringUtils.isEmpty(proxyHttpPassword)) {
                    throw new InvalidProxySettingsException("Missing proxy password");
                }

            }
        } else if (proxyType.isPac()) {
            if (StringUtils.isEmpty(proxyPacFileLocation)) {
                throw new InvalidProxySettingsException("Missing PAC file location");
            }
            if (!isPacAuthAutoMode() && !isPacAuthDisabledMode()) {
                int backslashIndex = proxyPacUsername.indexOf('\\');

                // Check whether it begins or ends with '\' character
                if (backslashIndex == 0 ||
                        backslashIndex == proxyPacUsername.length() - 1) {
                    throw new InvalidProxySettingsException("The proxy username is invalid");
                }

            }
        }
    }

    public boolean autoDetect() throws IOException {
        log.info("Detecting IE proxy settings");
        IEProxyConfig ieProxyConfig = WinHttpHelpers.readIEProxyConfig();
        log.info("IE settings {}", ieProxyConfig);
        if (ieProxyConfig != null) {
            String pacUrl = WinHttpHelpers.findPacFileLocation(ieProxyConfig);
            if (pacUrl != null) {
                log.info("Proxy Auto Config file location: {}", pacUrl);
                proxyType = Type.PAC;
                proxyPacFileLocation = pacUrl;
                return true;
            } else {// Manual case
                String proxySettings = ieProxyConfig.getProxy();
                log.info("Manual proxy settings: [{}]", proxySettings);
                if (proxySettings != null) {
                    if (proxySettings.indexOf('=') == -1) {
                        setProxy(Type.HTTP, proxySettings);
                        return true;
                    } else {
                        Properties properties = new Properties();
                        properties.load(
                                new ByteArrayInputStream(proxySettings.replace(';', '\n').
                                        getBytes(StandardCharsets.ISO_8859_1)));
                        String httpProxy = properties.getProperty("http");
                        if (httpProxy != null) {
                            setProxy(Type.HTTP, httpProxy);
                            return true;
                        } else {
                            String socksProxy = properties.getProperty("socks");
                            if (socksProxy != null) {
                                setProxy(Type.SOCKS5, socksProxy);
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            log.warn("Cannot retrieve IE settings");
        }
        return false;
    }

    private void setProxy(Type type, String proxy) {
        log.info("Set proxy type: {}, value: {}", type, proxy);
        proxyType = type;
        HttpHost httpHost = HttpHost.create(proxy);
        setProxyHost(httpHost.getHostName());
        setProxyPort(httpHost.getPort());
    }

    @JsonView(value = {Views.Settings.class})
    public String getAppVersion() {
        return appVersion;
    }

    @JsonView(value = {Views.Common.class})
    public Integer getLocalPort() {
        return localPort;
    }

    @JsonView(value = {Views.Http.class, Views.Socks4.class})
    public String getProxyHost() {
        return switch (proxyType) {
            case HTTP -> proxyHttpHost;
            case SOCKS4 -> proxySocks4Host;
            case SOCKS5 -> proxySocks5Host;
            default -> null;
        };
    }

    public void setProxyHost(String proxyHost) {
        switch (proxyType) {
            case HTTP:
                this.proxyHttpHost = proxyHost;
                break;
            case SOCKS4:
                this.proxySocks4Host = proxyHost;
                break;
            case SOCKS5:
                this.proxySocks5Host = proxyHost;
                break;
        }
    }

    @JsonView(value = {Views.Http.class, Views.Socks4.class})
    public Integer getProxyPort() {
        return switch (proxyType) {
            case HTTP -> proxyHttpPort;
            case SOCKS4 -> proxySocks4Port;
            case SOCKS5 -> proxySocks5Port;
            default -> 0;
        };
    }

    public void setProxyPort(Integer proxyPort) {
        switch (proxyType) {
            case HTTP:
                this.proxyHttpPort = proxyPort;
                break;
            case SOCKS4:
                this.proxySocks4Port = proxyPort;
                break;
            case SOCKS5:
                this.proxySocks5Port = proxyPort;
                break;
        }
    }

    @JsonView(value = {Views.Common.class})
    public String getProxyTestUrl() {
        return proxyTestUrl;
    }


    @JsonView(value = {Views.Common.class})
    public Type getProxyType() {
        return proxyType;
    }

    @JsonView(value = {Views.Socks5.class, Views.Pac.class, Views.HttpNonWindows.class, Views.HttpWindowsManual.class})
    public String getProxyUsername() {
        return switch (proxyType) {
            case HTTP -> proxyHttpUsername;
            case SOCKS5 -> proxySocks5Username;
            case PAC -> proxyPacUsername;
            default -> null;
        };
    }

    public void setProxyUsername(String proxyUsername) {
        switch (proxyType) {
            case HTTP:
                proxyHttpUsername = proxyUsername;
                break;
            case SOCKS5:
                proxySocks5Username = proxyUsername;
                break;
            case PAC:
                proxyPacUsername = proxyUsername;
                break;
        }
    }

    @Mask
    @JsonView(value = {Views.Socks5.class, Views.Pac.class, Views.HttpNonWindows.class, Views.HttpWindowsManual.class})
    public String getProxyPassword() {
        return switch (proxyType) {
            case HTTP -> proxyHttpPassword;
            case SOCKS5 -> proxySocks5Password;
            case PAC -> proxyPacPassword;
            default -> null;
        };
    }

    public void setProxyPassword(String proxyPassword) {
        switch (proxyType) {
            case HTTP:
                proxyHttpPassword = proxyPassword;
                break;
            case SOCKS5:
                proxySocks5Password = proxyPassword;
                break;
            case PAC:
                proxyPacPassword = proxyPassword;
                break;
        }
    }

    @JsonView(value = {Views.Pac.class})
    public HttpAuthProtocol getPacHttpAuthProtocol() {
        return pacHttpAuthProtocol;
    }

    @JsonView(value = {Views.HttpWindows.class})
    public boolean isUseCurrentCredentials() {
        return useCurrentCredentials;
    }

    public boolean isHttpAuthAutoMode() {
        return SystemConfig.IS_OS_WINDOWS
                && proxyType.isHttp()
                && useCurrentCredentials;
    }

    public boolean isPacAuthAutoMode() {
        return SystemConfig.IS_OS_WINDOWS
                && proxyType.isPac()
                && StringUtils.isEmpty(proxyPacUsername);
    }

    public boolean isPacAuthManualMode() {
        return proxyType.isPac() && !StringUtils.isEmpty(proxyPacUsername);
    }

    public boolean isAuthAutoMode() {
        return isHttpAuthAutoMode() || isPacAuthAutoMode();
    }

    public boolean isPacAuthDisabledMode() {
        return proxyType.isPac()
                && !SystemConfig.IS_OS_WINDOWS
                && StringUtils.isEmpty(proxyPacUsername);
    }

    @JsonView(value = {Views.Pac.class})
    public String getProxyPacFileLocation() {
        return proxyPacFileLocation;
    }

    @JsonView(value = {Views.Pac.class})
    public Integer getBlacklistTimeout() {
        return blacklistTimeout;
    }


    public URL getProxyPacFileLocationAsURL() throws MalformedURLException {
        if (StringUtils.isNotEmpty(proxyPacFileLocation)) {
            if (HttpUtils.containsSchema(proxyPacFileLocation)) {
                return new URL(proxyPacFileLocation);
            } else {
                return new URL("file:///" + proxyPacFileLocation);
            }
        }
        return null;
    }

    public boolean isAutoConfig() {
        return this.proxyType.isPac();
    }

    @JsonView(value = {Views.Settings.class})
    public boolean isAutostart() {
        return autostart;
    }

    @JsonView(value = {Views.WindowsSettings.class})
    public boolean isAutodetect() {
        return autodetect;
    }

    public boolean isNtlm() {
        return  !isAuthAutoMode() &&
                ((proxyType.isHttp() && httpAuthProtocol != null && httpAuthProtocol.isNtlm()) ||
                        (proxyType.isPac() && pacHttpAuthProtocol != null && pacHttpAuthProtocol.isNtlm()));
    }

    @JsonView(value = {Views.Settings.class})
    public Integer getApiPort() {
        return apiPort;
    }

    @JsonView(value = {Views.HttpNonWindows.class, Views.HttpWindowsManual.class})
    public HttpAuthProtocol getHttpAuthProtocol() {
        return httpAuthProtocol;
    }

    /**
     * Save the current settings to the home application directory, overwriting the existing values.
     *
     * @throws ConfigurationException
     */
    @PreDestroy
    void save() throws ConfigurationException {
        log.info("Save proxy settings");
        File userProperties = new File("./config/proxy.properties");
        FileBasedConfigurationBuilder<PropertiesConfiguration> propertiesBuilder = new Configurations()
                .propertiesBuilder(userProperties);
        Configuration config = propertiesBuilder.getConfiguration();
        setProperty(config, "app.version", appVersion);
        setProperty(config, "api.port", apiPort);
        setProperty(config, "proxy.type", proxyType);
        setProperty(config, "proxy.http.host", proxyHttpHost);
        setProperty(config, "proxy.http.port", proxyHttpPort);
        setProperty(config, "proxy.socks4.host", proxySocks4Host);
        setProperty(config, "proxy.socks4.port", proxySocks4Port);
        setProperty(config, "proxy.socks5.host", proxySocks5Host);
        setProperty(config, "proxy.socks5.port", proxySocks5Port);
        setProperty(config, "local.port", localPort);
        setProperty(config, "proxy.test.url", proxyTestUrl);
        setProperty(config, "proxy.http.username", proxyHttpUsername);
        setProperty(config, "proxy.http.win.useCurrentCredentials", useCurrentCredentials);
        setProperty(config, "proxy.socks5.username", proxySocks5Username);

        if (StringUtils.isNotEmpty(proxyHttpPassword)) {
            setProperty(config, "proxy.http.password", encode(Base64.getEncoder().encodeToString(proxyHttpPassword.getBytes())));
        } else {
            config.clearProperty("proxy.http.password");
        }

        if (StringUtils.isNotEmpty(proxySocks5Password)) {
            setProperty(config, "proxy.socks5.password", encode(Base64.getEncoder().encodeToString(proxySocks5Password.getBytes())));
        } else {
            config.clearProperty("proxy.socks5.password");
        }

        setProperty(config, "proxy.pac.fileLocation", proxyPacFileLocation);
        setProperty(config, "proxy.pac.username", proxyPacUsername);
        if (StringUtils.isNotEmpty(proxyPacPassword)) {
            setProperty(config, "proxy.pac.password", encode(Base64.getEncoder().encodeToString(proxyPacPassword.getBytes())));
        } else {
            config.clearProperty("proxy.pac.password");
        }
        setProperty(config, "pac.http.auth.protocol", pacHttpAuthProtocol);
        setProperty(config, "blacklist.timeout", blacklistTimeout);

        setProperty(config, "http.auth.protocol", httpAuthProtocol);
        setProperty(config, "autostart", autostart);
        setProperty(config, "autodetect", autodetect);
        propertiesBuilder.save();
    }

    private static String encode(String value) {
        return "encoded(" + value + ")";
    }

    private void setProperty(final Configuration config, final String key, final Object value) {
        if (value != null &&
                (!(value instanceof String) ||
                        StringUtils.isNotEmpty((String) value))) {
            config.setProperty(key, value);
        } else {
            config.clearProperty(key);
        }
    }


    public enum Type implements ProxyType {
        HTTP, SOCKS4, SOCKS5, PAC, DIRECT;

        public boolean isPac() {
            return this == PAC;
        }

        @Override
        public boolean isSocks4() {
            return this == SOCKS4;
        }

        @Override
        public boolean isSocks5() {
            return this == SOCKS5;
        }

        @Override
        public boolean isHttp() {
            return this == HTTP;
        }

        @Override
        public boolean isDirect() {
            return this == DIRECT;
        }

    }

    public enum HttpAuthProtocol {
        NTLM, BASIC;

        public boolean isNtlm() {
            return this == NTLM;
        }

        public boolean isBasic() {
            return this == BASIC;
        }

    }
}
