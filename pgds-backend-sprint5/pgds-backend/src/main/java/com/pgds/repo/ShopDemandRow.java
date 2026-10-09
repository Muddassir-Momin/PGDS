package com.pgds.repo;

import com.pgds.domain.CardCategory;

public record ShopDemandRow(Long fpsId, CardCategory category, Long cards, Long members) {}
