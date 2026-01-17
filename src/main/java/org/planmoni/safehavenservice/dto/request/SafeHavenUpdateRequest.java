package org.planmoni.safehavenservice.dto.request;

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
public class SafeHavenUpdateRequest {

    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Email(message = "Owner email must be a valid email address")
    @Size(max = 255, message = "Owner email must not exceed 255 characters")
    private String ownerEmail;

    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must not be negative")
    @Digits(integer = 17, fraction = 2, message = "Balance must have at most 17 integer digits and 2 decimal places")
    private BigDecimal balance;
}
