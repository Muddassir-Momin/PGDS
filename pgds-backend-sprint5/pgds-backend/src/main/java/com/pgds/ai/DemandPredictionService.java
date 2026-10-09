package com.pgds.ai;

import com.pgds.ai.AiDtos.DemandForecast;
import com.pgds.domain.GrainType;
import com.pgds.entitlement.EntitlementService;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandPredictionService {

    private static final int HISTORY_MONTHS = 24;
    private static final int TREND_MONTHS = 6;
    private static final List<GrainType> GRAINS = List.of(GrainType.RICE, GrainType.WHEAT);

    private final FairPriceShopRepository shops;
    private final RationCardRepository cards;
    private final DistributionRepository distributions;
    private final StockTransactionRepository txns;
    private final EntitlementService entitlement;
    private final Clock clock;

    public List<DemandForecast> forecastNextMonth() {
        YearMonth cur = YearMonth.now(clock);
        YearMonth next = cur.plusMonths(1);

        Map<String, Map<String, BigDecimal>> history = new HashMap<>();
        distributions.monthlyByShop(cur.minusMonths(HISTORY_MONTHS).toString(), cur.toString())
                .forEach(r -> history.computeIfAbsent(key(r.fpsId(), r.grainType()), k -> new HashMap<>())
                        .put(r.period(), r.kg() == null ? BigDecimal.ZERO : r.kg()));

        Map<Long, BigDecimal> need = new HashMap<>();
        cards.shopDemand().forEach(r -> need.merge(r.fpsId(), entitlement.groupTotalKg(r.category(),
                r.cards() == null ? 0 : r.cards(), r.members() == null ? 0 : r.members()), BigDecimal::add));

        Map<String, BigDecimal> stock = new HashMap<>();
        txns.fpsBalances().forEach(r -> stock.merge(key(r.locationId(), r.grainType()),
                r.kg() == null ? BigDecimal.ZERO : r.kg(), BigDecimal::add));

        List<DemandForecast> result = new ArrayList<>();
        for (var shop : shops.findAll()) {
            BigDecimal total = need.getOrDefault(shop.getId(), BigDecimal.ZERO);
            for (GrainType g : GRAINS) {
                BigDecimal rice = total.multiply(entitlement.riceShare()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal req = g == GrainType.RICE ? rice : total.subtract(rice);
                Map<String, BigDecimal> series = history.getOrDefault(key(shop.getId(), g), Map.of());

                var p = DemandModel.predict(recentSeries(series, cur), req, seasonalIndex(series, next));
                BigDecimal st = stock.getOrDefault(key(shop.getId(), g), BigDecimal.ZERO);
                result.add(new DemandForecast(shop.getId(), shop.getShopCode(), shop.getName(), g, next.toString(),
                        p.predictedKg(), req, st, p.predictedKg().subtract(st).max(BigDecimal.ZERO),
                        recentSeries(series, cur).size(), p.method(), p.confidence()));
            }
        }
        return result;
    }

    /** Last completed months in order, starting at the first month that has data. */
    static List<BigDecimal> recentSeries(Map<String, BigDecimal> byPeriod, YearMonth current) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = TREND_MONTHS; i >= 1; i--)
            values.add(byPeriod.getOrDefault(current.minusMonths(i).toString(), BigDecimal.ZERO));
        int first = 0;
        while (first < values.size() && values.get(first).signum() == 0) first++;
        return new ArrayList<>(values.subList(first, values.size()));
    }

    /** Same calendar month last year vs the average of the 12 months ending there; null if not enough data. */
    static Double seasonalIndex(Map<String, BigDecimal> byPeriod, YearMonth next) {
        BigDecimal lastYear = byPeriod.get(next.minusMonths(12).toString());
        if (lastYear == null || lastYear.signum() == 0) return null;
        double sum = 0;
        int count = 0;
        for (int i = 12; i <= 23; i++) {
            BigDecimal v = byPeriod.get(next.minusMonths(i).toString());
            if (v != null && v.signum() > 0) { sum += v.doubleValue(); count++; }
        }
        if (count < 6) return null;
        return Math.max(0.85, Math.min(1.2, lastYear.doubleValue() / (sum / count)));
    }

    private static String key(Long fpsId, GrainType g) { return fpsId + "|" + g; }
}
