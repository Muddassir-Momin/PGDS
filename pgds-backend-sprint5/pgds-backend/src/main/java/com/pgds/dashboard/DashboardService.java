package com.pgds.dashboard;

import com.pgds.dashboard.DashboardDtos.*;
import com.pgds.domain.AlertStatus;
import com.pgds.domain.ComplaintStatus;
import com.pgds.domain.Priority;
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
import java.util.stream.Collectors;

/** Read-only aggregates for the officer dashboard. All figures are computed from the ledger and distribution tables. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal HEALTHY_AT = new BigDecimal("1.5");
    private static final BigDecimal ADEQUATE_AT = BigDecimal.ONE;
    private static final BigDecimal LOW_AT = new BigDecimal("0.5");

    private static final List<AlertStatus> OPEN_ALERTS = List.of(AlertStatus.OPEN, AlertStatus.UNDER_REVIEW);

    private final AiAlertRepository aiAlerts;
    private final BeneficiaryRepository beneficiaries;
    private final RationCardRepository cards;
    private final FairPriceShopRepository shops;
    private final WarehouseRepository warehouses;
    private final ComplaintRepository complaints;
    private final DistributionRepository distributions;
    private final StockTransactionRepository txns;
    private final EntitlementService entitlement;
    private final Clock clock;

    public Summary summary() {
        String period = period();
        BigDecimal wh = txns.warehouseBalances().stream().map(r -> nz(r.kg())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shop = txns.fpsBalances().stream().map(r -> nz(r.kg())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ShopStock> statuses = shopStatuses(null);
        long ok = statuses.stream().filter(s -> s.status() == ShopStatus.HEALTHY || s.status() == ShopStatus.ADEQUATE).count();
        double healthyPct = statuses.isEmpty() ? 0 : round1(100.0 * ok / statuses.size());
        long open = complaints.countByStatus(ComplaintStatus.OPEN) + complaints.countByStatus(ComplaintStatus.IN_PROGRESS);
        return new Summary(period, beneficiaries.count(), cards.count(), shops.count(), warehouses.count(),
                wh, shop, wh.add(shop), healthyPct, open, nz(distributions.totalForPeriod(period)),
                aiAlerts.countByStatusIn(OPEN_ALERTS), aiAlerts.countBySeverityAndStatusIn(Priority.HIGH, OPEN_ALERTS));
    }

    /** Donut chart: stock across all warehouses and shops, by grain. */
    public List<GrainShare> stockByGrain() {
        Map<GrainType, BigDecimal> m = new EnumMap<>(GrainType.class);
        for (GrainType g : GrainType.values()) m.put(g, BigDecimal.ZERO);
        txns.warehouseBalances().forEach(r -> m.merge(r.grainType(), nz(r.kg()), BigDecimal::add));
        txns.fpsBalances().forEach(r -> m.merge(r.grainType(), nz(r.kg()), BigDecimal::add));
        BigDecimal total = m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return m.entrySet().stream().map(e -> new GrainShare(e.getKey(), e.getValue(),
                total.signum() == 0 ? 0 : round1(e.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(total, 4, RoundingMode.HALF_UP).doubleValue()))).toList();
    }

    /** Line chart: kg distributed per month and grain, with empty months filled with zero. */
    public Trend trend(int months) {
        int n = Math.max(1, Math.min(months, 24));
        YearMonth now = YearMonth.now(clock);
        List<String> periods = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) periods.add(now.minusMonths(i).toString());
        Map<GrainType, Map<String, BigDecimal>> data = new EnumMap<>(GrainType.class);
        for (GrainType g : GrainType.values()) data.put(g, new HashMap<>());
        distributions.trendSince(periods.get(0)).forEach(r -> data.get(r.grainType()).put(r.period(), nz(r.kg())));
        Map<GrainType, List<BigDecimal>> series = new EnumMap<>(GrainType.class);
        for (GrainType g : GrainType.values())
            series.put(g, periods.stream().map(p -> data.get(g).getOrDefault(p, BigDecimal.ZERO)).toList());
        return new Trend(periods, series);
    }

    /** Per-shop stock health (also feeds the district map colouring). A shop with no assigned cards counts as HEALTHY. */
    public List<ShopStock> shopStatuses(String district) {
        Map<Long, BigDecimal> stock = stockByShop();
        Map<Long, BigDecimal> need = requirementByShop();
        var list = (district == null || district.isBlank()) ? shops.findAll() : shops.findByDistrictIgnoreCase(district);
        return list.stream().map(s -> {
            BigDecimal st = stock.getOrDefault(s.getId(), BigDecimal.ZERO);
            BigDecimal req = need.getOrDefault(s.getId(), BigDecimal.ZERO);
            double coverage = req.signum() == 0 ? 0 : st.divide(req, 2, RoundingMode.HALF_UP).doubleValue();
            return new ShopStock(s.getId(), s.getShopCode(), s.getName(), s.getDistrict(), st, req, coverage, statusFor(st, req));
        }).toList();
    }

    public List<DistrictStock> districtStock() {
        Map<String, List<ShopStock>> byDistrict = shopStatuses(null).stream()
                .collect(Collectors.groupingBy(ShopStock::district, TreeMap::new, Collectors.toList()));
        return byDistrict.entrySet().stream().map(e -> {
            Map<ShopStatus, Long> counts = new EnumMap<>(ShopStatus.class);
            for (ShopStatus s : ShopStatus.values()) counts.put(s, 0L);
            e.getValue().forEach(s -> counts.merge(s.status(), 1L, Long::sum));
            BigDecimal total = e.getValue().stream().map(ShopStock::stockKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new DistrictStock(e.getKey(), e.getValue().size(), total, counts);
        }).toList();
    }

    /** Performance = kg issued this month / monthly requirement of the shop's eligible cards (capped at 100%). */
    public List<TopShop> topShops(int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        Map<Long, BigDecimal> done = distributions.distributedByShop(period()).stream()
                .collect(Collectors.toMap(ShopSum::fpsId, r -> nz(r.kg()), BigDecimal::add));
        Map<Long, Long> people = beneficiaries.countByShop().stream()
                .collect(Collectors.toMap(ShopCount::fpsId, ShopCount::count, Long::sum));
        Map<Long, BigDecimal> need = requirementByShop();
        Map<Long, BigDecimal> stock = stockByShop();
        return shops.findAll().stream().map(s -> {
            BigDecimal d = done.getOrDefault(s.getId(), BigDecimal.ZERO);
            BigDecimal req = need.getOrDefault(s.getId(), BigDecimal.ZERO);
            double perf = req.signum() == 0 ? 0
                    : Math.min(100.0, round1(d.multiply(BigDecimal.valueOf(100)).divide(req, 4, RoundingMode.HALF_UP).doubleValue()));
            return new TopShop(s.getShopCode(), s.getName(), people.getOrDefault(s.getId(), 0L), d,
                    stock.getOrDefault(s.getId(), BigDecimal.ZERO), perf);
        }).sorted(Comparator.comparingDouble(TopShop::performancePercent).reversed()).limit(n).toList();
    }

    public static ShopStatus statusFor(BigDecimal stock, BigDecimal requirement) {
        if (requirement == null || requirement.signum() == 0) return ShopStatus.HEALTHY;
        BigDecimal coverage = nz(stock).divide(requirement, 4, RoundingMode.HALF_UP);
        if (coverage.compareTo(HEALTHY_AT) >= 0) return ShopStatus.HEALTHY;
        if (coverage.compareTo(ADEQUATE_AT) >= 0) return ShopStatus.ADEQUATE;
        if (coverage.compareTo(LOW_AT) >= 0) return ShopStatus.LOW;
        return ShopStatus.CRITICAL;
    }

    // ---------- helpers ----------

    private Map<Long, BigDecimal> stockByShop() {
        Map<Long, BigDecimal> m = new HashMap<>();
        txns.fpsBalances().forEach(r -> m.merge(r.locationId(), nz(r.kg()), BigDecimal::add));
        return m;
    }

    private Map<Long, BigDecimal> requirementByShop() {
        Map<Long, BigDecimal> m = new HashMap<>();
        cards.shopDemand().forEach(r -> m.merge(r.fpsId(),
                entitlement.groupTotalKg(r.category(), nz(r.cards()), nz(r.members())), BigDecimal::add));
        return m;
    }

    private String period() { return YearMonth.now(clock).toString(); }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static long nz(Long v) { return v == null ? 0 : v; }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
