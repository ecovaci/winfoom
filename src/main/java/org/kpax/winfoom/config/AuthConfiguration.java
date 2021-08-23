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

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.auth.win.WindowsCredentialsProvider;
import org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNegotiateSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.kpax.winfoom.proxy.ManualAuthCredentialsProvider;
import org.kpax.winfoom.util.functional.ProxySingletonSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 1/24/2020
 */
@Configuration
class AuthConfiguration {

    /**
     * Create the system wide {@link CredentialsProvider}.
     * <p>Note: Only works with HTTP proxies.
     *
     * @return the system wide {@link CredentialsProvider}
     */
    @Bean
    public ProxySingletonSupplier<CredentialsProvider> credentialsProviderSupplier(ProxyConfig proxyConfig) {
        return new ProxySingletonSupplier<>(
                () -> proxyConfig.isAuthAutoMode() ?
                        new WindowsCredentialsProvider(new SystemDefaultCredentialsProvider()) :
                        proxyConfig.isPacAuthDisabledMode() ?
                                new BasicCredentialsProvider() : new ManualAuthCredentialsProvider(proxyConfig));
    }

    /**
     * Create the system wide {@link Registry<AuthSchemeProvider>}.
     * <p>Note: Only works with HTTP proxies.
     *
     * @return the system wide {@link Registry<AuthSchemeProvider>}
     */
    @Bean
    public ProxySingletonSupplier<Registry<AuthSchemeProvider>> authSchemeRegistrySupplier(ProxyConfig proxyConfig) {
        return new ProxySingletonSupplier<>(() -> {
            RegistryBuilder<AuthSchemeProvider> registryBuilder = RegistryBuilder.<AuthSchemeProvider>create()
                    .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                    .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                    .register(AuthSchemes.NTLM, proxyConfig.isAuthAutoMode() ?
                            new WindowsNTLMSchemeFactory(null) : new NTLMSchemeFactory())
                    .register(AuthSchemes.SPNEGO, proxyConfig.isAuthAutoMode() ?
                            new WindowsNegotiateSchemeFactory(null) : new SPNegoSchemeFactory());
            if (!proxyConfig.isAuthAutoMode()) {
                registryBuilder.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory());
            }
            return registryBuilder.build();
        });
    }

}
