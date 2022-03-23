package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class TargetedUpscaleSupportServiceTest {

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private TargetedUpscaleSupportService underTest;

    @Test
    public void testIfEntitlementsDisabled() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));

        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));

        verify(entitlementService, times(2)).targetedUpscaleSupported(any());
        verify(entitlementService, times(1)).isUnboundEliminationSupported(any());
    }

    @Test
    public void testIfFreeipaDnsResolver() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        assertTrue(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.FREEIPA)));
    }

    @Test
    public void testIfUnboundDnsResolver() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.LOCAL_UNBOUND)));
    }

    @Test
    public void testIfThereIsAnyError() {
        when(entitlementService.targetedUpscaleSupported(any())).thenThrow(new InternalServerErrorException("error"));
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));
    }

    private Stack getStack(DnsResolverType dnsResolverType) {
        Stack stack = new Stack();
        stack.setResourceCrn(DATAHUB_CRN);
        stack.setDomainDnsResolver(dnsResolverType);
        return stack;
    }
}
