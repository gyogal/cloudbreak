package com.sequenceiq.cloudbreak.common.service;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockService.class);

    @Inject
    private JdbcTemplate jdbcTemplate;

    public void lockAndRunIfLockWasSuccessful(Runnable runnable, Integer lockNumber) {
        LOGGER.info("Try to get PostgreSQL advisory lock with lock number: {}", lockNumber);
        boolean lockSuccess = lock(lockNumber, jdbcTemplate);
        if (lockSuccess) {
            LOGGER.info("PostgreSQL advisory lock was successful with lock number: {}", lockNumber);
            runnable.run();
            unlock(lockNumber, jdbcTemplate);
        } else {
            LOGGER.warn("PostgreSQL advisory lock was unsuccessful with lock number: {}", lockNumber);
        }
    }

    private void unlock(Integer lockNumber, JdbcTemplate jdbcTemplate) {
        Boolean unlocked = jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(" + lockNumber + ")", Boolean.class);
        if (Boolean.FALSE.equals(unlocked)) {
            LOGGER.error("Unable to release PostgreSQL advisory lock with lock number: {}", lockNumber);
        } else {
            LOGGER.info("PostgreSQL advisory lock was released with lock number: {}", lockNumber);
        }
    }

    private boolean lock(Integer lockNumber, JdbcTemplate jdbcTemplate) {
        List<Boolean> results = jdbcTemplate.query("SELECT pg_try_advisory_lock(" + lockNumber + ")",
                (rs, rowNum) -> rs.getBoolean("pg_try_advisory_lock"));
        return results.size() == 1 && results.get(0);
    }

}
