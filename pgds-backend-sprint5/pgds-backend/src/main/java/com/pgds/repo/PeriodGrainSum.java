package com.pgds.repo;

import com.pgds.domain.GrainType;
import java.math.BigDecimal;

public record PeriodGrainSum(String period, GrainType grainType, BigDecimal kg) {}
