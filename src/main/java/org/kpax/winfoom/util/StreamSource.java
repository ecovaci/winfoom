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

package org.kpax.winfoom.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.annotation.ThreadSafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@ThreadSafe
public interface StreamSource {

    @NotNull
    InputStream getInputStream();

    @NotNull
    OutputStream getOutputStream();

    static StreamSource from(@NotNull final InputStream inputStream, @NotNull final OutputStream outputStream) {
        return new DefaultStreamSource(inputStream, outputStream);
    }

    static StreamSource from(@NotNull final Socket socket) throws IOException {
        return new DefaultStreamSource(socket.getInputStream(), socket.getOutputStream());
    }

    @Getter
    @RequiredArgsConstructor
    class DefaultStreamSource implements StreamSource {
        private final InputStream inputStream;
        private final OutputStream outputStream;
    }

}
