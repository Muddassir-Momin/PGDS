package com.pgds.card;

import com.pgds.card.CardDtos.*;
import com.pgds.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Access: SUPER_ADMIN and GOVT_OFFICER (see SecurityConfig: /api/officer/**). */
@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerCardController {

    private final CardService cards;
    private final BeneficiaryService beneficiaries;
    private final com.pgds.repo.FairPriceShopRepository shopRepo;

    @PostMapping("/ration-cards") @ResponseStatus(HttpStatus.CREATED)
    public CardView createCard(@Valid @RequestBody CardRequest r) { return cards.create(r); }

    @GetMapping("/ration-cards")
    public PageResponse<CardView> listCards(@RequestParam(required = false) String district,
                                            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return cards.list(district, pageable);
    }

    @GetMapping("/ration-cards/{id}")
    public CardView getCard(@PathVariable Long id) { return cards.get(id); }

    @PutMapping("/ration-cards/{id}")
    public CardView updateCard(@PathVariable Long id, @Valid @RequestBody CardRequest r) { return cards.update(id, r); }

    @PatchMapping("/ration-cards/{id}/eligibility")
    public CardView eligibility(@PathVariable Long id, @RequestParam boolean eligible) {
        return cards.setEligibility(id, eligible);
    }

    @PostMapping("/beneficiaries") @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryView addBeneficiary(@Valid @RequestBody BeneficiaryRequest r) { return beneficiaries.create(r); }

    @GetMapping("/beneficiaries")
    public List<BeneficiaryView> beneficiariesOfCard(@RequestParam Long cardId) { return beneficiaries.byCard(cardId); }

    @GetMapping("/beneficiaries/{id}")
    public BeneficiaryView getBeneficiary(@PathVariable Long id) { return beneficiaries.get(id); }

    @DeleteMapping("/beneficiaries/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(@PathVariable Long id) { beneficiaries.delete(id); }

    /** Shop picker for the card form. */
    @GetMapping("/fps") @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.pgds.master.MasterController.FpsView> listShops() {
        return shopRepo.findAll().stream().map(com.pgds.master.MasterController::view).toList();
    }
}
