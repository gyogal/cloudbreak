package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class TargetedUpscaleSupportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetedUpscaleSupportService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackUtil stackUtil;

    public TargetedUpscaleSupportService() {
    }

    public boolean targetedUpscaleOperationSupported(Stack stack) {
        try {
            return targetedUpscaleEntitlementsEnabled(stack.getResourceCrn()) && DnsResolverType.FREEIPA.equals(stack.getDomainDnsResolver());
        } catch (Exception e) {
            LOGGER.error("Error occurred during checking if targeted upscale supported, thus assuming it is not enabled, cause: ", e);
            return false;
        }
    }

    public boolean targetedUpscaleEntitlementsEnabled(String crn) {
        String accountId = Crn.safeFromString(crn).getAccountId();
        return entitlementService.targetedUpscaleSupported(accountId) && isUnboundEliminationSupported(accountId);
    }

    public Stack updateDnsResolverType(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> reachableNodes = stackUtil.collectReachableNodes(stack);
        Set<String> reachableHostnames = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        boolean unboundClusterConfigPresentOnAnyNodes = hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(primaryGatewayConfig, reachableHostnames);
        LOGGER.info("Result of check whether unbound config is present on nodes of stack [{}] is: {}",
                stack.getResourceCrn(), unboundClusterConfigPresentOnAnyNodes);
        if (unboundClusterConfigPresentOnAnyNodes) {
            stack.setDomainDnsResolver(DnsResolverType.LOCAL_UNBOUND);
        } else {
            stack.setDomainDnsResolver(DnsResolverType.FREEIPA);
        }
        return stack;
    }

    private boolean isUnboundEliminationSupported(String accountId) {
        if (entitlementService.isUnboundEliminationSupported(accountId)) {
            return true;
        } else {
            LOGGER.info("Unbound elimination is disabled for account {}, thus targeted upscale is not supported!", accountId);
            return false;
        }
    }
}
