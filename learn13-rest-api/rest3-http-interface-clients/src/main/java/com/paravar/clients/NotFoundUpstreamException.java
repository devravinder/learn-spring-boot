package com.paravar.clients;

public class NotFoundUpstreamException extends RuntimeException {

    public NotFoundUpstreamException(String path) {
        super("upstream has nothing at " + path);
    }
}
