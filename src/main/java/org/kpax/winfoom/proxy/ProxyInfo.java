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

import lombok.Getter;
import org.apache.http.HttpHost;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Encapsulate the proxy information: type, host an port
 */
@Getter
@ThreadSafe
public final class ProxyInfo {

    private final ProxyType type;

    private final HttpHost proxyHost;

    public ProxyInfo(PacType type) {
        this(type, null);
    }

    public ProxyInfo(ProxyType type, HttpHost proxyHost) {
        Assert.notNull(type, "type cannot be null");
        this.type = type;
        this.proxyHost = proxyHost;
    }

    @Override
    public String toString() {
        return "ProxyInfo{" +
                "type=" + type +
                ", host=" + proxyHost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyInfo proxyInfo = (ProxyInfo) o;
        return type == proxyInfo.type &&
                Objects.equals(proxyHost != null ? proxyHost.toHostString() : null,
                        proxyInfo.proxyHost != null ? proxyInfo.proxyHost.toHostString() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, proxyHost != null ? proxyHost.toHostString() : null);
    }

    /**
     * The proxy types as they are present in a PAC script file.
     */
    public enum PacType implements ProxyType {
        PROXY, HTTP, HTTPS, SOCKS, SOCKS4, SOCKS5, DIRECT;

        @Override
        public boolean isSocks4() {
            return this == PacType.SOCKS4;
        }

        @Override
        public boolean isSocks5() {
            return this == PacType.SOCKS5 || this == PacType.SOCKS;
        }

        @Override
        public boolean isHttp() {
            return this == PacType.HTTP || this == PacType.HTTPS || this == PacType.PROXY;
        }

        @Override
        public boolean isDirect() {
            return this == PacType.DIRECT;
        }

    }
}
