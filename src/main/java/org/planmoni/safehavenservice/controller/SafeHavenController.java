package org.planmoni.safehavenservice.controller;

import org.planmoni.safehavenservice.dto.request.SafeHavenCreateRequest;
import org.planmoni.safehavenservice.dto.request.SafeHavenUpdateRequest;
import org.planmoni.safehavenservice.dto.response.SafeHavenResponse;
import org.planmoni.safehavenservice.service.SafeHavenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/safehavens")
@RequiredArgsConstructor
@Slf4j
public class SafeHavenController {

    private final SafeHavenService safeHavenService;

    @PostMapping
    public ResponseEntity<SafeHavenResponse> createSafeHaven(
            @Valid @RequestBody SafeHavenCreateRequest request) {
        log.info("POST /api/v1/safehavens - Creating SafeHaven");
        SafeHavenResponse response = safeHavenService.createSafeHaven(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafeHavenResponse> getSafeHavenById(@PathVariable Long id) {
        log.info("GET /api/v1/safehavens/{} - Fetching SafeHaven by ID", id);
        SafeHavenResponse response = safeHavenService.getSafeHavenById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<SafeHavenResponse> getSafeHavenByReference(@PathVariable String reference) {
        log.info("GET /api/v1/safehavens/reference/{} - Fetching SafeHaven by reference", reference);
        SafeHavenResponse response = safeHavenService.getSafeHavenByReference(reference);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<SafeHavenResponse>> getAllSafeHavens(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("GET /api/v1/safehavens - Fetching all SafeHavens with pagination");
        Page<SafeHavenResponse> response = safeHavenService.getAllSafeHavens(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafeHavenResponse> updateSafeHaven(
            @PathVariable Long id,
            @Valid @RequestBody SafeHavenUpdateRequest request) {
        log.info("PUT /api/v1/safehavens/{} - Updating SafeHaven", id);
        SafeHavenResponse response = safeHavenService.updateSafeHaven(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<SafeHavenResponse> suspendSafeHaven(@PathVariable Long id) {
        log.info("PATCH /api/v1/safehavens/{}/suspend - Suspending SafeHaven", id);
        SafeHavenResponse response = safeHavenService.suspendSafeHaven(id);
        return ResponseEntity.ok(response);
    }
}
