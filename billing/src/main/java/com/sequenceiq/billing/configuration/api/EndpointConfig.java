package com.sequenceiq.billing.configuration.api;

import java.util.List;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.billing.api.BillingApi;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPStructuredEventFilter;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@Configuration
@ApplicationPath(BillingApi.API_ROOT_CONTEXT)
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            AuthorizationUtilEndpoint.class,
            CDPStructuredEventV1Controller.class);

    private final String applicationVersion;

    private final Boolean auditEnabled;

    private final List<ExceptionMapper<?>> exceptionMappers;

    public EndpointConfig(@Value("${info.app.version:unspecified}") String applicationVersion,
            @Value("${billing.structuredevent.rest.enabled}") Boolean auditEnabled,
            List<ExceptionMapper<?>> exceptionMappers,
            ServerTracingDynamicFeature serverTracingDynamicFeature,
            ClientTracingFeature clientTracingFeature) {

        this.applicationVersion = applicationVersion;
        this.auditEnabled = auditEnabled;
        this.exceptionMappers = exceptionMappers;
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
        register(serverTracingDynamicFeature);
        register(clientTracingFeature);
    }

    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Billing API");
        swaggerConfig.setDescription("Billing operation related API.");
        swaggerConfig.setVersion(applicationVersion);
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(BillingApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.billing.api,com.sequenceiq.flow.api,com.billing.authorization," +
                "com.sequenceiq.billing.structuredevent.rest.endpoint");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);

        if (auditEnabled) {
            register(CDPStructuredEventFilter.class);
        }

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
