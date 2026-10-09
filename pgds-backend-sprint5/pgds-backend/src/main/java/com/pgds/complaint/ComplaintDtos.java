package com.pgds.complaint;

import com.pgds.domain.ComplaintStatus;
import com.pgds.domain.Priority;
import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ComplaintDtos {
    private ComplaintDtos() {}

    public record ComplaintRequest(@NotBlank @Size(min = 10, max = 2000) String description, Long fpsId) {}

    public record ComplaintUpdate(@NotNull ComplaintStatus status, String category, Priority priority,
                                  String department, @Size(max = 1000) String resolutionNote) {}

    public record ComplaintView(Long id, String description, Long fpsId, String category, Priority priority,
                                String department, ComplaintStatus status, String resolutionNote, Instant createdAt) {}
}
