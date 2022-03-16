package com.sequenceiq.billing.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class BillingServiceClientBuilder extends AbstractUserCrnServiceClientBuilder<BillingServiceCrnClient> {

    public BillingServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected BillingServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new BillingServiceCrnClient(serviceAddress, configKey);
    }
}
