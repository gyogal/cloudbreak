package com.sequenceiq.freeipa.service.cloud;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class PlatformParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformParameterService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    public PlatformParameters getPlatformParameters(Stack stack, Credential credential) {
        MDCBuilder.getOrGenerateRequestId();
        LOGGER.debug("Get platform parameters for: {}", stack);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        PlatformParameterRequest parameterRequest = new PlatformParameterRequest(cloudContext, cloudCredential);
        freeIpaFlowManager.notifyNonFlowEvent(parameterRequest);
        try {
            PlatformParameterResult res = parameterRequest.await();
            LOGGER.debug("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

}
