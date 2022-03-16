package com.sequenceiq.billing.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.billing.api.BillingApi;

public class BillingServiceApiKeyClient extends AbstractKeyBasedServiceClient<BillingServiceApiKeyEndpoints> {

    public BillingServiceApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, BillingApi.API_ROOT_CONTEXT);
    }

    @Override
    public BillingServiceApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new BillingServiceApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
