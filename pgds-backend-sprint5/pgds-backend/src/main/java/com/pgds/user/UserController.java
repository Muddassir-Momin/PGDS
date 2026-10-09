package com.pgds.user;

import com.pgds.auth.AuthDtos.CreateStaffRequest;
import com.pgds.auth.AuthService;
import com.pgds.common.ApiException;
import com.pgds.domain.Role;
import com.pgds.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    public record UserView(Long id, String username, String fullName, String email, Role role,
                           Long warehouseId, Long fpsId, boolean enabled) {}

    private final UserRepository users;
    private final AuthService authService;

    @GetMapping("/api/users/me")
    public UserView me(Authentication auth) {
        var u = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toView(u);
    }

    @GetMapping("/api/admin/users")
    public List<UserView> all() { return users.findAll().stream().map(UserController::toView).toList(); }

    @PostMapping("/api/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateStaffRequest req) {
        return toView(authService.createStaff(req));
    }

    private static UserView toView(com.pgds.domain.AppUser u) {
        return new UserView(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getWarehouse() == null ? null : u.getWarehouse().getId(),
                u.getFps() == null ? null : u.getFps().getId(), u.isEnabled());
    }
}
