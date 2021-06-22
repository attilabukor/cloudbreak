package com.sequenceiq.freeipa.client;

import java.util.UUID;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

public class RequestIdProviderInterceptor implements HttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdProviderInterceptor.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    @Override
    public void process(HttpRequest request, HttpContext context) {
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            LOGGER.debug("Generated requestId for request: {}", requestId);
        }
        request.addHeader(REQUEST_ID_HEADER, requestId);
    }
}
