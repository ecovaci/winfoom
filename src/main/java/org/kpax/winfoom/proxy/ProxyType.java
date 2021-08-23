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

/**
 * The contract of any proxy type.
 */
public interface ProxyType {

    /**
     * @return {@code true} iff the proxy type is SOCKS4.
     */
    boolean isSocks4();

    /**
     * @return {@code true} iff the proxy type is SOCKS5.
     */
    boolean isSocks5();

    /**
     * @return {@code true} iff the proxy type is SOCKS (version 4 or 5).
     */
    default boolean isSocks() {
        return isSocks4() || isSocks5();
    }

    /**
     * @return {@code true} iff the proxy type is HTTP.
     */
    boolean isHttp();

    /**
     * @return {@code true} iff there is no proxy.
     */
    boolean isDirect();

}
