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

package org.kpax.winfoom.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * @author Eugen Covaci
 */
@Slf4j
@UtilityClass
public final class InputOutputs {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Check for available data.
     *
     * @param inputBuffer The input buffer.
     * @return <code>false</code> iff EOF has been reached.
     */
    public static boolean isAvailable(@NotNull final SessionInputBufferImpl inputBuffer)
            throws IOException {
        return inputBuffer.hasBufferedData() || inputBuffer.fillBuffer() > -1;
    }

    /**
     * Close an <code>AutoCloseable</code>, debug the possible error.
     *
     * @param closeable The {@link AutoCloseable} instance.
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            log.debug("Close {}", closeable.getClass());
            try {
                closeable.close();
            } catch (Exception e) {
                log.debug("Fail to close: " + closeable.getClass().getName(), e);
            }
        }

    }

    public static void close(@NotNull final Socket socket) {
        log.debug("Close socket");
        try {
            socket.shutdownInput();
        } catch (Exception e) {
            log.debug("Fail to shutdown socket input", e);
        }

        try {
            socket.shutdownOutput();
        } catch (Exception e) {
            log.debug("Fail to shutdown socket output", e);
        }

        try {
            socket.close();
        } catch (Exception e) {
            log.debug("Fail to close socket", e);
        }
    }

    public static String generateCacheFilename() {
        return System.nanoTime() +
                "-" +
                (int) (Math.random() * 100);
    }

    /**
     * If it is a regular file, delete it. If it is a directory, delete recursively.
     *
     * @param file the regular file or directory to be deleted.
     * @return {@code true} if the deletion takes place.
     */
    public static boolean deleteFile(@NotNull final File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteFile(f);
            }
        }
        return file.delete();
    }

    /**
     * Delete the directory's content.
     *
     * @param directory the {@link File} to be emptied
     * @return {@code true} iff all the contained files were deleted.
     */
    public static boolean emptyDirectory(@NotNull final File directory) {
        Assert.isTrue(directory.isDirectory(), "Not a directory");
        File[] files = directory.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            deleteFile(file);
        }
        return files.length == 0;
    }

}
