package com.pgds.repo;

import com.pgds.domain.GrainType;
import java.math.BigDecimal;

public record LocationGrainBalance(Long locationId, GrainType grainType, BigDecimal kg) {}
