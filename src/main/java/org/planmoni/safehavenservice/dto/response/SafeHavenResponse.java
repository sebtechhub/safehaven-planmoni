package org.planmoni.safehavenservice.dto.response;

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
public class SafeHavenResponse {

    private Long id;
    private String reference;
    private String ownerName;
    private String ownerEmail;
    private BigDecimal balance;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
