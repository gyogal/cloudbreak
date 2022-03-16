package com.sequenceiq.billing.configuration.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.billing.configuration.registry.DNSServiceAddressResolver;
import com.sequenceiq.billing.configuration.registry.RetryingServiceAddressResolver;
import com.sequenceiq.billing.configuration.registry.ServiceAddressResolver;

@Configuration
public class ServiceEndpointConfig {

    @Value("${billing.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${billing.db.host:}")
    private String dbHost;

    @Value("${billing.db.port:}")
    private String dbPort;

    @Value("${billing.db.serviceId:}")
    private String databaseId;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }
}
