package com.pgds.ai;

import com.pgds.ai.AiDtos.ForecastLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockForecastLevelTest {
    @Test
    void levels() {
        assertEquals(ForecastLevel.CRITICAL, StockForecastService.levelFor(2.0));
        assertEquals(ForecastLevel.WARNING, StockForecastService.levelFor(5.0));
        assertEquals(ForecastLevel.WATCH, StockForecastService.levelFor(10.0));
        assertEquals(ForecastLevel.OK, StockForecastService.levelFor(30.0));
        assertEquals(ForecastLevel.OK, StockForecastService.levelFor(null));
    }
}
