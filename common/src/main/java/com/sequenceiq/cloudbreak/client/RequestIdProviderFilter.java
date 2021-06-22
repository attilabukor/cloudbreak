package com.sequenceiq.cloudbreak.client;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

public class RequestIdProviderFilter implements ClientRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdProviderFilter.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            LOGGER.debug("Generated requestId for request: {}", requestId);
        }
        requestContext.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);
    }
}
