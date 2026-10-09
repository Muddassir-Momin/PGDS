package com.pgds.dashboard;

import com.pgds.dashboard.DashboardDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Feeds the GrainGuard-style officer dashboard. Access: SUPER_ADMIN, GOVT_OFFICER (/api/officer/**). */
@RestController
@RequestMapping("/api/officer/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary") public Summary summary() { return service.summary(); }

    @GetMapping("/stock-by-grain") public List<GrainShare> stockByGrain() { return service.stockByGrain(); }

    @GetMapping("/trend") public Trend trend(@RequestParam(defaultValue = "6") int months) { return service.trend(months); }

    @GetMapping("/shops") public List<ShopStock> shops(@RequestParam(required = false) String district) {
        return service.shopStatuses(district);
    }

    @GetMapping("/districts") public List<DistrictStock> districts() { return service.districtStock(); }

    @GetMapping("/top-shops") public List<TopShop> topShops(@RequestParam(defaultValue = "5") int limit) {
        return service.topShops(limit);
    }
}
