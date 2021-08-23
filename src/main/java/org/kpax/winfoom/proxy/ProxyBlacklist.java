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
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * It implements the proxy blacklisting mechanism.
 * <p>If a proxy doesn't respond to a connect attempt, it can be blacklisted
 * which means it will not be used again until the blacklist timeout happens.
 */
@Slf4j
@ThreadSafe
@Component
public class ProxyBlacklist implements StopListener {

    /**
     * Key = the blacklisted ProxyInfo<br>
     * Value = the blacklist timeout Instant
     */
    private final Map<ProxyInfo, Instant> blacklistMap = new ConcurrentHashMap<>();

    /**
     * The temporal unit for measuring the timeout period.
     */
    public static final ChronoUnit TEMPORAL_UNIT = ChronoUnit.MINUTES;

    @Autowired
    private ProxyConfig proxyConfig;

    /**
     * Attempt to blacklist a proxy. Does nothing if the blacklisting is disabled.<br>
     * If the proxy is already blacklisted, it keeps the existent timeout.
     *
     * @param proxyInfo the proxy to be blacklisted.
     * @return the blacklist timeout {@link Instant} iff the blacklisting is enabled, {@code null} otherwise.
     */
    public Instant blacklist(@NotNull final ProxyInfo proxyInfo) {
        logger.debug("Attempt to blacklist proxy {}", proxyInfo);
        if (proxyConfig.getBlacklistTimeout() < 1) {
            logger.debug("Blacklisting is disabled, nothing to do");
            return null;
        }
        return blacklistMap.compute(proxyInfo, (key, value) -> {
            Instant now = Instant.now();
            if (value == null || value.isBefore(now)) {
                Instant timeoutInstant = now.plus(proxyConfig.getBlacklistTimeout(),
                        TEMPORAL_UNIT);
                logger.debug("Blacklisted until {}", timeoutInstant);
                return timeoutInstant;
            } else {
                logger.debug("Already blacklisted until {}", value);
                return value;
            }
        });
    }

    /**
     * Verify whether a proxy is blacklisted.<br>
     * If the proxy is in the blacklist map but expired, will be removed.
     *
     * @param proxyInfo the proxy to be checked
     * @return {@code true} iff the proxy is blacklisted
     */
    public boolean isBlacklisted(@NotNull final ProxyInfo proxyInfo) {
        if (proxyConfig.getBlacklistTimeout() < 1) {
            return false;
        }
        Instant timeoutInstant = blacklistMap.computeIfPresent(
                proxyInfo, (key, value) -> value.isBefore(Instant.now()) ? null : value);
        return timeoutInstant != null;
    }

    public boolean isActive(@NotNull final ProxyInfo proxyInfo) {
        return !isBlacklisted(proxyInfo);
    }

    /**
     * Clear the blacklist map.
     *
     * @return the number of currently blacklisted proxies.
     */
    public int clear() {
        long count = blacklistMap.keySet().stream().filter(this::isBlacklisted).count();
        blacklistMap.clear();
        return (int) count;
    }

    public Map<ProxyInfo, Instant> getBlacklistMap() {
        return Collections.unmodifiableMap(blacklistMap);
    }

    public boolean isBlacklistingEnabled() {
        return proxyConfig.getBlacklistTimeout() > 0;
    }

    /**
     * @return a map containing the currently blacklisted proxies.
     */
    public Map<ProxyInfo, Instant> getActiveBlacklistMap() {
        Instant now = Instant.now();
        return Collections.unmodifiableMap(
                blacklistMap.entrySet().stream().
                        filter(e -> !e.getValue().isBefore(now)).
                        collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public void onStop() {
        logger.debug("Clear the blacklist");
        blacklistMap.clear();
    }
}
