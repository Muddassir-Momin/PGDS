package com.pgds.portal;

import com.pgds.card.BeneficiaryService;
import com.pgds.card.CardDtos.*;
import com.pgds.card.CardService;
import com.pgds.common.ApiException;
import com.pgds.master.MasterController;
import com.pgds.master.MasterController.FpsView;
import com.pgds.repo.BeneficiaryRepository;
import com.pgds.repo.FairPriceShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Beneficiary portal (BENEFICIARY role only). */
@RestController
@RequestMapping("/api/beneficiary")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalController {

    public record MyProfile(BeneficiaryView me, CardView card, List<BeneficiaryView> familyMembers) {}

    private final BeneficiaryRepository beneficiaries;
    private final FairPriceShopRepository shops;
    private final CardService cardService;

    @GetMapping("/me")
    public MyProfile me(Authentication auth) {
        var b = beneficiaries.findByUserUsername(auth.getName()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "No beneficiary profile is linked to this account yet"));
        var card = b.getRationCard();
        var family = beneficiaries.findByRationCardId(card.getId()).stream().map(BeneficiaryService::toView).toList();
        return new MyProfile(BeneficiaryService.toView(b), cardService.toView(card), family);
    }

    /** Ration shops in the beneficiary's district. */
    @GetMapping("/shops")
    public List<FpsView> nearbyShops(Authentication auth) {
        var b = beneficiaries.findByUserUsername(auth.getName()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "No beneficiary profile is linked to this account yet"));
        return shops.findByDistrictIgnoreCase(b.getRationCard().getDistrict()).stream()
                .map(MasterController::view).toList();
    }
}
