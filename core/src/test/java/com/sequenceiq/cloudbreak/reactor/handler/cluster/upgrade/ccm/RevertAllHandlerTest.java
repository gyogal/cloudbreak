package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeOrchestratorService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RevertAllHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final Long CLUSTER_ID = 34L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private UpgradeOrchestratorService upgradeOrchestratorService;

    @Mock
    private HandlerEvent<UpgradeCcmFailedEvent> event;

    @InjectMocks
    private RevertAllHandler underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADE_CCM_REVERT_ALL_COMMENCE_EVENT");
    }

    @Test
    void doAccept() {
        UpgradeCcmFailedEvent request = new UpgradeCcmFailedEvent(STACK_ID, CLUSTER_ID, Tunnel.CCM, null, null, LocalDateTime.now());
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        InOrder inOrder = inOrder(upgradeCcmService, upgradeOrchestratorService);
        inOrder.verify(upgradeCcmService).registerClusterProxy(STACK_ID);
        inOrder.verify(upgradeCcmService).healthCheck(STACK_ID);
        inOrder.verify(upgradeOrchestratorService).pushSaltState(STACK_ID, CLUSTER_ID);
        assertThat(result.selector()).isEqualTo("UPGRADECCMFAILEDEVENT");
    }
}
