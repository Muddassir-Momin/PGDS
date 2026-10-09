package com.pgds.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DemandModelTest {

    private static List<BigDecimal> series(int... v) {
        return java.util.Arrays.stream(v).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void risingHistoryPredictsAboveRecentMean() {
        var p = DemandModel.predict(series(100, 110, 120, 130, 140, 150), new BigDecimal("500"), null);
        assertEquals("TREND_BLEND", p.method());
        assertEquals("HIGH", p.confidence());
        assertEquals(152.0, p.predictedKg().doubleValue(), 0.01);   // 0.6*160 + 0.4*140
    }

    @Test
    void neverExceedsEntitlementRequirement() {
        var p = DemandModel.predict(series(900, 950, 1000, 1050), new BigDecimal("200"), null);
        assertEquals(0, p.predictedKg().compareTo(new BigDecimal("200")));
    }

    @Test
    void noHistoryFallsBackToAssumedUptake() {
        var p = DemandModel.predict(List.of(), new BigDecimal("200"), null);
        assertEquals("ENTITLEMENT_BASELINE", p.method());
        assertEquals("LOW", p.confidence());
        assertEquals(170.0, p.predictedKg().doubleValue(), 0.01);
    }

    @Test
    void shortHistoryUsesMean() {
        var p = DemandModel.predict(series(80, 100), new BigDecimal("500"), null);
        assertEquals("HISTORICAL_MEAN", p.method());
        assertEquals(90.0, p.predictedKg().doubleValue(), 0.01);
    }

    @Test
    void seasonalIndexScalesPrediction() {
        var base = DemandModel.predict(series(100, 100, 100, 100), new BigDecimal("1000"), null);
        var seasonal = DemandModel.predict(series(100, 100, 100, 100), new BigDecimal("1000"), 1.2);
        assertEquals(base.predictedKg().doubleValue() * 1.2, seasonal.predictedKg().doubleValue(), 0.02);
        assertTrue(seasonal.method().endsWith("+SEASONAL"));
    }

    @Test
    void recentSeriesTrimsMonthsBeforeFirstData() {
        YearMonth cur = YearMonth.of(2026, 10);
        var s = DemandPredictionService.recentSeries(Map.of(
                "2026-07", new BigDecimal("50"), "2026-08", new BigDecimal("60"), "2026-09", new BigDecimal("70")), cur);
        assertEquals(3, s.size());
        assertEquals(new BigDecimal("50"), s.get(0));
    }

    @Test
    void seasonalIndexNeedsEnoughHistory() {
        assertNull(DemandPredictionService.seasonalIndex(Map.of("2025-11", new BigDecimal("100")), YearMonth.of(2026, 11)));
    }
}
