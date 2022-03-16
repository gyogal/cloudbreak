package com.sequenceiq.billing.client;

import com.sequenceiq.billing.api.BillingApi;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class BillingServiceCrnClient extends AbstractUserCrnServiceClient<BillingServiceCrnEndpoints> {

    protected BillingServiceCrnClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, BillingApi.API_ROOT_CONTEXT);
    }

    @Override
    public BillingServiceCrnEndpoints withCrn(String crn) {
        return new BillingServiceCrnEndpoints(getWebTarget(), crn);
    }
}
