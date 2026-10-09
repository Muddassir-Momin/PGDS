package com.pgds.stock;

import com.pgds.domain.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class StockDtos {
    private StockDtos() {}

    public record StockEntry(@NotNull GrainType grainType,
                             @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantityKg,
                             @Size(max = 255) String remarks) {}

    public record TransferRequest(@NotNull Long fpsId, @NotNull GrainType grainType,
                                  @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantityKg,
                                  @Size(max = 255) String remarks) {}

    public record ShopRef(Long id, String shopCode, String name) {}

    public record StockBalance(GrainType grainType, BigDecimal quantityKg) {}

    public record StockSummary(LocationType locationType, Long locationId, String name, List<StockBalance> balances) {}

    public record TxnView(Long id, StockTxnType txnType, GrainType grainType, BigDecimal quantityKg,
                          String remarks, String createdBy, Instant createdAt) {
        public static TxnView of(StockTransaction t) {
            return new TxnView(t.getId(), t.getTxnType(), t.getGrainType(), t.getQuantityKg(),
                    t.getRemarks(), t.getCreatedBy(), t.getCreatedAt());
        }
    }
}
