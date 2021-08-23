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

import org.apache.http.protocol.HttpContext;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.util.HttpUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

/**
 * A specialized version of {@link SocksConnectionSocketFactory} for SOCKS4 proxy.
 */
@ThreadSafe
@Component
class Socks4ConnectionSocketFactory extends SocksConnectionSocketFactory {

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        Socket socket = super.createSocket(context);
        try {
            HttpUtils.setSocks4(socket);
        } catch (UnsupportedOperationException e) {
            throw new IOException(e);
        }
        return socket;
    }

}