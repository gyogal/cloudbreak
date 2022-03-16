package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxResizeTests extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeTests.class);

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "resize called on the SDX cluster",
            then = "SDX resize should be successful, the cluster should be up and running"
    )
    public void testSDXResize(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        AtomicReference<String> expectedShape = new AtomicReference<>();
        AtomicReference<String> expectedCrn = new AtomicReference<>();
        AtomicReference<String> expectedName = new AtomicReference<>();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        testContext
                .given(sdx, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    expectedShape.set(sdxUtil.getShape(testDto, client));
                    expectedCrn.set(sdxUtil.getCrn(testDto, client));
                    expectedName.set(testDto.getName());
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> validateStackCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateShape(dto))
                .then((tc, dto, client) -> validateClusterName(expectedName, dto))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "resize called on the SDX cluster",
            then = "SDX resize should be successful, the cluster should be up and running"
    )
    public void testSDXResizeWithRefreshDatahubForEphemeralDisk(TestContext testContext) {

        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);

        String sdx = resourcePropertyProvider().getName();
        AtomicReference<String> expectedShape = new AtomicReference<>();
        AtomicReference<String> expectedCrn = new AtomicReference<>();
        AtomicReference<String> expectedName = new AtomicReference<>();
        AtomicReference<String> expectedHost = new AtomicReference<>();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        String oldHiveHost = "";

        testContext
                .given(sdx, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    expectedShape.set(sdxUtil.getShape(testDto, client));
                    expectedCrn.set(sdxUtil.getCrn(testDto, client));
                    expectedName.set(testDto.getName());
                    return testDto;
                });

        testContext
                .given("eph_dx", DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), RunningParameter.key("eph_dx"))
                .given("eph_dx", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
                .then((tc, testDto, client) -> {
                    verifyMountPointsUsedForTemporalDisks(testDto, "ephfs", "ephfs1");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(sanitizedUserName, testDto);
                    return testDto;
                })
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, sanitizedUserName,
                        MOCK_UMS_PASSWORD))
                .when(distroXTestClient.stop(), RunningParameter.key("eph_dx"))
                .await(STACK_STOPPED, RunningParameter.key("eph_dx"))
                .when(distroXTestClient.start(), RunningParameter.key("eph_dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
                .then((tc, testDto, client) -> {
                    verifyMountPointsUsedForTemporalDisks(testDto, "ephfs", "ephfs1");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, sanitizedUserName, MOCK_UMS_PASSWORD);
                    expectedHost.set(clouderaManagerUtil.getCmServiceResourceHiveHost(testDto, sanitizedUserName, MOCK_UMS_PASSWORD));
                    return testDto;
                })
                .validate();

        testContext
                .given(sdx, SdxInternalTestDto.class)
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.DATAHUB_REFRESH_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))

                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> validateStackCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateShape(dto))
                .then((tc, dto, client) -> validateClusterName(expectedName, dto))
                .validate();


        testContext
                .given("eph_dx", DistroXTestDto.class)
                .then((tc, dto, client) -> validateHostName(expectedHost, sanitizedUserName, dto))
                .validate();
    }

    private void verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(String sanitizedUserName, DistroXTestDto testDto) {
        Set<String> mountPoints = Set.of();
        if (activeCloudPlatform(CloudPlatform.AWS)) {
            mountPoints = sshJClientActions.getAwsEphemeralVolumeMountPoints(testDto.getResponse().getInstanceGroups(), List.of(HostGroupType.MASTER.getName()));
        } else if (activeCloudPlatform(CloudPlatform.AZURE)) {
            mountPoints = Set.of("/mnt/resource", "/hadoopfs/ephfs1");
        }
        clouderaManagerUtil.checkClouderaManagerHdfsDatanodeRoleConfigGroups(testDto, sanitizedUserName, MOCK_UMS_PASSWORD, mountPoints);
        clouderaManagerUtil.checkClouderaManagerHdfsNamenodeRoleConfigGroups(testDto, sanitizedUserName, MOCK_UMS_PASSWORD, mountPoints);
    }

    private void verifyMountPointsUsedForTemporalDisks(DistroXTestDto testDto, String awsMountPrefix, String azureMountDir) {
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        if (activeCloudPlatform(CloudPlatform.AWS)) {
            sshJClientActions.checkAwsEphemeralDisksMounted(instanceGroups, List.of(HostGroupType.WORKER.getName()), awsMountPrefix);
        } else if (activeCloudPlatform(CloudPlatform.AZURE)) {
            sshJClientActions.checkAzureTemporalDisksMounted(instanceGroups, List.of(HostGroupType.WORKER.getName()), azureMountDir);
        }
    }

    private SdxInternalTestDto validateStackCrn(AtomicReference<String> originalCrn, SdxInternalTestDto dto) {
        String newCrn = dto.getResponse().getStackV4Response().getCrn();
        Log.log(LOGGER, format(" Stack new crn: %s ", newCrn));
        if (!newCrn.equals(originalCrn.get())) {
            throw new TestFailException(" The stack CRN has changed to: " + newCrn + " instead of: " + originalCrn.get());
        }
        return dto;
    }

    private SdxInternalTestDto validateCrn(AtomicReference<String> originalCrn, SdxInternalTestDto dto) {
        String newCrn = dto.getResponse().getCrn();
        Log.log(LOGGER, format(" New crn: %s ", newCrn));
        if (!newCrn.equals(originalCrn.get())) {
            throw new TestFailException(" The stack CRN has changed to: " + newCrn + " instead of: " + originalCrn.get());
        }
        return dto;
    }

    private DistroXTestDto validateHostName(AtomicReference<String> originalHostName, String sanitizedUserName, DistroXTestDto testDto) {
        String newHiveHost = clouderaManagerUtil.getCmServiceResourceHiveHost(testDto, sanitizedUserName, MOCK_UMS_PASSWORD);
        Log.log(LOGGER, format(" New Hive host: %s ", newHiveHost));

        if (newHiveHost.equals(originalHostName.get())) {
            throw new TestFailException(" The Cluster Hive host has not changed: " + newHiveHost + " and " + originalHostName.get());
        }

        return testDto;
    }

    private SdxInternalTestDto validateShape(SdxInternalTestDto dto) {
        SdxClusterShape newShape = dto.getResponse().getClusterShape();
        Log.log(LOGGER, format(" New shape: %s ", newShape.name()));
        if (!SdxClusterShape.MEDIUM_DUTY_HA.equals(newShape)) {
            throw new TestFailException(" The datalake shape is : " + newShape + " instead of: " + SdxClusterShape.MEDIUM_DUTY_HA.name());
        }
        return dto;
    }

    private SdxInternalTestDto validateClusterName(AtomicReference<String> originalName, SdxInternalTestDto dto) {
        String newClusterName = dto.getResponse().getStackV4Response().getCluster().getName();
        Log.log(LOGGER, format(" New cluster name: %s ", newClusterName));
        if (!originalName.get().equals(newClusterName)) {
            throw new TestFailException(" The datalake cluster name is : " + newClusterName + " instead of: " + originalName);
        }
        return dto;
    }

    private boolean activeCloudPlatform(CloudPlatform cloudPlatform) {
        return cloudPlatform.name().equalsIgnoreCase(commonCloudProperties().getCloudProvider());
    }
}
