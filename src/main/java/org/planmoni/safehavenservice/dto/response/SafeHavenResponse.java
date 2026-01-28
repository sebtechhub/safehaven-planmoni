package org.planmoni.safehavenservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.planmoni.safehavenservice.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "SafeHaven account details response")
public class SafeHavenResponse {

    @Schema(
            description = "Unique database identifier",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(
            description = "Unique reference identifier",
            example = "SH-2024-001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String reference;

    @Schema(
            description = "Full name of the account owner",
            example = "John Doe",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String ownerName;

    @Schema(
            description = "Email address of the account owner",
            example = "john.doe@example.com",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String ownerEmail;

    @Schema(
            description = "Current account balance",
            example = "1000.50",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private BigDecimal balance;

    @Schema(
            description = "Account status (ACTIVE or SUSPENDED)",
            example = "ACTIVE",
            accessMode = Schema.AccessMode.READ_ONLY,
            allowableValues = {"ACTIVE", "SUSPENDED"}
    )
    private Status status;

    @Schema(
            description = "Timestamp when the account was created",
            example = "2024-01-27T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the account was last updated",
            example = "2024-01-27T15:45:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updatedAt;
}
