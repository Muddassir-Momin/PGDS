package com.pgds.card;

import com.pgds.card.CardDtos.*;
import com.pgds.common.ApiException;
import com.pgds.common.PageResponse;
import com.pgds.domain.*;
import com.pgds.entitlement.EntitlementService;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final RationCardRepository cards;
    private final FairPriceShopRepository shops;
    private final BeneficiaryRepository beneficiaries;
    private final EntitlementService entitlement;

    public CardView create(CardRequest r) {
        if (cards.existsByCardNumber(r.cardNumber()))
            throw new ApiException(HttpStatus.CONFLICT, "Ration card number already exists");
        RationCard c = RationCard.builder().cardNumber(r.cardNumber().trim()).build();
        apply(c, r);
        return toView(cards.save(c));
    }

    public CardView update(Long id, CardRequest r) {
        RationCard c = find(id);
        if (!c.getCardNumber().equals(r.cardNumber()) && cards.existsByCardNumber(r.cardNumber()))
            throw new ApiException(HttpStatus.CONFLICT, "Ration card number already exists");
        if (r.familyMembers() < beneficiaries.countByRationCardId(id))
            throw new ApiException(HttpStatus.CONFLICT, "Family size is smaller than the number of registered members");
        c.setCardNumber(r.cardNumber().trim());
        apply(c, r);
        return toView(c);
    }

    public CardView setEligibility(Long id, boolean eligible) {
        RationCard c = find(id);
        c.setEligible(eligible);
        return toView(c);
    }

    @Transactional(readOnly = true)
    public CardView get(Long id) { return toView(find(id)); }

    @Transactional(readOnly = true)
    public PageResponse<CardView> list(String district, Pageable pageable) {
        var page = (district == null || district.isBlank())
                ? cards.findAll(pageable)
                : cards.findByDistrictIgnoreCase(district, pageable);
        return PageResponse.of(page.map(this::toView));
    }

    public CardView toView(RationCard c) {
        return new CardView(c.getId(), c.getCardNumber(), c.getHeadOfFamily(), c.getAddress(), c.getDistrict(),
                c.getCategory(), c.getFamilyMembers(), c.isEligible(),
                c.getFps() == null ? null : c.getFps().getId(),
                entitlement.calculate(c.getCategory(), c.getFamilyMembers()));
    }

    private void apply(RationCard c, CardRequest r) {
        c.setHeadOfFamily(r.headOfFamily());
        c.setAddress(r.address());
        c.setDistrict(r.district());
        c.setCategory(r.category());
        c.setFamilyMembers(r.familyMembers());
        c.setFps(r.fpsId() == null ? null : shops.findById(r.fpsId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Fair Price Shop not found")));
    }

    private RationCard find(Long id) {
        return cards.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ration card not found"));
    }
}
