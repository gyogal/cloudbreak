package com.sequenceiq.billing.client.internal;

public class BillingApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String billingServerUrl;

    public BillingApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String billingServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.billingServerUrl = billingServerUrl;
    }

    public String getServiceUrl() {
        return billingServerUrl;
    }

    public boolean isCertificateValidation() {
        return certificateValidation;
    }

    public boolean isIgnorePreValidation() {
        return ignorePreValidation;
    }

    public boolean isRestDebug() {
        return restDebug;
    }
}
