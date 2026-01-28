package org.planmoni.safehavenservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SafeHaven account status")
public enum Status {
    @Schema(description = "Account is active and operational")
    ACTIVE,
    
    @Schema(description = "Account has been suspended")
    SUSPENDED
}
