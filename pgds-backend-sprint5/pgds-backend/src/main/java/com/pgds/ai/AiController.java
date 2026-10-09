package com.pgds.ai;

import com.pgds.ai.AiDtos.*;
import com.pgds.common.PageResponse;
import com.pgds.complaint.ComplaintClassifier.Classification;
import com.pgds.complaint.WekaComplaintClassifier;
import com.pgds.domain.AlertStatus;
import com.pgds.domain.LocationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/officer/** = ADMIN + OFFICER, /api/audit/** = ADMIN + AUDITOR (see SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class AiController {

    private final AlertService alerts;
    private final AiAnalysisService analysis;
    private final StockForecastService stockForecast;
    private final DemandPredictionService demand;
    private final WekaComplaintClassifier classifier;

    @GetMapping({"/api/officer/ai/alerts", "/api/audit/ai/alerts"})
    public PageResponse<AlertView> alerts(@RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable p) {
        return alerts.list(status, p);
    }

    @PatchMapping("/api/officer/ai/alerts/{id}")
    public AlertView review(Authentication a, @PathVariable Long id, @Valid @RequestBody AlertUpdate u) {
        return alerts.update(id, a.getName(), u);
    }

    /** Runs anomaly detection, stock-exhaustion and demand-shortfall checks now. */
    @PostMapping("/api/officer/ai/run")
    public RunSummary run() { return analysis.runAll(); }

    @GetMapping("/api/officer/ai/stock-forecast")
    public List<StockForecast> stockForecast(@RequestParam(defaultValue = "FPS") LocationType type) {
        return stockForecast.forecast(type);
    }

    @GetMapping("/api/officer/ai/demand")
    public List<DemandForecast> demand() { return demand.forecastNextMonth(); }

    @PostMapping("/api/officer/ai/classify")
    public Classification classify(@Valid @RequestBody ClassifyRequest r) { return classifier.classify(r.text()); }

    @GetMapping("/api/officer/ai/model-info")
    public WekaComplaintClassifier.ModelInfo modelInfo() { return classifier.info(); }
}
