package com.sequenceiq.datalake.service.upgrade.database;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;

@ExtendWith(MockitoExtension.class)
public class DatabaseEngineVersionReaderServiceTest {

    private static final String DATABASE_CRN = "databaseCrn";

    @Mock
    private DatabaseService databaseService;

    @Mock
    private CloudbreakStackService cloudbreakStackService;

    @InjectMocks
    private DatabaseEngineVersionReaderService underTest;

    @Test
    void testGetExternalDbMajorVersionWhenMajorVersionPresent() {
        SdxCluster sdxCluster = setupSdxClusterWithExternalDb();
        StackDatabaseServerResponse stackDatabaseServerResponse = new StackDatabaseServerResponse();
        stackDatabaseServerResponse.setMajorVersion(MajorVersion.VERSION_12);
        when(databaseService.getDatabaseServer(DATABASE_CRN)).thenReturn(stackDatabaseServerResponse);

        Optional<MajorVersion> result = underTest.getDatabaseServerMajorVersion(sdxCluster);

        assertTrue(result.isPresent());
        Assertions.assertEquals(MajorVersion.VERSION_12, result.get());
    }

    @Test
    void testGetExternalDbMajorVersionWhenMajorVersionEmpty() {
        SdxCluster sdxCluster = setupSdxClusterWithExternalDb();
        StackDatabaseServerResponse stackDatabaseServerResponse = new StackDatabaseServerResponse();
        when(databaseService.getDatabaseServer(DATABASE_CRN)).thenReturn(stackDatabaseServerResponse);

        Optional<MajorVersion> result = underTest.getDatabaseServerMajorVersion(sdxCluster);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmbeddedDbMajorVersionWhenMajorVersionPresent() {
        SdxCluster sdxCluster = new SdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        DatabaseResponse databaseResponse = new DatabaseResponse();
        databaseResponse.setDatabaseEngineVersion("10.4");
        stackV4Response.setExternalDatabase(databaseResponse);
        when(cloudbreakStackService.getStack(sdxCluster)).thenReturn(stackV4Response);

        Optional<MajorVersion> result = underTest.getDatabaseServerMajorVersion(sdxCluster);

        assertTrue(result.isPresent());
        Assertions.assertEquals(MajorVersion.VERSION_10, result.get());
    }

    @Test
    void testGetEmbeddedDbMajorVersionWhenMajorVersionEmpty() {
        SdxCluster sdxCluster = new SdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        when(cloudbreakStackService.getStack(sdxCluster)).thenReturn(stackV4Response);

        Optional<MajorVersion> result = underTest.getDatabaseServerMajorVersion(sdxCluster);

        assertTrue(result.isEmpty());
    }

    private SdxCluster setupSdxClusterWithExternalDb() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        return sdxCluster;
    }

}