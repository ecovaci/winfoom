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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select the appropriate {@link ClientConnectionProcessor} to process a {@link org.kpax.winfoom.proxy.ClientConnection}.
 */
@RequiredArgsConstructor
@Component
public class ConnectionProcessorSelector {

    private final HttpConnectClientConnectionProcessor httpConnectClientConnectionProcessor;

    private final SocksConnectClientConnectionProcessor socksConnectClientConnectionProcessor;

    private final NonConnectClientConnectionProcessor nonConnectClientConnectionProcessor;

    private final SocksNonConnectClientConnectionProcessor socksNonConnectClientConnectionProcessor;

    public ClientConnectionProcessor select(boolean isConnect, boolean isHttp) {
        if (isConnect) {
            if (isHttp) {
                return httpConnectClientConnectionProcessor;
            } else {
                return socksConnectClientConnectionProcessor;
            }
        } else {
            if (isHttp) {
                return nonConnectClientConnectionProcessor;
            } else {
                return socksNonConnectClientConnectionProcessor;
            }
        }
    }

}
