package com.pgds.ai;

import com.pgds.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiAdminController {

    private final ObjectProvider<DemoHistoryGenerator> generator;

    /** SUPER_ADMIN only (/api/admin/**). Dev/demo helper, disabled when pgds.ai.demo-endpoint-enabled=false. */
    @PostMapping("/api/admin/ai/demo-history")
    public DemoHistoryGenerator.Result demoHistory(@RequestParam(defaultValue = "1") Long fpsId) {
        DemoHistoryGenerator g = generator.getIfAvailable();
        if (g == null) throw new ApiException(HttpStatus.NOT_FOUND, "Demo generator is disabled");
        return g.generate(fpsId);
    }
}
