package com.sequenceiq.billing.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class BillingServiceUserCrnClient extends AbstractUserCrnServiceClient<BillingServiceCrnEndpoints> {

    protected BillingServiceUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public BillingServiceCrnEndpoints withCrn(String crn) {
        return new BillingServiceCrnEndpoints(getWebTarget(), crn);
    }
}
