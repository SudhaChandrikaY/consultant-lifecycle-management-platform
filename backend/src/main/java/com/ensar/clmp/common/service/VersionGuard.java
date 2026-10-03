package com.ensar.clmp.common.service;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.ensar.clmp.common.domain.Versioned;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;

/** Refuses an update made against a stale read (409 CONCURRENT_MODIFICATION, research R10). */
@Component
public class VersionGuard {

    public void check(Versioned entity, Long requestVersion) {
        if (requestVersion == null) {
            throw BusinessException.fieldError("version", "Version is required.");
        }
        if (!Objects.equals(entity.getVersion(), requestVersion)) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                    "This record was changed by someone else. Reload to continue.",
                    Map.of("currentVersion", entity.getVersion()));
        }
    }
}
