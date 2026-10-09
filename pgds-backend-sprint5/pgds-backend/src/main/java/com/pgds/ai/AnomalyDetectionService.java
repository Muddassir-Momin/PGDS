package com.pgds.ai;

import com.pgds.domain.*;
import com.pgds.entitlement.EntitlementService;
import com.pgds.repo.DistributionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Flags suspicious distribution events.
 * 1. Isolation Forest over four features per issue:
 *    quantity / entitlement, issues for the same card+grain this month, hour of day, issues by the same shop in that hour.
 * 2. Hard rules that always apply: off-hours issue (before 06:00 / from 22:00) and shop velocity (>= 25 issues in one hour).
 * Training uses the last windowDays of history; only events in the last lookbackDays can raise new alerts.
 */
@Service
@Transactional
public class AnomalyDetectionService {

    static final int VELOCITY_LIMIT = 25;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    private final DistributionRepository distributions;
    private final EntitlementService entitlement;
    private final AlertService alerts;
    private final Clock clock;
    private final int windowDays, lookbackDays, minSamples, trees, sampleSize;
    private final double threshold;

    public AnomalyDetectionService(DistributionRepository distributions, EntitlementService entitlement,
                                   AlertService alerts, Clock clock,
                                   @Value("${pgds.ai.window-days:90}") int windowDays,
                                   @Value("${pgds.ai.alert-lookback-days:7}") int lookbackDays,
                                   @Value("${pgds.ai.anomaly-threshold:0.65}") double threshold,
                                   @Value("${pgds.ai.min-training-samples:50}") int minSamples,
                                   @Value("${pgds.ai.trees:100}") int trees,
                                   @Value("${pgds.ai.sample-size:256}") int sampleSize) {
        this.distributions = distributions;
        this.entitlement = entitlement;
        this.alerts = alerts;
        this.clock = clock;
        this.windowDays = windowDays;
        this.lookbackDays = lookbackDays;
        this.threshold = threshold;
        this.minSamples = minSamples;
        this.trees = trees;
        this.sampleSize = sampleSize;
    }

    /** Returns the number of new alerts raised. */
    public int detectAndAlert() {
        Instant now = Instant.now(clock);
        List<Distribution> rows = distributions.findSince(now.minus(windowDays, ChronoUnit.DAYS));
        if (rows.isEmpty()) return 0;
        ZoneId zone = clock.getZone();

        Map<String, Integer> cardEvents = new HashMap<>();
        Map<String, Integer> shopHour = new HashMap<>();
        for (Distribution d : rows) {
            cardEvents.merge(cardKey(d), 1, Integer::sum);
            shopHour.merge(shopHourKey(d, zone), 1, Integer::sum);
        }

        double[][] x = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            Distribution d = rows.get(i);
            x[i] = features(d, cardEvents.get(cardKey(d)), shopHour.get(shopHourKey(d, zone)), zone);
        }

        double[] scores = null;
        if (rows.size() >= minSamples) {
            IsolationForest forest = new IsolationForest(trees, sampleSize, 42L);
            forest.fit(x);
            scores = new double[rows.size()];
            for (int i = 0; i < rows.size(); i++) scores[i] = forest.score(x[i]);
        }

        Instant from = now.minus(lookbackDays, ChronoUnit.DAYS);
        int raised = 0;
        for (int i = 0; i < rows.size(); i++) {
            Distribution d = rows.get(i);
            if (d.getDistributedAt().isBefore(from)) continue;
            double[] f = x[i];
            String what = "Card " + d.getRationCard().getCardNumber() + " received " + d.getQuantityKg().toPlainString()
                    + " kg " + d.getGrainType() + " at " + d.getFps().getShopCode() + " on "
                    + d.getDistributedAt().atZone(zone).format(WHEN);

            if (scores != null && scores[i] >= threshold) {
                String details = String.format(Locale.ROOT,
                        "%s. Isolation Forest score %.2f. Quantity/entitlement %.2f, issues this month %d, hour %d, shop issues in that hour %d.",
                        what, scores[i], f[0], (int) f[1], (int) f[2], (int) f[3]);
                if (alerts.raise("ISO:" + d.getId(), AlertType.ANOMALY,
                        scores[i] >= 0.75 ? Priority.HIGH : Priority.MEDIUM, d.getFps(),
                        "Unusual distribution at " + d.getFps().getShopCode(), details, scores[i])) raised++;
            }
            int hour = (int) f[2];
            if (hour < 6 || hour >= 22) {
                if (alerts.raise("OFFHOURS:" + d.getId(), AlertType.ANOMALY, Priority.MEDIUM, d.getFps(),
                        "Off-hours distribution at " + d.getFps().getShopCode(),
                        what + ". Issued outside normal shop hours (06:00-22:00).", null)) raised++;
            }
            if (f[3] >= VELOCITY_LIMIT) {
                if (alerts.raise("VELOCITY:" + shopHourKey(d, zone), AlertType.ANOMALY, Priority.HIGH, d.getFps(),
                        "Burst of distributions at " + d.getFps().getShopCode(),
                        (int) f[3] + " issues by " + d.getFps().getShopCode() + " within one hour (limit " + VELOCITY_LIMIT
                                + "). Example: " + what + ".", null)) raised++;
            }
        }
        return raised;
    }

    private double[] features(Distribution d, int cardEvents, int shopHourEvents, ZoneId zone) {
        var e = entitlement.calculate(d.getRationCard().getCategory(), d.getRationCard().getFamilyMembers());
        double entitled = switch (d.getGrainType()) {
            case RICE -> e.riceKg().doubleValue();
            case WHEAT -> e.wheatKg().doubleValue();
            default -> 0;
        };
        double ratio = entitled > 0 ? d.getQuantityKg().doubleValue() / entitled : 0;
        int hour = d.getDistributedAt().atZone(zone).getHour();
        return new double[]{ratio, cardEvents, hour, shopHourEvents};
    }

    private static String cardKey(Distribution d) {
        return d.getRationCard().getId() + "|" + d.getGrainType() + "|" + d.getPeriod();
    }

    private static String shopHourKey(Distribution d, ZoneId zone) {
        return d.getFps().getId() + ":" + d.getDistributedAt().atZone(zone).truncatedTo(ChronoUnit.HOURS)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
    }
}
