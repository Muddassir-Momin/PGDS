package com.pgds.entitlement;

import com.pgds.domain.CardCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntitlementServiceTest {

    private final EntitlementService svc =
            new EntitlementService(new BigDecimal("35"), new BigDecimal("5"), new BigDecimal("0.5"));

    @Test
    void phhIsFiveKgPerPerson() {
        var e = svc.calculate(CardCategory.PHH, 4);
        assertEquals(new BigDecimal("20.00"), e.totalKg());
        assertEquals(new BigDecimal("10.00"), e.riceKg());
        assertEquals(new BigDecimal("10.00"), e.wheatKg());
    }

    @Test
    void aayIsFixedPerFamilyRegardlessOfSize() {
        assertEquals(new BigDecimal("35.00"), svc.calculate(CardCategory.AAY, 2).totalKg());
        assertEquals(new BigDecimal("35.00"), svc.calculate(CardCategory.AAY, 9).totalKg());
    }

    @Test
    void riceAndWheatAlwaysAddUpToTotal() {
        var e = svc.calculate(CardCategory.AAY, 3);
        assertEquals(e.totalKg(), e.riceKg().add(e.wheatKg()));
    }

    @Test
    void zeroMembersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> svc.calculate(CardCategory.PHH, 0));
    }
}
