package com.sequenceiq.datalake.service.sdx.refresh;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Component
public class SdxRefreshService {

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    public void refreshAllDatahub(Long sdxId) {

    }

    public void waitCloudbreakCluster(Long sdxId, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        cloudbreakPoller.pollDatahubRefreshUntilAvailable(sdxCluster, pollingConfig);

    }
}
