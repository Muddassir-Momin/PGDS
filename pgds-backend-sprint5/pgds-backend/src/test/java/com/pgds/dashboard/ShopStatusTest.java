package com.pgds.dashboard;

import com.pgds.dashboard.DashboardDtos.ShopStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopStatusTest {

    private static ShopStatus s(String stock, String need) {
        return DashboardService.statusFor(new BigDecimal(stock), new BigDecimal(need));
    }

    @Test void thresholds() {
        assertEquals(ShopStatus.HEALTHY, s("1500", "1000"));
        assertEquals(ShopStatus.ADEQUATE, s("1000", "1000"));
        assertEquals(ShopStatus.LOW, s("500", "1000"));
        assertEquals(ShopStatus.CRITICAL, s("499", "1000"));
    }

    @Test void shopWithNoDemandIsHealthy() { assertEquals(ShopStatus.HEALTHY, s("0", "0")); }
}
