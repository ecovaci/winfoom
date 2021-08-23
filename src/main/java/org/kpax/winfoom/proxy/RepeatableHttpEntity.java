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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.kpax.winfoom.annotation.NotThreadSafe;
import org.kpax.winfoom.config.SystemContext;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A special type of repeatable {@link AbstractHttpEntity}.
 * <p>The data is cached either internally or externally into a temporary file.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/6/2020
 */
@NotThreadSafe
public class RepeatableHttpEntity extends AbstractHttpEntity implements Closeable {

    private final SessionInputBufferImpl inputBuffer;

    /**
     * The directory path containing the temporary files.
     */
    private final Path tempDirectory;

    /**
     * The value of Content-Length header.
     */
    private final long contentLength;

    /**
     * The max amount of  internally cached bytes.
     */
    private final int internalBufferLength;

    /**
     * Write into this buffer when contentLength < maximum buffered.
     */
    private byte[] bufferedBytes;

    /**
     * The temporary file containing the cached data.
     */
    private Path tempFilepath;

    /**
     * Whether it reads from {@link SessionInputBufferImpl} or from the temp cache file.
     */
    private boolean streaming = true;

    public RepeatableHttpEntity(final HttpRequest request,
                                final SessionInputBufferImpl inputBuffer,
                                final Path tempDirectory,
                                final int internalBufferLength) {
        this.inputBuffer = inputBuffer;
        this.tempDirectory = tempDirectory;
        this.contentType = request.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        this.contentEncoding = request.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
        this.contentLength = HttpUtils.getContentLength(request);
        this.internalBufferLength = internalBufferLength;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        if (contentLength == 0) {
            return new ByteArrayInputStream(new byte[0]);
        } else {
            if (streaming) {
                return new InputStream() {
                    @Override
                    public int read() {
                        throw new UnsupportedOperationException("Do not use it");
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        return inputBuffer.read(b, off, len);
                    }
                };
            } else {
                if (bufferedBytes != null) {
                    return new ByteArrayInputStream(bufferedBytes);
                } else {
                    return Files.newInputStream(tempFilepath);
                }
            }
        }
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        if (streaming) {
            if (this.contentLength > 0 && this.contentLength <= internalBufferLength) {
                int length;
                final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                long remaining = contentLength;
                while (remaining > 0 && InputOutputs.isAvailable(inputBuffer)) {
                    length = inputBuffer.read(buffer, 0, (int) Math.min(OUTPUT_BUFFER_SIZE, remaining));
                    if (length == -1) {
                        break;
                    }
                    outStream.write(buffer, 0, length);
                    outStream.flush();

                    out.write(buffer, 0, length);
                    remaining -= length;
                }
                out.flush();
                bufferedBytes = out.toByteArray();
            } else if (contentLength != 0) {
                tempFilepath = tempDirectory.resolve(InputOutputs.generateCacheFilename());
                final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
                try (CacheFile cacheFile = CacheFile.from(tempFilepath, buffer)) {
                    if (contentLength < 0) {
                        if (isChunked()) {
                            ChunkedInputStream chunkedInputStream = new ChunkedInputStream(inputBuffer);
                            int length;
                            while ((length = chunkedInputStream.read(buffer)) > 0) {
                                outStream.write(buffer, 0, length);
                                outStream.flush();

                                // Write to file
                                cacheFile.write(length);
                            }
                        } else {

                            // consume until EOF
                            int length;
                            while (InputOutputs.isAvailable(inputBuffer)) {
                                length = inputBuffer.read(buffer);
                                if (length == -1) {
                                    break;
                                }
                                outStream.write(buffer, 0, length);
                                outStream.flush();

                                // Write to file
                                cacheFile.write(length);
                            }
                        }

                    } else {
                        int length;
                        long remaining = contentLength;

                        // consume no more than maxLength
                        while (remaining > 0 && InputOutputs.isAvailable(inputBuffer)) {
                            length = inputBuffer.read(buffer, 0, (int) Math.min(OUTPUT_BUFFER_SIZE, remaining));
                            if (length == -1) {
                                break;
                            }
                            outStream.write(buffer, 0, length);
                            outStream.flush();
                            remaining -= length;

                            // Write to temp file
                            cacheFile.write(length);
                        }
                    }
                }
            }
            streaming = false;
        } else {
            if (bufferedBytes != null) {
                outStream.write(bufferedBytes);
                outStream.flush();
            } else {
                //read from file
                try (InputStream inputStream = Files.newInputStream(tempFilepath)) {
                    inputStream.transferTo(outStream);
                    outStream.flush();
                }
            }
        }
    }

    @Override
    public boolean isStreaming() {
        return streaming;
    }

    @Override
    public void close() throws IOException {
        // Delete the temp file if exists
        if (tempFilepath != null) {
            Files.deleteIfExists(tempFilepath);
        }
    }

    @NotThreadSafe
    private static class WindowsCacheFile implements CacheFile {

        private final ByteBuffer byteBuffer;
        private final AsynchronousFileChannel fileChannel;
        private long position = 0;

        private WindowsCacheFile(final Path tempFilepath, final byte[] buffer) throws IOException {
            this.byteBuffer = ByteBuffer.wrap(buffer);
            this.fileChannel = AsynchronousFileChannel.open(tempFilepath,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
        }

        @Override
        public void write(int length) throws IOException {
            fileChannel.write(byteBuffer.position(0).limit(length), position);
            position += length;
        }

        @Override
        public void close() throws IOException {
            fileChannel.close();
        }
    }

    @NotThreadSafe
    private static class SimpleCacheFile implements CacheFile {

        private final byte[] buffer;
        private final BufferedOutputStream outputStream;

        private SimpleCacheFile(final Path tempFilepath, final byte[] buffer) throws FileNotFoundException {
            this.buffer = buffer;
            this.outputStream = new BufferedOutputStream(new FileOutputStream(tempFilepath.toFile()));
        }

        @Override
        public void write(int length) throws IOException {
            outputStream.write(buffer, 0, length);
        }

        @Override
        public void close() throws IOException {
            this.outputStream.close();
        }
    }

    private interface CacheFile extends Closeable {
        void write(int length) throws IOException;

        static CacheFile from(final Path tempFilepath, final byte[] buffer) throws IOException {
            return SystemContext.IS_OS_WINDOWS ?
                    new WindowsCacheFile(tempFilepath, buffer) : new SimpleCacheFile(tempFilepath, buffer);
        }
    }

}
