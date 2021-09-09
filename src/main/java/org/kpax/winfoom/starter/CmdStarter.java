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

package org.kpax.winfoom.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.proxy.ProxyController;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Profile("!gui")
@Component
public class CmdStarter implements ApplicationListener<ApplicationReadyEvent> {

    private final ProxyConfig proxyConfig;

    private final ProxyController proxyController;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (proxyConfig.isAutodetect()) {
            try {
                boolean result = proxyConfig.autoDetect();
                if (!result) {
                    log.warn("Failed to retrieve Internet Explorer network settings:");
                }
            } catch (Exception e) {
                log.error("Error on retrieving Internet Explorer network settings:", e);
            }
        }
        if (proxyConfig.isAutostart()) {
            try {
                proxyConfig.validate();
                proxyController.start();
                log.info("The local proxy server has been started");
            } catch (InvalidProxySettingsException e) {
                log.warn("Cannot start local proxy server, invalid proxy configuration", e);
            } catch (Exception e) {
                log.error("The local proxy server failed to start", e);
            }
        }
        log.info("Application is ready");
    }
}