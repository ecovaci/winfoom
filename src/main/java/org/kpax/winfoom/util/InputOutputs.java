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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.config.SystemConfig;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Eugen Covaci
 */
@Slf4j
public final class InputOutputs {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private InputOutputs() {
    }

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
            logger.debug("Close {}", closeable.getClass());
            try {
                closeable.close();
            } catch (Exception e) {
                logger.debug("Fail to close: " + closeable.getClass().getName(), e);
            }
        }

    }

    public static void close(@NotNull final Socket socket) {
        logger.debug("Close socket");
        try {
            socket.shutdownInput();
        } catch (Exception e) {
            logger.debug("Fail to shutdown socket input", e);
        }

        try {
            socket.shutdownOutput();
        } catch (Exception e) {
            logger.debug("Fail to shutdown socket output", e);
        }

        try {
            socket.close();
        } catch (Exception e) {
            logger.debug("Fail to close socket", e);
        }
    }

    public static String generateCacheFilename() {
        return System.nanoTime() +
                "-" +
                (int) (Math.random() * 100);
    }


    public static boolean isIncluded(final Properties who, final Properties where) {
        Assert.notNull(who, "who cannot be null");
        Assert.notNull(where, "where cannot be null");
        for (String key : who.stringPropertyNames()) {
            if (where.getProperty(key) == null) {
                return false;
            }
        }
        return true;
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

    /**
     * Move the file designated by a {@link Path} to a backup location.
     *
     * @param path        the file's {@link Path}
     * @param withWarning if {@code true} an warning will popup
     * @param options     the moving {@link CopyOption}s
     * @return the new {@link Path} or {@code null} if the original file does not exist
     * @throws IOException
     */
    public static Path backupFile(final Path path,
                                  final boolean withWarning,
                                  final CopyOption... options) throws IOException {
        Assert.notNull(path, "path cannot be null");
        if (Files.exists(path)) {
            Path appHomePath = Paths.get(System.getProperty(SystemConfig.WINFOOM_CONFIG_ENV), SystemConfig.APP_HOME_DIR_NAME);
            Path backupDirPath = appHomePath.resolve(SystemConfig.BACKUP_DIR_NAME);
            if (!Files.exists(backupDirPath)) {
                Files.createDirectories(backupDirPath);
            }
            if (withWarning) {
                SwingUtils.showWarningMessage(
                        String.format("The %s file found belongs to a different application version<br>" +
                                        "and is not compatible with the current version!<br>" +
                                        "The existent one will be moved to:<br>" +
                                        "%s directory.",
                                path.getFileName(),
                                backupDirPath.toString()));
            }
            logger.info("Move the file {} to: {} directory", path, backupDirPath);
            return Files.move(path, backupDirPath.resolve(path.getFileName()), options);
        }
        logger.info("Cannot move file {} because it does not exist", path);
        return null;
    }

}
