package com.pgds.portal;

import com.pgds.common.ApiException;
import com.pgds.distribution.DistributionDtos.EntitlementBalance;
import com.pgds.distribution.DistributionDtos.ReceiptView;
import com.pgds.distribution.DistributionService;
import com.pgds.domain.RationCard;
import com.pgds.repo.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiary")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalActivityController {

    private final BeneficiaryRepository beneficiaries;
    private final DistributionService distributions;

    /** This month: entitled, already received, remaining. */
    @GetMapping("/entitlement")
    public EntitlementBalance entitlement(Authentication auth) { return distributions.balance(myCard(auth)); }

    @GetMapping("/distributions")
    public List<ReceiptView> history(Authentication auth) { return distributions.historyForCard(myCard(auth).getId()); }

    private RationCard myCard(Authentication auth) {
        return beneficiaries.findByUserUsername(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No beneficiary profile is linked to this account yet"))
                .getRationCard();
    }
}
