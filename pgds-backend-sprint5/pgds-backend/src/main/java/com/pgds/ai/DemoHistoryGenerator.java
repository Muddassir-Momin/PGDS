package com.pgds.ai;

import com.pgds.common.ApiException;
import com.pgds.domain.*;
import com.pgds.entitlement.EntitlementService;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Creates ~5 months of synthetic distribution history for one shop (60 demo cards) and injects known anomalies
 * (night-time issues and a one-hour burst) so the AI features can be demonstrated and evaluated.
 * Rows are written directly (not through DistributionService) because they are back-dated.
 */
@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(name = "pgds.ai.demo-endpoint-enabled", havingValue = "true")
public class DemoHistoryGenerator {

    public record Result(int cards, int distributions, int injectedAnomalies) {}

    private final FairPriceShopRepository shops;
    private final RationCardRepository cards;
    private final DistributionRepository distributions;
    private final StockTransactionRepository txns;
    private final EntitlementService entitlement;
    private final Clock clock;

    private int receiptSeq = 0;

    public Result generate(Long fpsId) {
        FairPriceShop fps = shops.findById(fpsId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fair Price Shop not found"));
        if (cards.existsByCardNumber("DEMO-0001"))
            throw new ApiException(HttpStatus.CONFLICT, "Demo history was already generated");

        Random rnd = new Random(7);
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);

        List<RationCard> demo = new ArrayList<>();
        for (int i = 1; i <= 60; i++)
            demo.add(RationCard.builder().cardNumber(String.format("DEMO-%04d", i)).headOfFamily("Demo Family " + i)
                    .address("Demo address").district(fps.getDistrict()).category(CardCategory.PHH)
                    .familyMembers(2 + rnd.nextInt(5)).eligible(true).fps(fps).build());
        cards.saveAll(demo);

        List<Distribution> dist = new ArrayList<>();
        List<StockTransaction> ledger = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // normal history: each card collects 60-100% of its entitlement once a month during shop hours
        for (int m = 4; m >= 0; m--) {
            YearMonth ym = YearMonth.from(today).minusMonths(m);
            int lastDay = m == 0 ? today.getDayOfMonth() : 25;
            if (lastDay < 2) continue;
            for (RationCard c : demo) {
                for (GrainType g : List.of(GrainType.RICE, GrainType.WHEAT)) {
                    BigDecimal qty = entitled(c, g).multiply(BigDecimal.valueOf(0.6 + rnd.nextDouble() * 0.4))
                            .setScale(2, RoundingMode.HALF_UP);
                    LocalDateTime when = ym.atDay(1 + rnd.nextInt(lastDay)).atTime(9 + rnd.nextInt(8), rnd.nextInt(60));
                    total = total.add(qty);
                    add(dist, ledger, c, fps, g, qty, when, zone);
                }
            }
        }

        // injected anomalies, yesterday: 4 night-time issues and a burst of 30 issues in one hour
        LocalDate y = today.minusDays(1);
        int injected = 0;
        for (int i = 0; i < 4; i++) {
            RationCard c = demo.get(i);
            BigDecimal qty = entitled(c, GrainType.RICE);
            total = total.add(qty);
            add(dist, ledger, c, fps, GrainType.RICE, qty, y.atTime(i < 2 ? 23 : 2, 10 + i * 10), zone);
            injected++;
        }
        for (int i = 10; i < 25; i++) {
            RationCard c = demo.get(i);
            for (GrainType g : List.of(GrainType.RICE, GrainType.WHEAT)) {
                BigDecimal qty = entitled(c, g).multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
                total = total.add(qty);
                add(dist, ledger, c, fps, g, qty, y.atTime(15, (i - 10) * 2), zone);
                injected++;
            }
        }

        // opening receipt big enough to cover everything, so balances stay positive
        ledger.add(0, StockTransaction.builder().locationType(LocationType.FPS).fps(fps).grainType(GrainType.RICE)
                .txnType(StockTxnType.RECEIPT).quantityKg(total).remarks("Demo opening stock").createdBy("demo-generator")
                .createdAt(today.minusMonths(5).atStartOfDay(zone).toInstant()).build());
        distributions.saveAll(dist);
        txns.saveAll(ledger);
        return new Result(demo.size(), dist.size(), injected);
    }

    private BigDecimal entitled(RationCard c, GrainType g) {
        var e = entitlement.calculate(c.getCategory(), c.getFamilyMembers());
        return g == GrainType.RICE ? e.riceKg() : e.wheatKg();
    }

    private void add(List<Distribution> dist, List<StockTransaction> ledger, RationCard c, FairPriceShop fps,
                     GrainType g, BigDecimal qty, LocalDateTime when, ZoneId zone) {
        Instant at = when.atZone(zone).toInstant();
        String receipt = String.format("RCP-DEMO-%06d", ++receiptSeq);
        dist.add(Distribution.builder().receiptNo(receipt).rationCard(c).fps(fps).grainType(g).quantityKg(qty)
                .period(YearMonth.from(when).toString()).distributedBy("demo-generator").distributedAt(at).build());
        ledger.add(StockTransaction.builder().locationType(LocationType.FPS).fps(fps).grainType(g)
                .txnType(StockTxnType.DISTRIBUTION).quantityKg(qty).remarks("Receipt " + receipt)
                .createdBy("demo-generator").createdAt(at).build());
    }
}
