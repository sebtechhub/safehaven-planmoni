package org.planmoni.safehavenservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating a new SafeHaven account")
public class SafeHavenCreateRequest {

    @Schema(
            description = "Unique reference identifier for the SafeHaven account",
            example = "SH-2024-001",
            required = true,
            maxLength = 100
    )
    @NotBlank(message = "Reference is required")
    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String reference;

    @Schema(
            description = "Full name of the account owner",
            example = "John Doe",
            required = true,
            maxLength = 255
    )
    @NotBlank(message = "Owner name is required")
    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Schema(
            description = "Email address of the account owner",
            example = "john.doe@example.com",
            required = true,
            maxLength = 255,
            format = "email"
    )
    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be a valid email address")
    @Size(max = 255, message = "Owner email must not exceed 255 characters")
    private String ownerEmail;

    @Schema(
            description = "Initial account balance (must be non-negative)",
            example = "1000.50",
            required = true,
            minimum = "0",
            type = "number",
            format = "decimal"
    )
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must not be negative")
    @Digits(integer = 17, fraction = 2, message = "Balance must have at most 17 integer digits and 2 decimal places")
    private BigDecimal balance;
}
