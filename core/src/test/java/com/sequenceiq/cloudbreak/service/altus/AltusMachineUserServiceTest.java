package com.sequenceiq.cloudbreak.service.altus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class AltusMachineUserServiceTest {

    private static final String TEST_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private AltusMachineUserService underTest;

    @Mock
    private AltusIAMService altusIAMService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private TelemetryFeatureService telemetryFeatureService;

    private Stack stack;

    private Cluster cluster;

    private Telemetry telemetry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        User creator = new User();
        creator.setUserCrn(TEST_CRN);
        stack.setCreator(creator);
        stack.setType(StackType.WORKLOAD);
        stack.setResourceCrn(TEST_CRN);
        cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        telemetry = new Telemetry();
        Features features = new Features();
        features.addClusterLogsCollection(true);
        telemetry.setFeatures(features);
        underTest = new AltusMachineUserService(altusIAMService, stackDtoService,
                clusterService, componentConfigProviderService, regionAwareInternalCrnGeneratorFactory, entitlementService, telemetryFeatureService);
    }

    @Test
    public void testCreateMachineUserAndGenerateKeys() {
        // GIVEN
        Optional<AltusCredential> altusCredential = Optional.of(new AltusCredential("accessKey", "secretKey".toCharArray()));
        when(altusIAMService.generateDatabusMachineUserWithAccessKey(any(), anyBoolean())).thenReturn(altusCredential);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        // WHEN
        underTest.generateDatabusMachineUserForFluent(stack, telemetry, CdpAccessKeyType.ED25519);

        // THEN
        assertEquals("secretKey", new String(altusCredential.get().getPrivateKey()));
        verify(altusIAMService, times(1)).generateDatabusMachineUserWithAccessKey(any(), anyBoolean());
    }

    @Test
    public void testCleanupMachineUser() {
        // GIVEN
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doNothing().when(altusIAMService).clearMachineUser(any(), any(), any(), anyBoolean());
        // WHEN
        underTest.clearFluentMachineUser(stack, cluster, telemetry);

        // THEN
        verify(altusIAMService, times(1)).clearMachineUser(any(), any(), any(), anyBoolean());
    }
}