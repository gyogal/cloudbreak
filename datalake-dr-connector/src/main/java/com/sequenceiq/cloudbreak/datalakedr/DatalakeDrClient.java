package com.sequenceiq.cloudbreak.datalakedr;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc.datalakeDRBlockingStub;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeBackupInfo;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SkipFlag;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

@Component
public class DatalakeDrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDrClient.class);

    private static final String NO_CONNECTOR_ERROR = "The thunderhead-datalakedr service connector is not configured. Cannot get status.";

    private final DatalakeDrConfig datalakeDrConfig;

    private final GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter statusConverter;

    private final Tracer tracer;

    public DatalakeDrClient(DatalakeDrConfig datalakeDrConfig, GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter statusConverter, Tracer tracer) {
        this.datalakeDrConfig = datalakeDrConfig;
        this.statusConverter = statusConverter;
        this.tracer = tracer;
    }

    public DatalakeBackupStatusResponse triggerBackup(String datalakeName, String backupLocation, String backupName, String actorCrn,
            boolean skipAtlasMetadata, boolean skipRangerAudits, boolean skipRangerMetadata) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupLocation);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeRequest.Builder builder = BackupDatalakeRequest.newBuilder()
                    .setDatalakeName(datalakeName)
                    .setBackupLocation(backupLocation)
                    .setCloseDbConnections(true);

            if (skipAtlasMetadata) {
                builder.setSkipAtlasMetadata(SkipFlag.SKIP);
            }
            if (skipRangerAudits) {
                builder.setSkipRangerAudits(SkipFlag.SKIP);
            }
            if (skipRangerMetadata) {
                builder.setSkipRangerHmsMetadata(SkipFlag.SKIP);
            }
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .backupDatalake(builder.build())
            );
        }
    }

    public DatalakeBackupStatusResponse triggerBackupValidation(String datalakeName, String backupLocation, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupLocation);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeRequest.Builder builder = BackupDatalakeRequest.newBuilder()
                    .setDatalakeName(datalakeName)
                    .setBackupLocation(backupLocation)
                    .setValidationOnly(true);
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .backupDatalake(builder.build())
            );
        }
    }

    public DatalakeRestoreStatusResponse triggerRestore(String datalakeName, String backupId, String backupLocationOverride, String actorCrn,
            boolean skipAtlasMetadata, boolean skipRangerAudits, boolean skipRangerMetadata) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnRestore();
        }
        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeRequest.Builder builder = RestoreDatalakeRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            if (!Strings.isNullOrEmpty(backupId)) {
                builder.setBackupId(backupId);
            }
            if (skipAtlasMetadata) {
                builder.setSkipAtlasMetadata(SkipFlag.SKIP);
            }
            if (skipRangerAudits) {
                builder.setSkipRangerAudits(SkipFlag.SKIP);
            }
            if (skipRangerMetadata) {
                builder.setSkipRangerHmsMetadata(SkipFlag.SKIP);
            }
            if (!Strings.isNullOrEmpty(backupLocationOverride)) {
                builder.setBackupLocationOverride(backupLocationOverride);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .restoreDatalake(builder.build())
            );
        }
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String actorCrn) {
        return getBackupStatus(datalakeName, null, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String backupId, String actorCrn) {
        return getBackupStatus(datalakeName, backupId, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatus(String datalakeName, String backupId, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            if (!Strings.isNullOrEmpty(backupId)) {
                builder.setBackupId(backupId);
            }
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .backupDatalakeStatus(builder.build())
            );
        }
    }

    public String getBackupId(String datalakeName, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            throw new IllegalStateException("altus.datalakedr.endpoint is not enabled or configured appropriately!");
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            return newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn).backupDatalakeStatus(builder.build()).getBackupId();
        }
    }

    public DatalakeBackupStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId, String actorCrn) {
        return getBackupStatusByBackupId(datalakeName, backupId, null, actorCrn);
    }

    public DatalakeBackupStatusResponse getBackupStatusByBackupId(String datalakeName, String backupId,
        String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(backupId);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            BackupDatalakeStatusRequest.Builder builder = BackupDatalakeStatusRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setBackupId(backupId);
            if (!Strings.isNullOrEmpty(backupName)) {
                builder.setBackupName(backupName);
            }
            return statusConverter.convert(
                newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .backupDatalakeStatus(builder.build())
            );
        }
    }

    public DatalakeBackupStatusResponse getRestoreStatusByRestoreId(String datalakeName, String restoreId, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnBackup();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(restoreId);

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                    .setDatalakeName(datalakeName)
                    .setRestoreId(restoreId);

            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .restoreDatalakeStatus(builder.build())
            );
        }
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String actorCrn) {
        return getRestoreStatus(datalakeName, null, null, actorCrn);
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String restoreId, String actorCrn) {
        return getRestoreStatus(datalakeName, restoreId, null, actorCrn);
    }

    public DatalakeRestoreStatusResponse getRestoreStatus(String datalakeName, String restoreId, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            return missingConnectorResponseOnRestore();
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            if (!Strings.isNullOrEmpty(restoreId)) {
                builder.setRestoreId(restoreId);
            }
            return statusConverter.convert(
                    newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                            .restoreDatalakeStatus(builder.build())
            );
        }
    }

    public String getRestoreId(String datalakeName, String backupName, String actorCrn) {
        if (!datalakeDrConfig.isConfigured()) {
            throw new IllegalStateException("altus.datalakedr.endpoint is not enabled or configured appropriately!");
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            RestoreDatalakeStatusRequest.Builder builder = RestoreDatalakeStatusRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            return newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn).restoreDatalakeStatus(builder.build()).getRestoreId();
        }
    }

    public DatalakeBackupInfo getLastSuccessfulBackup(String datalakeName, String actorCrn, Optional<String> runtime) {
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .listDatalakeBackups(builder.build());

            if (response != null) {
                return response.getDatalakeInfoList().stream()
                        .filter(backup -> "SUCCESSFUL".equals(backup.getOverallState()))
                        .filter(backup -> runtime.isEmpty() || backup.getRuntimeVersion().equalsIgnoreCase(runtime.get()))
                        .peek(backupInfo -> LOGGER.debug(
                                "The following successful backup was found for data lake {} and runtime {}: {}", datalakeName, runtime, backupInfo))
                        .findFirst()
                        .orElse(null);
            }
            LOGGER.debug("No successful backup was found for data lake {} and runtime {}", datalakeName, runtime);
            return null;
        }
    }

    public DatalakeBackupInfo getBackupById(String datalakeName, String backupId, String actorCrn) {
        DatalakeBackupInfo datalakeBackupInfo = null;
        if (!datalakeDrConfig.isConfigured()) {
            return null;
        }

        checkNotNull(datalakeName);
        checkNotNull(backupId);
        checkNotNull(actorCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ListDatalakeBackupRequest.Builder builder = ListDatalakeBackupRequest.newBuilder()
                    .setDatalakeName(datalakeName);
            ListDatalakeBackupResponse response = newStub(channelWrapper.getChannel(), UUID.randomUUID().toString(), actorCrn)
                    .listDatalakeBackups(builder.build());

            if (response != null) {
                datalakeBackupInfo = response.getDatalakeInfoList().stream()
                        .filter(backup -> backupId.equals(backup.getBackupId()))
                        .findFirst().orElse(null);
            }
            return datalakeBackupInfo;
        }
    }

    /**
     * Creates Managed Channel wrapper from endpoint address
     *
     * @return the wrapper object
     */
    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
            ManagedChannelBuilder.forAddress(datalakeDrConfig.getHost(), datalakeDrConfig.getPort())
                .usePlaintext()
                .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                .build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param channel   channel
     * @param requestId the request ID
     * @param actorCrn  actor
     * @return the stub
     */
    private datalakeDRBlockingStub newStub(ManagedChannel channel, String requestId, String actorCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        return datalakeDRGrpc.newBlockingStub(channel)
            .withInterceptors(GrpcUtil.getTracingInterceptor(tracer), new AltusMetadataInterceptor(requestId, actorCrn));
    }

    private DatalakeBackupStatusResponse missingConnectorResponseOnBackup() {
        return new DatalakeBackupStatusResponse(UUID.randomUUID().toString(),
            DatalakeBackupStatusResponse.State.FAILED,
            Optional.of(NO_CONNECTOR_ERROR)
        );
    }

    private DatalakeRestoreStatusResponse missingConnectorResponseOnRestore() {
        return new DatalakeRestoreStatusResponse(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                DatalakeBackupStatusResponse.State.FAILED,
                Optional.of(NO_CONNECTOR_ERROR)
        );
    }
}
