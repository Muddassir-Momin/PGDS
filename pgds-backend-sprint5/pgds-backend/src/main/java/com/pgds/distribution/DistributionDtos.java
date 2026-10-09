package com.pgds.distribution;

import com.pgds.domain.GrainType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DistributionDtos {
    private DistributionDtos() {}

    public record DistributionItem(@NotNull GrainType grainType,
                                   @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantityKg) {}

    public record DistributionRequest(@NotBlank String cardNumber,
                                      @NotEmpty @Size(max = 3) List<@Valid DistributionItem> items) {}

    public record ReceiptItem(GrainType grainType, BigDecimal quantityKg) {}

    public record ReceiptView(String receiptNo, String shopCode, String shopName, String cardNumber,
                              String headOfFamily, String period, Instant distributedAt, String distributedBy,
                              List<ReceiptItem> items) {}

    public record GrainBalance(GrainType grainType, BigDecimal entitledKg, BigDecimal distributedKg, BigDecimal remainingKg) {}

    public record EntitlementBalance(String cardNumber, String headOfFamily, String period, boolean eligible,
                                     List<GrainBalance> grains) {}

    public record DistributionRow(Long id, String receiptNo, String cardNumber, GrainType grainType,
                                  BigDecimal quantityKg, String period, Instant distributedAt, String distributedBy) {}
}
