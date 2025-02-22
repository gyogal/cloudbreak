package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;

class DistroXDatabaseRequestToStackDatabaseRequestConverterTest {

    private static final String DB_VERSION = "13";

    private final DistroXDatabaseRequestToStackDatabaseRequestConverter underTest = new DistroXDatabaseRequestToStackDatabaseRequestConverter();

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void convertDatabaseAvailabilityType(DatabaseAvailabilityType daType) {
        DatabaseRequest source = new DatabaseRequest();
        source.setAvailabilityType(daType);
        DistroXDatabaseRequest result = underTest.convert(source);
        assertThat(result.getAvailabilityType().name()).isEqualTo(daType.name());
    }

    @ParameterizedTest
    @EnumSource(DistroXDatabaseAvailabilityType.class)
    void convert(DistroXDatabaseAvailabilityType daType) {
        DistroXDatabaseRequest source = new DistroXDatabaseRequest();
        source.setAvailabilityType(daType);
        DatabaseRequest result = underTest.convert(source);
        assertThat(result.getAvailabilityType().name()).isEqualTo(daType.name());
    }

    void convertDatabaseEngineVersion() {
        DatabaseRequest source = new DatabaseRequest();
        source.setDatabaseEngineVersion(DB_VERSION);
        DistroXDatabaseRequest result = underTest.convert(source);
        assertThat(result.getDatabaseEngineVersion()).isEqualTo(DB_VERSION);
    }

    void convertDistroXDatabaseEngineVersion() {
        DistroXDatabaseRequest source = new DistroXDatabaseRequest();
        source.setDatabaseEngineVersion(DB_VERSION);
        DatabaseRequest result = underTest.convert(source);
        assertThat(result.getDatabaseEngineVersion()).isEqualTo(DB_VERSION);
    }
}
