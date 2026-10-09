package com.pgds.card;

import com.pgds.card.CardDtos.*;
import com.pgds.common.ApiException;
import com.pgds.domain.*;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;
    private final RationCardRepository cards;
    private final UserRepository users;

    public BeneficiaryView create(BeneficiaryRequest r) {
        RationCard card = cards.findById(r.rationCardId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ration card not found"));
        if (!card.isEligible())
            throw new ApiException(HttpStatus.CONFLICT, "Ration card is not eligible");
        if (beneficiaries.countByRationCardId(card.getId()) >= card.getFamilyMembers())
            throw new ApiException(HttpStatus.CONFLICT, "Card already has " + card.getFamilyMembers() + " registered members");
        // Duplicate guard (basic): same name + same Aadhaar last-4 anywhere in the system.
        // Fuzzy matching across spellings is planned for the AI sprint.
        if (beneficiaries.existsByAadhaarLast4AndNameIgnoreCase(r.aadhaarLast4(), r.name().trim()))
            throw new ApiException(HttpStatus.CONFLICT, "Possible duplicate beneficiary already registered");

        AppUser linked = null;
        if (r.linkUsername() != null && !r.linkUsername().isBlank()) {
            linked = users.findByUsername(r.linkUsername())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Linked user not found"));
            if (linked.getRole() != Role.BENEFICIARY)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Linked user is not a BENEFICIARY account");
            if (beneficiaries.existsByUserId(linked.getId()))
                throw new ApiException(HttpStatus.CONFLICT, "That user is already linked to a beneficiary");
        }
        return toView(beneficiaries.save(Beneficiary.builder().name(r.name().trim()).age(r.age())
                .aadhaarLast4(r.aadhaarLast4()).rationCard(card).user(linked).build()));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryView> byCard(Long cardId) {
        return beneficiaries.findByRationCardId(cardId).stream().map(BeneficiaryService::toView).toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryView get(Long id) {
        return toView(beneficiaries.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Beneficiary not found")));
    }

    public void delete(Long id) {
        if (!beneficiaries.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Beneficiary not found");
        beneficiaries.deleteById(id);
    }

    public static BeneficiaryView toView(Beneficiary b) {
        return new BeneficiaryView(b.getId(), b.getName(), b.getAge(), "XXXX-XXXX-" + b.getAadhaarLast4(),
                b.getRationCard().getId(), b.getRationCard().getCardNumber());
    }
}
