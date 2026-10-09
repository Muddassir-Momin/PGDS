package com.pgds.ai;

import com.pgds.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AiDtos {
    private AiDtos() {}

    public record AlertView(Long id, AlertType type, Priority severity, AlertStatus status, Long fpsId, String shopCode,
                            String title, String details, Double score, Instant detectedAt,
                            String reviewedBy, String reviewNote) {}

    public record AlertUpdate(@NotNull AlertStatus status, @Size(max = 500) String note) {}

    public record RunSummary(int anomalyAlerts, int stockAlerts, int shortfallAlerts, Instant ranAt) {}

    public enum ForecastLevel { CRITICAL, WARNING, WATCH, OK }

    /** daysToExhaustion is null when nothing left the location in the last 30 days. */
    public record StockForecast(LocationType locationType, Long locationId, String name, GrainType grainType,
                                BigDecimal stockKg, BigDecimal dailyUseKg, Double daysToExhaustion,
                                LocalDate exhaustionDate, ForecastLevel level) {}

    public record DemandForecast(Long fpsId, String shopCode, String shopName, GrainType grainType, String forPeriod,
                                 BigDecimal predictedKg, BigDecimal requirementKg, BigDecimal currentStockKg,
                                 BigDecimal shortfallKg, int historyMonths, String method, String confidence) {}

    public record ClassifyRequest(@NotBlank String text) {}
}
