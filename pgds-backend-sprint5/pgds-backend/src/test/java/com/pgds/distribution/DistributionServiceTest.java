package com.pgds.distribution;

import com.pgds.common.ApiException;
import com.pgds.distribution.DistributionDtos.*;
import com.pgds.domain.*;
import com.pgds.entitlement.EntitlementService;
import com.pgds.repo.*;
import com.pgds.security.AccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionServiceTest {

    @Mock FairPriceShopRepository shops;
    @Mock RationCardRepository cards;
    @Mock DistributionRepository distributions;
    @Mock StockTransactionRepository txns;
    @Mock AccessGuard access;

    DistributionService service;
    FairPriceShop fps;
    RationCard card;

    @BeforeEach
    void setUp() {
        var entitlement = new EntitlementService(new BigDecimal("35"), new BigDecimal("5"), new BigDecimal("0.5"));
        var clock = Clock.fixed(Instant.parse("2026-10-08T05:00:00Z"), ZoneId.of("Asia/Kolkata"));
        service = new DistributionService(shops, cards, distributions, txns, entitlement, access, clock);

        fps = FairPriceShop.builder().id(10L).shopCode("FPS101").name("Ganesh Ration Shop").district("Pune").build();
        // PHH, 4 members -> 10 kg rice + 10 kg wheat per month
        card = RationCard.builder().id(5L).cardNumber("C1").headOfFamily("Ramesh").district("Pune")
                .category(CardCategory.PHH).familyMembers(4).eligible(true).fps(fps).build();
        lenient().when(shops.findByIdForUpdate(10L)).thenReturn(Optional.of(fps));
        lenient().when(cards.findByCardNumber("C1")).thenReturn(Optional.of(card));
    }

    private DistributionRequest req(GrainType g, String kg) {
        return new DistributionRequest("C1", List.of(new DistributionItem(g, new BigDecimal(kg))));
    }

    @Test
    void issuesGrainAndWritesLedgerEntries() {
        when(distributions.sumForCard(5L, "2026-10", GrainType.RICE)).thenReturn(BigDecimal.ZERO);
        when(txns.fpsBalance(10L, GrainType.RICE)).thenReturn(new BigDecimal("500"));

        ReceiptView r = service.distribute("dealer", 10L, req(GrainType.RICE, "10"));

        assertTrue(r.receiptNo().startsWith("RCP-20261008-FPS101-"));
        assertEquals("2026-10", r.period());
        assertEquals(1, r.items().size());
        @SuppressWarnings("unchecked") ArgumentCaptor<List<StockTransaction>> cap = ArgumentCaptor.forClass(List.class);
        verify(txns).saveAll(cap.capture());
        assertEquals(StockTxnType.DISTRIBUTION, cap.getValue().get(0).getTxnType());
        verify(distributions).saveAll(any());
    }

    @Test
    void rejectsQuantityAboveRemainingEntitlement() {
        when(distributions.sumForCard(5L, "2026-10", GrainType.RICE)).thenReturn(new BigDecimal("8"));
        var ex = assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, req(GrainType.RICE, "5")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("remaining entitlement of 2"));
        verify(txns, never()).saveAll(any());
    }

    @Test
    void rejectsWhenShopStockIsInsufficient() {
        when(distributions.sumForCard(5L, "2026-10", GrainType.WHEAT)).thenReturn(BigDecimal.ZERO);
        when(txns.fpsBalance(10L, GrainType.WHEAT)).thenReturn(new BigDecimal("3"));
        var ex = assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, req(GrainType.WHEAT, "5")));
        assertTrue(ex.getMessage().startsWith("Insufficient stock"));
        verify(distributions, never()).saveAll(any());
    }

    @Test
    void rejectsIneligibleCard() {
        card.setEligible(false);
        assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, req(GrainType.RICE, "1")));
    }

    @Test
    void rejectsCardServedByAnotherShop() {
        card.setFps(FairPriceShop.builder().id(99L).shopCode("FPS999").name("Other").district("Pune").build());
        var ex = assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, req(GrainType.RICE, "1")));
        assertTrue(ex.getMessage().contains("not assigned"));
    }

    @Test
    void rejectsGrainWithNoEntitlement() {
        var ex = assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, req(GrainType.OTHER, "1")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rejectsDuplicateGrainInOneRequest() {
        when(distributions.sumForCard(5L, "2026-10", GrainType.RICE)).thenReturn(BigDecimal.ZERO);
        when(txns.fpsBalance(10L, GrainType.RICE)).thenReturn(new BigDecimal("500"));
        var dup = new DistributionRequest("C1", List.of(
                new DistributionItem(GrainType.RICE, new BigDecimal("2")),
                new DistributionItem(GrainType.RICE, new BigDecimal("2"))));
        assertThrows(ApiException.class, () -> service.distribute("dealer", 10L, dup));
    }
}
