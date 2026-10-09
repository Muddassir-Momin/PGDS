package com.pgds.card;

import com.pgds.domain.CardCategory;
import com.pgds.entitlement.EntitlementService.Entitlement;
import jakarta.validation.constraints.*;

public final class CardDtos {
    private CardDtos() {}

    public record CardRequest(
            @NotBlank String cardNumber,
            @NotBlank String headOfFamily,
            String address,
            @NotBlank String district,
            @NotNull CardCategory category,
            @Min(1) @Max(30) int familyMembers,
            Long fpsId) {}

    public record CardView(Long id, String cardNumber, String headOfFamily, String address, String district,
                           CardCategory category, int familyMembers, boolean eligible, Long fpsId,
                           Entitlement monthlyEntitlement) {}

    public record BeneficiaryRequest(
            @NotBlank String name,
            @Min(0) @Max(120) int age,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String aadhaarLast4,
            @NotNull Long rationCardId,
            /** Optional: username of a registered BENEFICIARY account to link to this profile. */
            String linkUsername) {}

    public record BeneficiaryView(Long id, String name, int age, String aadhaarMasked,
                                  Long rationCardId, String cardNumber) {}
}
