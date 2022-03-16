package com.sequenceiq.billing.client;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface BillingClient {

    FlowEndpoint flowEndpoint();

    FlowPublicEndpoint flowPublicEndpoint();

    CDPStructuredEventV1Endpoint structuredEventsV1Endpoint();

    AuthorizationUtilEndpoint authorizationUtilEndpoint();

}