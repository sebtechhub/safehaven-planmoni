package org.planmoni.safehavenservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard error response returned for all API errors")
public class ErrorResponse {

    @Schema(
            description = "Timestamp when the error occurred",
            example = "2024-01-27T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime timestamp;

    @Schema(
            description = "HTTP status code",
            example = "400",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private int status;

    @Schema(
            description = "HTTP status reason phrase",
            example = "Bad Request",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String error;

    @Schema(
            description = "Detailed error message",
            example = "Validation failed for one or more fields",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String message;

    @Schema(
            description = "Request path that caused the error",
            example = "/api/v1/safehaven",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String path;

    @Schema(
            description = "List of field-level validation errors (only present for validation failures)",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private List<FieldError> fieldErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Field-level validation error details")
    public static class FieldError {

        @Schema(
                description = "Name of the field that failed validation",
                example = "ownerEmail",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        private String field;

        @Schema(
                description = "Validation error message",
                example = "Owner email must be a valid email address",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        private String message;

        @Schema(
                description = "The rejected value that failed validation",
                example = "invalid-email",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        private Object rejectedValue;
    }
}
