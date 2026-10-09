package com.pgds.complaint;

import com.pgds.common.PageResponse;
import com.pgds.complaint.ComplaintDtos.*;
import com.pgds.domain.ComplaintStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** /api/beneficiary/** is BENEFICIARY-only and /api/officer/** is ADMIN/OFFICER-only (see SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService service;

    @PostMapping("/api/beneficiary/complaints") @ResponseStatus(HttpStatus.CREATED)
    public ComplaintView submit(Authentication a, @Valid @RequestBody ComplaintRequest r) {
        return service.submit(a.getName(), r);
    }

    @GetMapping("/api/beneficiary/complaints")
    public PageResponse<ComplaintView> mine(Authentication a,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.mine(a.getName(), p);
    }

    @GetMapping("/api/officer/complaints")
    public PageResponse<ComplaintView> list(@RequestParam(required = false) ComplaintStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.list(status, p);
    }

    @PatchMapping("/api/officer/complaints/{id}")
    public ComplaintView update(@PathVariable Long id, @Valid @RequestBody ComplaintUpdate u) {
        return service.update(id, u);
    }
}
