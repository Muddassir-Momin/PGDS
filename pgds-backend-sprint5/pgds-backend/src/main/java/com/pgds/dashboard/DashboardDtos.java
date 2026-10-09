package com.pgds.dashboard;

import com.pgds.domain.GrainType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    /** Stock vs the monthly demand of the shop's cards: coverage >= 1.5 HEALTHY, >= 1.0 ADEQUATE, >= 0.5 LOW, else CRITICAL. */
    public enum ShopStatus { HEALTHY, ADEQUATE, LOW, CRITICAL }

    public record Summary(String period, long totalBeneficiaries, long totalRationCards, long totalShops,
                          long totalWarehouses, BigDecimal warehouseStockKg, BigDecimal shopStockKg,
                          BigDecimal totalStockKg, double healthyShopPercent, long openComplaints,
                          BigDecimal distributedThisMonthKg,
                          long openAiAlerts, long highPriorityAiAlerts) {}

    public record GrainShare(GrainType grainType, BigDecimal kg, double percent) {}

    public record Trend(List<String> periods, Map<GrainType, List<BigDecimal>> series) {}

    public record ShopStock(Long fpsId, String shopCode, String name, String district, BigDecimal stockKg,
                            BigDecimal monthlyRequirementKg, double coverage, ShopStatus status) {}

    public record DistrictStock(String district, int shops, BigDecimal stockKg, Map<ShopStatus, Long> statusCounts) {}

    public record TopShop(String shopCode, String name, long beneficiaries, BigDecimal distributedKg,
                          BigDecimal stockKg, double performancePercent) {}
}
