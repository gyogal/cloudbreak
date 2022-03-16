package com.sequenceiq.billing.client;

import com.sequenceiq.billing.api.BillingApi;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class BillingServiceUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<BillingServiceUserCrnClient> {

    public BillingServiceUserCrnClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected BillingServiceUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new BillingServiceUserCrnClient(serviceAddress, configKey, BillingApi.API_ROOT_CONTEXT);
    }
}
