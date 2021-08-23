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

import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.annotation.TypeQualifier;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.pac.net.IpAddresses;
import org.kpax.winfoom.proxy.listener.StartListener;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.util.DomainUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provide methods to begin, end proxy session.
 *
 * @author Eugen Covaci
 */
@Slf4j
@ThreadSafe
@Component
public class ProxyController {

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private LocalProxyServer localProxyServer;

    /**
     * Whether the proxy session is started or not.
     */
    private volatile boolean started;

    /**
     * Begin a proxy session.
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        Assert.state(!started, "Already started");
        logger.debug("Attempting to start local proxy facade with: {}", proxyConfig);
        List<StartListener> startListeners = Stream.of(applicationContext.getBeanNamesForType(StartListener.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(Objects::nonNull).
                sorted(AnnotationAwareOrderComparator.INSTANCE).
                map(b -> (StartListener) b).
                collect(Collectors.toList());
        try {
            for (StartListener startListener : startListeners) {
                TypeQualifier typeQualifier = startListener.getClass().getMethod("onStart").
                        getDeclaredAnnotation(TypeQualifier.class);
                if (typeQualifier == null || typeQualifier.value() == proxyConfig.getProxyType()) {
                    logger.debug("Call onStart for: {}", startListener.getClass());
                    startListener.onStart();
                } else {
                    logger.debug("onStart ignored for {}", startListener.getClass());
                }
            }
        } catch (Exception e) {
            resetState();
            throw e;
        }
        if (proxyConfig.getProxyType().isSocks5() || proxyConfig.isPacAuthManualMode()) {
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    String proxyPassword = proxyConfig.getProxyPassword();
                    return (new PasswordAuthentication(DomainUser.extractUsername(proxyConfig.getProxyUsername()),
                            proxyPassword != null ? proxyPassword.toCharArray() : new char[0]));
                }
            });
        }
        localProxyServer.start();
        started = true;
    }

    /**
     * End the proxy session.
     */
    public synchronized void stop() {
        if (started) {
            started = false;
            resetState();
        } else {
            logger.info("Already stopped, nothing to do");
        }
    }

    /**
     * Reset the Spring beans state.
     * <p>Also, it removes the {@link Authenticator}, if any.
     */
    private void resetState() {
        callStopListeners();

        // We reset these suppliers because the network state
        // might have changed during the proxy session.
        // Though unlikely, we take no chances.
        IpAddresses.allPrimaryAddresses.reset();
        IpAddresses.primaryIPv4Address.reset();

        // Remove auth for SOCKS proxy
        if (proxyConfig.getProxyType().isSocks5() || proxyConfig.isPacAuthManualMode()) {
            Authenticator.setDefault(null);
        }
    }

    void callStopListeners() {
        logger.debug("Call all StopListener.afterStop singletons");
        Stream.of(applicationContext.getBeanNamesForType(StopListener.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(Objects::nonNull).sorted(AnnotationAwareOrderComparator.INSTANCE).
                map(b -> (StopListener) b).forEach(StopListener::onStop);
    }

    void restart() throws Exception {
        stop();
        start();
    }

    public boolean isRunning() {
        return started;
    }


    public boolean isStopped() {
        return !started;
    }

}
