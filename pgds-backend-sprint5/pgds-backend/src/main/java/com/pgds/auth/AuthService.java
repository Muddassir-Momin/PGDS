package com.pgds.auth;

import com.pgds.auth.AuthDtos.*;
import com.pgds.common.ApiException;
import com.pgds.domain.AppUser;
import com.pgds.domain.Role;
import com.pgds.repo.FairPriceShopRepository;
import com.pgds.repo.UserRepository;
import com.pgds.repo.WarehouseRepository;
import com.pgds.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final WarehouseRepository warehouses;
    private final FairPriceShopRepository shops;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        AppUser u = users.findByUsername(req.username()).orElseThrow();
        return toResponse(u);
    }

    /** Public self-registration is only allowed for the BENEFICIARY role. */
    public AuthResponse register(RegisterRequest req) {
        AppUser u = save(req.username(), req.password(), req.fullName(), req.email(), Role.BENEFICIARY, null, null);
        return toResponse(u);
    }

    public AppUser createStaff(CreateStaffRequest r) {
        var wh = r.warehouseId() == null ? null : warehouses.findById(r.warehouseId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Warehouse not found"));
        var fps = r.fpsId() == null ? null : shops.findById(r.fpsId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Fair Price Shop not found"));
        if (r.role() == Role.WAREHOUSE_MANAGER && wh == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "warehouseId is required for WAREHOUSE_MANAGER");
        if (r.role() == Role.FPS_DEALER && fps == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "fpsId is required for FPS_DEALER");
        return save(r.username(), r.password(), r.fullName(), r.email(), r.role(), wh, fps);
    }

    private AppUser save(String username, String password, String fullName, String email, Role role,
                         com.pgds.domain.Warehouse wh, com.pgds.domain.FairPriceShop fps) {
        if (users.existsByUsername(username))
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        return users.save(AppUser.builder().username(username).passwordHash(encoder.encode(password))
                .fullName(fullName).email(email).role(role).warehouse(wh).fps(fps).build());
    }

    private AuthResponse toResponse(AppUser u) {
        return new AuthResponse(jwt.generateToken(u.getUsername(), u.getRole().name()), "Bearer",
                jwt.getExpirationMs(), u.getUsername(), u.getFullName(), u.getRole());
    }
}
