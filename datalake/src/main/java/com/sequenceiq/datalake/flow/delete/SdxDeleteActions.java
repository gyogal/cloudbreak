package com.sequenceiq.datalake.flow.delete;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_STACK_DELETION_IN_PROGRESS_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingSuccessEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Configuration
public class SdxDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private ProvisionerService provisionerService;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Bean(name = "SDX_DELETION_START_STATE")
    public Action<?, ?> sdxDeletion() {
        return new AbstractSdxAction<>(SdxDeleteStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxDeleteStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeleteStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Start stack deletion for SDX: {}", payload.getResourceId());
                jobService.unschedule(String.valueOf(context.getSdxId()));
                provisionerService.startStackDeletion(payload.getResourceId(), payload.isForced());
                eventSenderService.notifyEvent(context, ResourceEvent.SDX_CLUSTER_DELETION_STARTED);
                sendEvent(context, SDX_STACK_DELETION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDeleteStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex, payload.isForced());
            }
        };
    }

    @Bean(name = "SDX_STACK_DELETION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackDeletionInProgress() {
        return new AbstractSdxAction<>(SdxDeleteStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxDeleteStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeleteStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake stack deletion in progress: {}", payload.getResourceId());
                sendEvent(context, StackDeletionWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(SdxDeleteStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex, payload.isForced());
            }
        };
    }

    @Bean(name = "SDX_DELETION_STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STATE")
    public Action<?, ?> storageConsumptionCollectionUnschedulingInProgress() {
        return new AbstractSdxAction<>(StackDeletionSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackDeletionSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StackDeletionSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake storage consumption collection unscheduling of SDX cluster: {}", payload.getResourceId());
                eventSenderService.notifyEvent(context, ResourceEvent.SDX_STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED);
                sendEvent(context, StorageConsumptionCollectionUnschedulingRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(StackDeletionSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex, payload.isForced());
            }
        };
    }

    @Bean(name = "SDX_DELETION_WAIT_RDS_STATE")
    public Action<?, ?> sdxDeleteRdsAction() {
        return new AbstractSdxAction<>(StorageConsumptionCollectionUnschedulingSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StorageConsumptionCollectionUnschedulingSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StorageConsumptionCollectionUnschedulingSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake delete remote database of SDX cluster: {}", payload.getResourceId());
                sendEvent(context, RdsDeletionWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(StorageConsumptionCollectionUnschedulingSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex, payload.isForced());
            }
        };
    }

    @Bean(name = "SDX_DELETION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(RdsDeletionSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    RdsDeletionSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsDeletionSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                Long datalakeId = payload.getResourceId();
                LOGGER.info("Datalake delete finalized: {}", datalakeId);
                SdxCluster sdxCluster = sdxService.getById(datalakeId);
                if (sdxCluster != null) {
                    metricService.incrementMetricCounter(MetricType.SDX_DELETION_FINISHED, sdxCluster);
                }
                eventSenderService.notifyEvent(context, ResourceEvent.SDX_CLUSTER_DELETION_FINISHED);
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(),
                        flowLogService.getLastFlowLog(context.getFlowParameters().getFlowId()))) {
                    eventSenderService.notifyEvent(context, ResourceEvent.DATALAKE_RESIZE_COMPLETE);
                }
                sendEvent(context, SDX_DELETE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(RdsDeletionSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_DELETION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxDeletionFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxDeletionFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeletionFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                String statusReason = "Datalake deletion failed";
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
                if (StringUtils.hasText(errorMessage)) {
                    statusReason = statusReason + ". " + errorMessage;
                } else if (exception.getMessage() != null) {
                    statusReason = statusReason + ". " + exception.getMessage();
                }
                LOGGER.error(statusReason, exception);
                try {
                    SdxCluster sdxCluster =  sdxService.getById(payload.getResourceId());
                    metricService.incrementMetricCounter(MetricType.SDX_DELETION_FAILED, sdxCluster);

                    if (sdxCluster.isDetached()) {
                        sdxStatusService.setStatusForDatalakeAndNotify(
                                DatalakeStatusEnum.DELETE_FAILED, ResourceEvent.SDX_DETACHED_CLUSTER_DELETION_FAILED,
                                List.of(sdxCluster.getClusterName()), statusReason, sdxCluster
                        );
                    } else {
                        sdxStatusService.setStatusForDatalakeAndNotify(
                                DatalakeStatusEnum.DELETE_FAILED, List.of(sdxCluster.getClusterName()), statusReason, sdxCluster
                        );
                    }
                } catch (NotFoundException notFoundException) {
                    LOGGER.error("Cannot set status to SDX_DELETION_FAILED because datalake was not found", notFoundException);
                }
                sendEvent(context, SDX_DELETE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDeletionFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
