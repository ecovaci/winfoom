package org.kpax.winfoom.exception;

import org.apache.http.HttpResponse;

public class ProxyAuthorizationException extends Exception {
    private final HttpResponse response;

    public ProxyAuthorizationException(HttpResponse response) {
        this.response = response;
    }

    public ProxyAuthorizationException(HttpResponse response, String message) {
        super(message);
        this.response = response;
    }

    public HttpResponse getResponse() {
        return response;
    }
}