package com.sequenceiq.cloudbreak.cm.client.tracing;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@Component
public class CmRequestIdProviderInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmRequestIdProviderInterceptor.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            LOGGER.debug("Generated requestId for request: {}", requestId);
        }
        Request modifiedRequest = originalRequest.newBuilder()
                .header(REQUEST_ID_HEADER, requestId)
                .build();
        return chain.proceed(modifiedRequest);
    }
}
