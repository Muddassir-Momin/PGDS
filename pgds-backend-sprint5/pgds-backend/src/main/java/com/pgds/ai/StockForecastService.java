package com.pgds.ai;

import com.pgds.ai.AiDtos.*;
import com.pgds.domain.FairPriceShop;
import com.pgds.domain.LocationType;
import com.pgds.domain.Warehouse;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Days until stock runs out = current balance / average daily outflow over the last 30 days. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockForecastService {

    static final int WINDOW_DAYS = 30;

    private final StockTransactionRepository txns;
    private final FairPriceShopRepository shops;
    private final WarehouseRepository warehouses;
    private final Clock clock;

    public List<StockForecast> forecast(LocationType type) {
        Instant since = Instant.now(clock).minus(WINDOW_DAYS, ChronoUnit.DAYS);
        boolean fps = type == LocationType.FPS;
        List<LocationGrainBalance> balances = fps ? txns.fpsBalances() : txns.warehouseBalances();
        List<LocationGrainBalance> outflow = fps ? txns.fpsOutflowSince(since) : txns.warehouseOutflowSince(since);

        Map<String, BigDecimal> out = new HashMap<>();
        outflow.forEach(r -> out.merge(r.locationId() + "|" + r.grainType(), nz(r.kg()), BigDecimal::add));

        Map<Long, String> names = fps
                ? shops.findAll().stream().collect(Collectors.toMap(FairPriceShop::getId, s -> s.getShopCode() + " - " + s.getName()))
                : warehouses.findAll().stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));

        LocalDate today = LocalDate.now(clock);
        return balances.stream().map(b -> {
            BigDecimal stock = nz(b.kg());
            BigDecimal daily = out.getOrDefault(b.locationId() + "|" + b.grainType(), BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(WINDOW_DAYS), 4, RoundingMode.HALF_UP);
            Double days = null;
            LocalDate date = null;
            if (daily.signum() > 0) {
                days = stock.max(BigDecimal.ZERO).divide(daily, 1, RoundingMode.HALF_UP).doubleValue();
                date = today.plusDays((long) Math.floor(days));
            }
            return new StockForecast(type, b.locationId(), names.getOrDefault(b.locationId(), "#" + b.locationId()),
                    b.grainType(), stock, daily.setScale(2, RoundingMode.HALF_UP), days, date, levelFor(days));
        }).sorted(Comparator.comparingDouble(f -> f.daysToExhaustion() == null ? Double.MAX_VALUE : f.daysToExhaustion())).toList();
    }

    public static ForecastLevel levelFor(Double days) {
        if (days == null) return ForecastLevel.OK;
        if (days <= 3) return ForecastLevel.CRITICAL;
        if (days <= 7) return ForecastLevel.WARNING;
        if (days <= 14) return ForecastLevel.WATCH;
        return ForecastLevel.OK;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
