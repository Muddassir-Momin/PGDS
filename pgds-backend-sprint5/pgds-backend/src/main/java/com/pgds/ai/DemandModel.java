package com.pgds.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Next-month demand for one shop and grain.
 *  - 3+ months of history: 60% linear trend (least squares) + 40% mean of last 3 months, optionally times a seasonal index
 *  - 1-2 months: historical mean
 *  - no history: 85% of the entitlement requirement of the shop's cards (assumed uptake)
 * The result never exceeds the monthly entitlement requirement, because distribution validation caps issues at that level.
 * The requirement itself already reflects current beneficiary counts.
 */
public final class DemandModel {
    private DemandModel() {}

    public record Prediction(BigDecimal predictedKg, String method, String confidence) {}

    public static Prediction predict(List<BigDecimal> history, BigDecimal requirement, Double seasonalIndex) {
        int n = history.size();
        double req = requirement == null ? 0 : requirement.doubleValue();
        double pred;
        String method;

        if (n >= 3) {
            double[] y = history.stream().mapToDouble(BigDecimal::doubleValue).toArray();
            double xMean = (n - 1) / 2.0, yMean = 0;
            for (double v : y) yMean += v;
            yMean /= n;
            double num = 0, den = 0;
            for (int i = 0; i < n; i++) { num += (i - xMean) * (y[i] - yMean); den += (i - xMean) * (i - xMean); }
            double slope = num / den;
            double intercept = yMean - slope * xMean;
            double trend = Math.max(0, intercept + slope * n);
            double last3 = (y[n - 1] + y[n - 2] + y[n - 3]) / 3.0;
            pred = 0.6 * trend + 0.4 * last3;
            method = "TREND_BLEND";
            if (seasonalIndex != null) { pred *= seasonalIndex; method += "+SEASONAL"; }
        } else if (n >= 1) {
            pred = history.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            method = "HISTORICAL_MEAN";
        } else {
            pred = req * 0.85;
            method = "ENTITLEMENT_BASELINE";
        }
        if (req > 0) pred = Math.min(pred, req);
        pred = Math.max(0, pred);
        String confidence = n >= 6 ? "HIGH" : n >= 3 ? "MEDIUM" : "LOW";
        return new Prediction(BigDecimal.valueOf(pred).setScale(2, RoundingMode.HALF_UP), method, confidence);
    }
}
