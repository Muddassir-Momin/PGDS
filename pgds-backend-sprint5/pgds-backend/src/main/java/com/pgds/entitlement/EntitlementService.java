package com.pgds.entitlement;

import com.pgds.domain.CardCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monthly entitlement under NFSA 2013:
 *   AAY: fixed kg per family     PHH: kg per person
 * Quantities and the rice/wheat split are configurable (they vary by state and month).
 */
@Service
public class EntitlementService {

    public record Entitlement(CardCategory category, int familyMembers,
                              BigDecimal riceKg, BigDecimal wheatKg, BigDecimal totalKg) {}

    private final BigDecimal aayKgPerFamily;
    private final BigDecimal phhKgPerPerson;
    private final BigDecimal riceShare;

    public EntitlementService(@Value("${pgds.entitlement.aay-kg-per-family:35}") BigDecimal aayKgPerFamily,
                              @Value("${pgds.entitlement.phh-kg-per-person:5}") BigDecimal phhKgPerPerson,
                              @Value("${pgds.entitlement.rice-share:0.5}") BigDecimal riceShare) {
        this.aayKgPerFamily = aayKgPerFamily;
        this.phhKgPerPerson = phhKgPerPerson;
        this.riceShare = riceShare;
    }

    public Entitlement calculate(CardCategory category, int familyMembers) {
        if (familyMembers < 1) throw new IllegalArgumentException("familyMembers must be at least 1");
        BigDecimal total = category == CardCategory.AAY
                ? aayKgPerFamily
                : phhKgPerPerson.multiply(BigDecimal.valueOf(familyMembers));
        BigDecimal rice = total.multiply(riceShare).setScale(2, RoundingMode.HALF_UP);
        BigDecimal wheat = total.subtract(rice).setScale(2, RoundingMode.HALF_UP);
        return new Entitlement(category, familyMembers, rice, wheat, total.setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal riceShare() { return riceShare; }

    /** Total monthly requirement of a group of cards (used by dashboards): AAY by card count, PHH by member count. */
    public BigDecimal groupTotalKg(CardCategory category, long cards, long members) {
        BigDecimal kg = category == CardCategory.AAY
                ? aayKgPerFamily.multiply(BigDecimal.valueOf(cards))
                : phhKgPerPerson.multiply(BigDecimal.valueOf(members));
        return kg.setScale(2, RoundingMode.HALF_UP);
    }
}
