package com.sequenceiq.environment.proxy.service;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class ProxyConfigModificationService {

    private final EntitlementService entitlementService;

    private final FreeIpaService freeIpaService;

    private final SdxService sdxService;

    private final DatahubService datahubService;

    public ProxyConfigModificationService(
            EntitlementService entitlementService,
            FreeIpaService freeIpaService,
            SdxService sdxService,
            DatahubService datahubService) {
        this.entitlementService = entitlementService;
        this.freeIpaService = freeIpaService;
        this.sdxService = sdxService;
        this.datahubService = datahubService;
    }

    public void modify(Environment environment, ProxyConfig proxyConfig) {
        validateModify(environment);
        throw new NotImplementedException("Editing the proxy configuration is not supported yet");
    }

    private void validateModify(Environment environment) {
        if (!entitlementService.isEditProxyConfigEnabled(environment.getAccountId())) {
            throw new BadRequestException("Proxy config editing is not enabled in your account");
        }
        DescribeFreeIpaResponse freeIpaResponse = freeIpaService.describe(environment.getResourceCrn())
                .orElseThrow(() -> new IllegalStateException("FreeIpa not found for environment " + environment.getResourceCrn()));
        if (!freeIpaResponse.getStatus().isAvailable()) {
            throw new BadRequestException("Proxy config editing is not supported when FreeIpa is not available");
        }
        List<SdxClusterResponse> sdxClusters = sdxService.list(environment.getResourceCrn());
        if (sdxClusters.stream().anyMatch(sdxClusterResponse -> !sdxClusterResponse.getStatus().isRunning())) {
            throw new BadRequestException("Proxy config editing is not supported when Data Lake is not running");
        }
        StackViewV4Responses stackViewV4Responses = datahubService.list(environment.getResourceCrn());
        if (stackViewV4Responses.getResponses().stream().anyMatch(stackViewV4Response -> !stackViewV4Response.getStatus().isAvailable())) {
            throw new BadRequestException("Proxy config editing is not supported when not all Data Hubs are available");
        }
    }
}
