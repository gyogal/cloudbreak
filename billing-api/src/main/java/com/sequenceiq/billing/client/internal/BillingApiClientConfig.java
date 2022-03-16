package com.sequenceiq.billing.client.internal;

import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.billing.api.BillingApi;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class BillingApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    private final ClientTracingFeature clientTracingFeature;

    public BillingApiClientConfig(ApiClientRequestFilter apiClientRequestFilter, ClientTracingFeature clientTracingFeature) {
        this.apiClientRequestFilter = apiClientRequestFilter;
        this.clientTracingFeature = clientTracingFeature;
    }

    @Bean
    @ConditionalOnBean(BillingApiClientParams.class)
    public WebTarget billingApiClientWebTarget(BillingApiClientParams billingApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(billingApiClientParams.getServiceUrl())
                .withCertificateValidation(billingApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(billingApiClientParams.isIgnorePreValidation())
                .withDebug(billingApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(BillingApi.API_ROOT_CONTEXT)
                .withTracer(clientTracingFeature)
                .build();
    }
}
