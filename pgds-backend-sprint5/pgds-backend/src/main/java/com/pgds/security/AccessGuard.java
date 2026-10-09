package com.pgds.security;

import com.pgds.common.ApiException;
import com.pgds.domain.AppUser;
import com.pgds.domain.Role;
import com.pgds.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Object-level checks: a warehouse manager only touches their warehouse, a dealer only their shop. Call inside a transaction. */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final UserRepository users;

    public void checkWarehouse(String username, Long warehouseId) {
        AppUser u = load(username);
        if (u.getRole() == Role.WAREHOUSE_MANAGER
                && (u.getWarehouse() == null || !u.getWarehouse().getId().equals(warehouseId)))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own warehouse");
    }

    public void checkFps(String username, Long fpsId) {
        AppUser u = load(username);
        if (u.getRole() == Role.FPS_DEALER
                && (u.getFps() == null || !u.getFps().getId().equals(fpsId)))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own Fair Price Shop");
    }

    private AppUser load(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}
