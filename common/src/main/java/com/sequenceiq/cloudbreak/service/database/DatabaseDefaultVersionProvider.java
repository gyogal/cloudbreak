package com.sequenceiq.cloudbreak.service.database;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class DatabaseDefaultVersionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDefaultVersionProvider.class);

    @Value("${cb.db.override.minRuntimeVersion}")
    private String minRuntimeVersion;

    @Value("${cb.db.override.engineVersion}")
    private String dbEngineVersion;

    private final VersionComparator versionComparator = new VersionComparator();

    public String calculateDbVersionBasedOnRuntimeIfMissing(String runtime, String requestedDbEngineVersion) {
        if (StringUtils.isNotBlank(requestedDbEngineVersion)) {
            LOGGER.debug("DB engine version already requested to be [{}]", requestedDbEngineVersion);
            return requestedDbEngineVersion;
        } else if (StringUtils.isNotBlank(runtime)) {
            if (0 <= versionComparator.compare(() -> runtime, () -> minRuntimeVersion)) {
                LOGGER.debug("Setting DB engine version to [{}] for runtime [{}]", dbEngineVersion, runtime);
                return dbEngineVersion;
            } else {
                LOGGER.debug("Setting DB engine version to 'null' for runtime [{}]", runtime);
                return null;
            }
        } else {
            LOGGER.warn("Runtime is null, cannot provide default db engine version. Setting it to 'null'");
            return null;
        }
    }
}
