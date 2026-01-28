package org.planmoni.safehavenservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.planmoni.safehavenservice.dto.request.SafeHavenCreateRequest;
import org.planmoni.safehavenservice.dto.request.SafeHavenUpdateRequest;
import org.planmoni.safehavenservice.dto.response.ErrorResponse;
import org.planmoni.safehavenservice.dto.response.SafeHavenResponse;
import org.planmoni.safehavenservice.service.SafeHavenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/safehaven")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SafeHaven", description = "SafeHaven account management endpoints")
public class SafeHavenController {

    private final SafeHavenService safeHavenService;

    @Operation(
            summary = "Create a new SafeHaven account",
            description = "Creates a new SafeHaven account with the provided details. The reference must be unique."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "SafeHaven account created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SafeHavenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "SafeHaven with the given reference already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<SafeHavenResponse> createSafeHaven(
            @Valid @RequestBody SafeHavenCreateRequest request) {
        log.info("POST /api/v1/safehaven - Creating SafeHaven");
        SafeHavenResponse response = safeHavenService.createSafeHaven(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get SafeHaven account by ID",
            description = "Retrieves a SafeHaven account using its unique database ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SafeHaven account found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SafeHavenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SafeHaven account not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<SafeHavenResponse> getSafeHavenById(
            @Parameter(description = "SafeHaven account ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("GET /api/v1/safehaven/{} - Fetching SafeHaven by ID", id);
        SafeHavenResponse response = safeHavenService.getSafeHavenById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get SafeHaven account by reference",
            description = "Retrieves a SafeHaven account using its unique reference identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SafeHaven account found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SafeHavenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SafeHaven account not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/reference/{reference}")
    public ResponseEntity<SafeHavenResponse> getSafeHavenByReference(
            @Parameter(description = "SafeHaven reference identifier", required = true, example = "SH-2024-001")
            @PathVariable String reference) {
        log.info("GET /api/v1/safehaven/reference/{} - Fetching SafeHaven by reference", reference);
        SafeHavenResponse response = safeHavenService.getSafeHavenByReference(reference);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all SafeHaven accounts (paginated)",
            description = """
                    Retrieves all SafeHaven accounts with pagination support.
                    
                    Query parameters:
                    - page: Page number (0-indexed, default: 0)
                    - size: Number of items per page (default: 20)
                    - sort: Sort field and direction (default: createdAt,desc)
                    
                    Example: /api/v1/safehaven?page=0&size=10&sort=createdAt,desc
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SafeHaven accounts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Page<SafeHavenResponse>> getAllSafeHavens(
            @Parameter(description = "Pagination parameters (page, size, sort)", hidden = true)
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("GET /api/v1/safehaven - Fetching all SafeHavens with pagination");
        Page<SafeHavenResponse> response = safeHavenService.getAllSafeHavens(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update SafeHaven account",
            description = "Updates an existing SafeHaven account with the provided details. Only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SafeHaven account updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SafeHavenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SafeHaven account not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<SafeHavenResponse> updateSafeHaven(
            @Parameter(description = "SafeHaven account ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SafeHavenUpdateRequest request) {
        log.info("PUT /api/v1/safehaven/{} - Updating SafeHaven", id);
        SafeHavenResponse response = safeHavenService.updateSafeHaven(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Suspend SafeHaven account",
            description = "Suspends an active SafeHaven account. The account status will be changed to SUSPENDED."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SafeHaven account suspended successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SafeHavenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SafeHaven account not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "SafeHaven account is already suspended",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<SafeHavenResponse> suspendSafeHaven(
            @Parameter(description = "SafeHaven account ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("PATCH /api/v1/safehaven/{}/suspend - Suspending SafeHaven", id);
        SafeHavenResponse response = safeHavenService.suspendSafeHaven(id);
        return ResponseEntity.ok(response);
    }
}
