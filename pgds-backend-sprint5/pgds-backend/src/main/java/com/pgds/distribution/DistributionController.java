package com.pgds.distribution;

import com.pgds.common.PageResponse;
import com.pgds.distribution.DistributionDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fps/{fpsId}")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService service;

    @PostMapping("/distributions") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FPS_DEALER')")
    public ReceiptView distribute(Authentication a, @PathVariable Long fpsId, @Valid @RequestBody DistributionRequest r) {
        return service.distribute(a.getName(), fpsId, r);
    }

    @GetMapping("/cards/{cardNumber}/balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FPS_DEALER')")
    public EntitlementBalance balance(Authentication a, @PathVariable Long fpsId, @PathVariable String cardNumber) {
        return service.balanceAtShop(a.getName(), fpsId, cardNumber);
    }

    @GetMapping("/distributions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','GOVT_OFFICER','AUDITOR','FPS_DEALER')")
    public PageResponse<DistributionRow> history(Authentication a, @PathVariable Long fpsId,
                                                 @PageableDefault(size = 20) Pageable p) {
        return service.shopHistory(a.getName(), fpsId, p);
    }
}
