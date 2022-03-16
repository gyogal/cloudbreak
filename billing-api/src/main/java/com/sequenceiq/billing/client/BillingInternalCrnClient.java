package com.sequenceiq.billing.client;

import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class BillingInternalCrnClient {

    private BillingServiceUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public BillingInternalCrnClient(BillingServiceUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public BillingServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }
}
