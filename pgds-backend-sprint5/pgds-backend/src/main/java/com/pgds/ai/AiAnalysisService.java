package com.pgds.ai;

import com.pgds.ai.AiDtos.*;
import com.pgds.domain.*;
import com.pgds.repo.FairPriceShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Locale;

/** Runs all AI checks and turns findings into de-duplicated alerts. Called nightly and on demand. */
@Service
@RequiredArgsConstructor
@Transactional
public class AiAnalysisService {

    private final AnomalyDetectionService anomalies;
    private final StockForecastService stockForecast;
    private final DemandPredictionService demand;
    private final AlertService alerts;
    private final FairPriceShopRepository shops;
    private final Clock clock;

    public RunSummary runAll() {
        int a = anomalies.detectAndAlert();
        int s = stockAlerts();
        int d = shortfallAlerts();
        return new RunSummary(a, s, d, Instant.now(clock));
    }

    private int stockAlerts() {
        int raised = 0;
        String month = YearMonth.now(clock).toString();
        for (LocationType type : LocationType.values()) {
            for (StockForecast f : stockForecast.forecast(type)) {
                if (f.level() != ForecastLevel.CRITICAL && f.level() != ForecastLevel.WARNING) continue;
                FairPriceShop shop = type == LocationType.FPS ? shops.getReferenceById(f.locationId()) : null;
                String key = "LOWSTOCK:" + type + ":" + f.locationId() + ":" + f.grainType() + ":" + month + ":" + f.level();
                String title = f.grainType() + " stock at " + f.name() + " may run out in "
                        + String.format(Locale.ROOT, "%.0f", f.daysToExhaustion()) + " days";
                String details = "Current stock " + f.stockKg().toPlainString() + " kg, average use "
                        + f.dailyUseKg().toPlainString() + " kg/day over the last 30 days. Estimated exhaustion: "
                        + f.exhaustionDate() + ".";
                if (alerts.raise(key, AlertType.LOW_STOCK,
                        f.level() == ForecastLevel.CRITICAL ? Priority.HIGH : Priority.MEDIUM, shop, title, details, null))
                    raised++;
            }
        }
        return raised;
    }

    private int shortfallAlerts() {
        int raised = 0;
        for (DemandForecast f : demand.forecastNextMonth()) {
            if (f.shortfallKg().signum() <= 0 || f.predictedKg().signum() <= 0) continue;
            boolean severe = f.shortfallKg().compareTo(f.predictedKg().multiply(new BigDecimal("0.5"))) >= 0;
            String key = "SHORTFALL:" + f.fpsId() + ":" + f.grainType() + ":" + f.forPeriod();
            String title = "Predicted " + f.grainType() + " shortfall of " + f.shortfallKg().toPlainString()
                    + " kg at " + f.shopCode() + " for " + f.forPeriod();
            String details = "Predicted demand " + f.predictedKg().toPlainString() + " kg vs current stock "
                    + f.currentStockKg().toPlainString() + " kg (method " + f.method() + ", confidence " + f.confidence() + ").";
            if (alerts.raise(key, AlertType.DEMAND_SHORTFALL, severe ? Priority.HIGH : Priority.MEDIUM,
                    shops.getReferenceById(f.fpsId()), title, details, null)) raised++;
        }
        return raised;
    }
}
