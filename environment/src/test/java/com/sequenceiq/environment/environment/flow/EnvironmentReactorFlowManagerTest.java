package com.sequenceiq.environment.environment.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class EnvironmentReactorFlowManagerTest {

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String USER_CRN = "userCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private EnvironmentReactorFlowManager underTest;

    @Mock
    private FlowIdentifier flowIdentifier;

    @Captor
    private ArgumentCaptor<EnvDeleteEvent> envDeleteEventCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersCaptor;

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void triggerDeleteFlowTest(boolean forced) {
        EnvironmentView environment = environmentView();
        when(eventSender.sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.triggerDeleteFlow(environment, USER_CRN, forced);

        assertThat(result).isSameAs(flowIdentifier);
        verify(flowCancelService).cancelRunningFlows(ENVIRONMENT_ID);
        verify(eventSender).sendEvent(envDeleteEventCaptor.capture(), headersCaptor.capture());
        verifyEnvDeleteEvent(forced, "START_FREEIPA_DELETE_EVENT");
        verifyHeaders();
    }

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void triggerCascadingDeleteFlowTest(boolean forced) {
        EnvironmentView environment = environmentView();
        when(eventSender.sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.triggerCascadingDeleteFlow(environment, USER_CRN, forced);

        assertThat(result).isSameAs(flowIdentifier);
        verify(flowCancelService).cancelRunningFlows(ENVIRONMENT_ID);
        verify(eventSender).sendEvent(envDeleteEventCaptor.capture(), headersCaptor.capture());
        verifyEnvDeleteEvent(forced, "ENV_DELETE_CLUSTERS_TRIGGER_EVENT");
        verifyHeaders();
    }

    private void verifyEnvDeleteEvent(boolean forcedExpected, String selectorExpected) {
        EnvDeleteEvent envDeleteEvent = envDeleteEventCaptor.getValue();
        assertThat(envDeleteEvent).isNotNull();
        assertThat(envDeleteEvent.accepted()).isNotNull();
        assertThat(envDeleteEvent.getSelector()).isEqualTo(selectorExpected);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.isForceDelete()).isEqualTo(forcedExpected);
    }

    private void verifyHeaders() {
        Event.Headers headers = headersCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.asMap()).containsOnly(Map.entry(FlowConstants.FLOW_TRIGGER_USERCRN, USER_CRN));
    }

    private EnvironmentView environmentView() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(ENVIRONMENT_ID);
        environment.setName(ENVIRONMENT_NAME);
        return environment;
    }

}