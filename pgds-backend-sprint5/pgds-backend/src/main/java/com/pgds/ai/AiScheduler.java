package com.pgds.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pgds.ai.schedule-enabled", havingValue = "true", matchIfMissing = true)
public class AiScheduler {

    private final AiAnalysisService analysis;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    public void nightlyRun() {
        try {
            log.info("Nightly AI analysis finished: {}", analysis.runAll());
        } catch (Exception e) {
            log.error("Nightly AI analysis failed", e);
        }
    }
}
