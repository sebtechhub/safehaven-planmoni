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
@Schema(description = "Request body for updating an existing SafeHaven account. All fields are optional.")
public class SafeHavenUpdateRequest {

    @Schema(
            description = "Updated full name of the account owner",
            example = "Jane Smith",
            maxLength = 255
    )
    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Schema(
            description = "Updated email address of the account owner",
            example = "jane.smith@example.com",
            maxLength = 255,
            format = "email"
    )
    @Email(message = "Owner email must be a valid email address")
    @Size(max = 255, message = "Owner email must not exceed 255 characters")
    private String ownerEmail;

    @Schema(
            description = "Updated account balance (must be non-negative)",
            example = "2500.75",
            minimum = "0",
            type = "number",
            format = "decimal"
    )
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must not be negative")
    @Digits(integer = 17, fraction = 2, message = "Balance must have at most 17 integer digits and 2 decimal places")
    private BigDecimal balance;
}
