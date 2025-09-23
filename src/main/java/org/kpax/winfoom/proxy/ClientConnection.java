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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.annotation.NotThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.CrlfConverter;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.kpax.winfoom.util.StreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * It encapsulates a client's connection.
 * <p>It provides various methods for exchanging data with the client,
 * also some information about the state of the request's processing.
 * <p><b>Note:</b> This class doesn't have the responsibility to close the underlying socket.
 *
 * @author Eugen Covaci
 */
@Slf4j
@NotThreadSafe
public class ClientConnection implements StreamSource, AutoCloseable {

    /**
     * The underlying socket.
     */
    private final Socket socket;

    private final ProxyConfig proxyConfig;

    private final SystemConfig systemConfig;

    /**
     * The socket's input stream.
     */
    private final InputStream inputStream;

    /**
     * The socket's output stream.
     */
    private final OutputStream outputStream;

    /**
     * Used for request parsing also by the {@link org.kpax.winfoom.proxy.RepeatableHttpEntity} class.
     */
    private final SessionInputBufferImpl sessionInputBuffer;

    /**
     * The parsed {@link HttpRequest}.
     */
    private final HttpRequest request;

    /**
     * The request URI extracted from the request line.
     */
    private final URI requestUri;

    /**
     * Whether the request method is CONNECT or not.
     */
    @Getter
    private final boolean connect;

    /**
     * Constructor.<br>
     * Has the responsibility of parsing the request and initiate various objects.
     * <p><b>The response should be committed before throwing any exception.</b></p>
     *
     * @param socket
     * @param proxyConfig
     * @param systemConfig
     * @throws IOException
     * @throws HttpException
     */
    ClientConnection(final Socket socket,
                     final ProxyConfig proxyConfig,
                     final SystemConfig systemConfig) throws IOException, HttpException {
        this.socket = socket;
        this.proxyConfig = proxyConfig;
        this.systemConfig = systemConfig;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();

        // Parse the request
        try {
            this.sessionInputBuffer = new SessionInputBufferImpl(
                    new HttpTransportMetricsImpl(),
                    InputOutputs.DEFAULT_BUFFER_SIZE,
                    InputOutputs.DEFAULT_BUFFER_SIZE,
                    MessageConstraints.DEFAULT,
                    StandardCharsets.UTF_8.newDecoder());
            this.sessionInputBuffer.bind(this.inputStream);
            this.request = new DefaultHttpRequestParser(this.sessionInputBuffer).parse();
            this.connect = HttpUtils.HTTP_CONNECT.equals(request.getRequestLine().getMethod().toUpperCase(Locale.ROOT));
            try {
                this.requestUri = HttpUtils.parseRequestUri(this.connect, this.request.getRequestLine().getUri());
            } catch (URISyntaxException e) {
                throw new HttpException("Invalid request uri", e);
            }
        } catch (Exception e) {
            if (e instanceof HttpException) {
                // Most likely a bad request
                // even though might not always be the case
                writeErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            } else {
                writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            throw e;
        }

        if (log.isDebugEnabled()) {
            for (Header header : request.getAllHeaders()) {
                log.debug("<<< Request header: {}", header);
            }
        }
    }

    /**
     * @return the input stream of the client's socket
     */
    @NotNull
    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the output stream of the client's socket
     */
    @NotNull
    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @return the session input buffer used to parse the request into a {@link HttpRequest} instance
     */
    @NotNull
    public SessionInputBufferImpl getSessionInputBuffer() {
        return sessionInputBuffer;
    }

    /**
     * @return the HTTP request.
     */
    @NotNull
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * @return the request URI extracted from the request line.
     */
    @NotNull
    public URI getRequestUri() {
        return requestUri;
    }

    /**
     * Write an object to the output stream using CRLF format.
     *
     * @param obj the object
     * @throws IOException
     */
    public void write(@NotNull Object obj) throws IOException {
        outputStream.write(CrlfConverter.convert(obj));
    }

    /**
     * Write an empty line to the output stream using CRLF format.
     *
     * @throws IOException
     */
    public void writeln() throws IOException {
        outputStream.write(CrlfConverter.CRLF.getBytes());
    }

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     * <p><b>This method commits the response.</b></p>
     *
     * @param statusCode the request's status code.
     */
    public void writeErrorResponse(int statusCode) {
        writeErrorResponse(statusCode, null);
    }

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     * <p><b>This method commits the response.</b></p>
     *
     * @param statusCode   the request's status code.
     * @param reasonPhrase the request's reason code
     */
    public void writeErrorResponse(int statusCode, String reasonPhrase) {
        log.debug("Write error response: statusCode = {}  reasonPhrase = [{}]", statusCode, reasonPhrase);
        try {
            write(HttpUtils.toStatusLine(request != null ? request.getProtocolVersion() : HttpVersion.HTTP_1_1,
                    statusCode, HttpUtils.replaceCRAndLF(reasonPhrase, StringUtils.SPACE)));
            write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER, HttpUtils.getCurrentDate()));
            writeln();
        } catch (Exception ex) {
            log.debug("Error on writing error response", ex);
        }
    }

    public void writeBadGatewayResponse(String reasonPhrase) {
        writeErrorResponse(HttpStatus.SC_BAD_GATEWAY, reasonPhrase);
    }

    public void writeProxyAuthRequiredErrorResponse() {
        log.debug("Write error response: statusCode = {}", HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED);
        String body = """
                <!DOCTYPE HTML "-//IETF//DTD HTML 2.0//EN">
                <html><head>
                <title>Proxy authentication failed</title>
                </head><body>
                Winfoom failed to login to the remote proxy with the provided credentials\
                </body></html>
                """;
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            write(HttpUtils.toStatusLine(request != null ? request.getProtocolVersion() : HttpVersion.HTTP_1_1,
                    HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED));
            write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER, HttpUtils.getCurrentDate()));
            write(HttpUtils.createHttpHeader(HttpHeaders.CONTENT_LENGTH, "" + bytes.length));
            writeln();
            outputStream.write(bytes);
            outputStream.flush();
        } catch (Exception ex) {
            log.debug("Error on writing proxy auth required error response", ex);
        }
    }

    /**
     * Write the response to the output stream as it is.
     * <p><b>This method commits the response.</b></p>
     *
     * @param httpResponse the HTTP response
     * @throws Exception
     */
    public void writeHttpResponse(@NotNull final HttpResponse httpResponse) throws IOException {
        StatusLine statusLine = httpResponse.getStatusLine();
        log.debug("Write statusLine {}", statusLine);
        write(statusLine);

        for (Header header : httpResponse.getAllHeaders()) {
            log.debug("Write header {}", header);
            write(header);
        }

        // Empty line between headers and the body
        writeln();

        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            log.debug("Write entity content");
            entity.writeTo(outputStream);
        }
        EntityUtils.consume(entity);
    }

    /**
     * @return {@code true} iff the underlying socket is closed.
     */
    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * @return the request's line
     */
    public RequestLine getRequestLine() {
        return request.getRequestLine();
    }

    void prepare() {
        if (!connect) {
            try {
                prepareRequest();
            } catch (Exception e) {
                writeErrorResponse(
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        "Unexpected error: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     Prepare the request for execution:
     remove some headers, fix VIA header and set a proper entity.
     */
    private void prepareRequest() {
        log.debug("Prepare the request for execution");

        if (request instanceof HttpEntityEnclosingRequest) {
            prepareHttpEntityEnclosingRequest();
        } else {
            log.debug("No enclosing entity");
        }

        removeBannedHeaders();
        fixViaHeader();
    }

    private void fixViaHeader() {
        // Add a Via header and remove the existent one(s)
        Header viaHeader = request.getFirstHeader(HttpHeaders.VIA);
        request.removeHeaders(HttpHeaders.VIA);
        request.setHeader(HttpUtils.createViaHeader(request.getRequestLine().getProtocolVersion(),
                viaHeader));
    }

    private void removeBannedHeaders() {
        List<String> bannedHeaders = request instanceof HttpEntityEnclosingRequest ?
                HttpUtils.ENTITY_BANNED_HEADERS : HttpUtils.DEFAULT_BANNED_HEADERS;
        for (Header header : request.getAllHeaders()) {
            if (bannedHeaders.contains(header.getName())) {
                request.removeHeader(header);
                log.debug("Request header {} removed", header);
            } else {
                log.debug("Allow request header {}", header);
            }
        }
    }

    private void prepareHttpEntityEnclosingRequest() {
        log.debug("Set enclosing entity");
        RepeatableHttpEntity entity = new RepeatableHttpEntity(request,
                sessionInputBuffer,
                proxyConfig.getTempDirectory(),
                systemConfig.getInternalBufferLength());
        Header transferEncoding = request.getFirstHeader(HTTP.TRANSFER_ENCODING);
        if (transferEncoding != null
                && StringUtils.containsIgnoreCase(transferEncoding.getValue(), HTTP.CHUNK_CODING)) {
            log.debug("Mark entity as chunked");
            entity.setChunked(true);

            // Apache HttpClient adds a Transfer-Encoding header's chunk directive
            // so remove or strip the existent one from chunk directive
            request.removeHeader(transferEncoding);
            String nonChunkedTransferEncoding = HttpUtils.stripChunked(transferEncoding.getValue());
            if (StringUtils.isNotEmpty(nonChunkedTransferEncoding)) {
                request.addHeader(
                        HttpUtils.createHttpHeader(HttpHeaders.TRANSFER_ENCODING,
                                nonChunkedTransferEncoding));
                log.debug("Add chunk-striped request header");
            } else {
                log.debug("Remove transfer encoding chunked request header");
            }

        }
        ((HttpEntityEnclosingRequest) request).setEntity(entity);
    }

    @Override
    public void close() {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (entity instanceof AutoCloseable) {
                InputOutputs.close((AutoCloseable) entity);
            }
        }
    }

    @Override
    public String toString() {
        return "ClientConnection{" +
                "requestUri=" + requestUri +
                '}';
    }

}
