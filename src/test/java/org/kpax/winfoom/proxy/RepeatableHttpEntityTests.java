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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class RepeatableHttpEntityTests {

    private Path tempDirectory;

    @Mock
    private HttpRequest httpRequest;

    @BeforeAll
    void init() {
        tempDirectory = Paths.get(".", "target", "test-output");
        File tmpFile = tempDirectory.toFile();
        if (!tmpFile.exists()) {
            tmpFile.mkdir();
        }
    }

    @Test
    void writeTo_bufferLessThanContentLength_UseTempFile() throws IOException {
        final String content = "12345";
        final int bufferSize = content.getBytes().length - 1;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(content.getBytes()));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.getBytes().length)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(content, outputStream.toString());
            assertEquals(1, tempDirectory.toFile().listFiles().length);
        }
    }

    @Test
    void writeTo_bufferEqualsContentLength_Buffering() throws IOException {
        final String content = "12345";
        final int bufferSize = content.getBytes().length;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(content.getBytes()));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.getBytes().length)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(content, outputStream.toString());
            assertEquals(0, tempDirectory.toFile().listFiles().length);
        }
    }

    @Test
    void writeTo_bufferBiggerThanContentLength_Buffering() throws IOException {
        final String content = "12345";
        final int bufferSize = content.getBytes().length + 1;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(content.getBytes()));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.getBytes().length)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(content, outputStream.toString());
            assertEquals(0, tempDirectory.toFile().listFiles().length);
        }
    }

    @Test
    void writeTo_negativeContentLengthBufferBiggerThanRealContentLength_UseTempFile() throws IOException {
        final String content = "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque" +
                " laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto " +
                "beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit " +
                "aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro " +
                "quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam " +
                "eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima " +
                "veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi" +
                " consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae" +
                " consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?";
        final int bufferSize = content.getBytes().length + 1;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(content.getBytes()));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(-1)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(content, outputStream.toString());
            assertEquals(1, tempDirectory.toFile().listFiles().length);
        }
    }

    @Test
    void writeTo_negativeContentLengthBufferLessThanContentLength_useTempFile() throws IOException {
        final String content = "12345";
        final int bufferSize = content.getBytes().length - 1;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(content.getBytes()));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(-1)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(content, outputStream.toString());
            assertEquals(1, tempDirectory.toFile().listFiles().length);
        }
    }

    @Test
    void writeTo_noAvailableData_doNotUseTempFile() throws IOException {
        final int bufferSize = 1024;

        SessionInputBufferImpl inputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        inputBuffer.bind(new ByteArrayInputStream(new byte[0]));

        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(0)));
        when(httpRequest.getFirstHeader(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        try (RepeatableHttpEntity repeatableHttpEntity = new RepeatableHttpEntity(httpRequest, inputBuffer, tempDirectory, bufferSize)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(repeatableHttpEntity.isStreaming());
            repeatableHttpEntity.writeTo(outputStream);
            assertFalse(repeatableHttpEntity.isStreaming());
            assertFalse(repeatableHttpEntity.isChunked());
            assertEquals(0, tempDirectory.toFile().listFiles().length);
        }
    }

    @AfterAll
    void after() {
        InputOutputs.deleteFile(tempDirectory.toFile());
    }

}
