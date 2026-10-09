package com.pgds.distribution;

import com.pgds.common.ApiException;
import com.pgds.common.PageResponse;
import com.pgds.distribution.DistributionDtos.*;
import com.pgds.domain.*;
import com.pgds.entitlement.EntitlementService;
import com.pgds.entitlement.EntitlementService.Entitlement;
import com.pgds.repo.*;
import com.pgds.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Issues grain to a ration card. Validation order:
 * card exists + eligible + served by this shop -> no duplicate grains -> within remaining monthly entitlement
 * -> shop has stock. The shop row is locked first, so two dealers' terminals cannot overdraw stock or
 * double-issue the same entitlement. Success writes Distribution rows and DISTRIBUTION ledger entries together.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DistributionService {

    private static final List<GrainType> ENTITLED_GRAINS = List.of(GrainType.RICE, GrainType.WHEAT);

    private final FairPriceShopRepository shops;
    private final RationCardRepository cards;
    private final DistributionRepository distributions;
    private final StockTransactionRepository txns;
    private final EntitlementService entitlement;
    private final AccessGuard access;
    private final Clock clock;

    public ReceiptView distribute(String username, Long fpsId, DistributionRequest req) {
        access.checkFps(username, fpsId);
        FairPriceShop fps = shops.findByIdForUpdate(fpsId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fair Price Shop not found"));
        RationCard card = servedCard(fps, req.cardNumber());
        if (!card.isEligible())
            throw new ApiException(HttpStatus.CONFLICT, "Ration card is not eligible for distribution");

        String period = currentPeriod();
        Entitlement ent = entitlement.calculate(card.getCategory(), card.getFamilyMembers());
        String receiptNo = newReceiptNo(fps);

        Set<GrainType> seen = EnumSet.noneOf(GrainType.class);
        List<Distribution> rows = new ArrayList<>();
        List<StockTransaction> ledger = new ArrayList<>();

        for (DistributionItem item : req.items()) {
            GrainType grain = item.grainType();
            if (!seen.add(grain))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Grain " + grain + " appears more than once");
            BigDecimal entitled = entitledFor(ent, grain);
            if (entitled.signum() == 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "No monthly entitlement for " + grain);

            BigDecimal remaining = entitled.subtract(nz(distributions.sumForCard(card.getId(), period, grain)));
            if (item.quantityKg().compareTo(remaining) > 0)
                throw new ApiException(HttpStatus.CONFLICT, "Requested " + item.quantityKg().toPlainString() + " kg " + grain
                        + " exceeds remaining entitlement of " + remaining.max(BigDecimal.ZERO).toPlainString() + " kg for " + period);

            BigDecimal stock = nz(txns.fpsBalance(fpsId, grain));
            if (stock.compareTo(item.quantityKg()) < 0)
                throw new ApiException(HttpStatus.CONFLICT, "Insufficient stock at shop for " + grain
                        + ": available " + stock.toPlainString() + " kg");

            rows.add(Distribution.builder().receiptNo(receiptNo).rationCard(card).fps(fps).grainType(grain)
                    .quantityKg(item.quantityKg()).period(period).distributedBy(username).build());
            ledger.add(StockTransaction.builder().locationType(LocationType.FPS).fps(fps).grainType(grain)
                    .txnType(StockTxnType.DISTRIBUTION).quantityKg(item.quantityKg())
                    .remarks("Receipt " + receiptNo).createdBy(username).build());
        }
        distributions.saveAll(rows);
        txns.saveAll(ledger);
        return toReceipt(rows);
    }

    /** Entitled / distributed / remaining for the current month. */
    @Transactional(readOnly = true)
    public EntitlementBalance balance(RationCard card) {
        String period = currentPeriod();
        Entitlement ent = entitlement.calculate(card.getCategory(), card.getFamilyMembers());
        List<GrainBalance> grains = ENTITLED_GRAINS.stream().map(g -> {
            BigDecimal entitled = entitledFor(ent, g);
            BigDecimal done = nz(distributions.sumForCard(card.getId(), period, g));
            return new GrainBalance(g, entitled, done, entitled.subtract(done).max(BigDecimal.ZERO));
        }).toList();
        return new EntitlementBalance(card.getCardNumber(), card.getHeadOfFamily(), period, card.isEligible(), grains);
    }

    /** Dealer lookup before issuing grain. */
    @Transactional(readOnly = true)
    public EntitlementBalance balanceAtShop(String username, Long fpsId, String cardNumber) {
        access.checkFps(username, fpsId);
        FairPriceShop fps = shops.findById(fpsId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fair Price Shop not found"));
        return balance(servedCard(fps, cardNumber));
    }

    @Transactional(readOnly = true)
    public List<ReceiptView> historyForCard(Long cardId) {
        var rows = distributions.findTop200ByRationCardIdOrderByDistributedAtDesc(cardId);
        Map<String, List<Distribution>> byReceipt = rows.stream()
                .collect(Collectors.groupingBy(Distribution::getReceiptNo, LinkedHashMap::new, Collectors.toList()));
        return byReceipt.values().stream().map(this::toReceipt).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionRow> shopHistory(String username, Long fpsId, Pageable p) {
        access.checkFps(username, fpsId);
        return PageResponse.of(distributions.findByFpsIdOrderByDistributedAtDesc(fpsId, p).map(d ->
                new DistributionRow(d.getId(), d.getReceiptNo(), d.getRationCard().getCardNumber(), d.getGrainType(),
                        d.getQuantityKg(), d.getPeriod(), d.getDistributedAt(), d.getDistributedBy())));
    }

    // ---------- helpers ----------

    private RationCard servedCard(FairPriceShop fps, String cardNumber) {
        RationCard card = cards.findByCardNumber(cardNumber.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ration card not found"));
        if (card.getFps() == null || !card.getFps().getId().equals(fps.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "This ration card is not assigned to this shop");
        return card;
    }

    private String currentPeriod() { return YearMonth.now(clock).toString(); }   // e.g. 2026-10

    private String newReceiptNo(FairPriceShop fps) {
        String prefix = "RCP-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + fps.getShopCode() + "-";
        String no;
        do { no = prefix + String.format("%05d", ThreadLocalRandom.current().nextInt(100_000)); }
        while (distributions.existsByReceiptNo(no));
        return no;
    }

    private static BigDecimal entitledFor(Entitlement e, GrainType g) {
        return switch (g) { case RICE -> e.riceKg(); case WHEAT -> e.wheatKg(); default -> BigDecimal.ZERO; };
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private ReceiptView toReceipt(List<Distribution> rows) {
        Distribution f = rows.get(0);
        return new ReceiptView(f.getReceiptNo(), f.getFps().getShopCode(), f.getFps().getName(),
                f.getRationCard().getCardNumber(), f.getRationCard().getHeadOfFamily(), f.getPeriod(),
                f.getDistributedAt(), f.getDistributedBy(),
                rows.stream().map(r -> new ReceiptItem(r.getGrainType(), r.getQuantityKg())).toList());
    }
}
