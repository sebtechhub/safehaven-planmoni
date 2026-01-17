package org.planmoni.safehavenservice.service.impl;

import org.planmoni.safehavenservice.dto.request.SafeHavenCreateRequest;
import org.planmoni.safehavenservice.dto.request.SafeHavenUpdateRequest;
import org.planmoni.safehavenservice.dto.response.SafeHavenResponse;
import org.planmoni.safehavenservice.entity.SafeHaven;
import org.planmoni.safehavenservice.entity.Status;
import org.planmoni.safehavenservice.exception.DuplicateReferenceException;
import org.planmoni.safehavenservice.exception.IllegalOperationException;
import org.planmoni.safehavenservice.exception.SafeHavenNotFoundException;
import org.planmoni.safehavenservice.mapper.SafeHavenMapper;
import org.planmoni.safehavenservice.repository.SafeHavenRepository;
import org.planmoni.safehavenservice.service.SafeHavenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SafeHavenServiceImpl implements SafeHavenService {

    private final SafeHavenRepository safeHavenRepository;
    private final SafeHavenMapper safeHavenMapper;

    @Override
    public SafeHavenResponse createSafeHaven(SafeHavenCreateRequest request) {
        log.info("Creating SafeHaven with reference: {}", request.getReference());

        // Business rule: reference must be unique
        if (safeHavenRepository.existsByReference(request.getReference())) {
            throw new DuplicateReferenceException(
                    String.format("SafeHaven with reference '%s' already exists", request.getReference()));
        }

        // Business rule: balance must not be negative
        if (request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalOperationException("Balance must not be negative");
        }

        SafeHaven safeHaven = safeHavenMapper.toEntity(request);
        safeHaven = safeHavenRepository.save(safeHaven);

        log.info("SafeHaven created successfully with ID: {}", safeHaven.getId());
        return safeHavenMapper.toResponse(safeHaven);
    }

    @Override
    @Transactional(readOnly = true)
    public SafeHavenResponse getSafeHavenById(Long id) {
        log.info("Fetching SafeHaven with ID: {}", id);
        SafeHaven safeHaven = safeHavenRepository.findById(id)
                .orElseThrow(() -> new SafeHavenNotFoundException(
                        String.format("SafeHaven with ID %d not found", id)));
        return safeHavenMapper.toResponse(safeHaven);
    }

    @Override
    @Transactional(readOnly = true)
    public SafeHavenResponse getSafeHavenByReference(String reference) {
        log.info("Fetching SafeHaven with reference: {}", reference);
        SafeHaven safeHaven = safeHavenRepository.findByReference(reference)
                .orElseThrow(() -> new SafeHavenNotFoundException(
                        String.format("SafeHaven with reference '%s' not found", reference)));
        return safeHavenMapper.toResponse(safeHaven);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SafeHavenResponse> getAllSafeHavens(Pageable pageable) {
        log.info("Fetching all SafeHavens with pagination: {}", pageable);
        return safeHavenRepository.findAll(pageable)
                .map(safeHavenMapper::toResponse);
    }

    @Override
    public SafeHavenResponse updateSafeHaven(Long id, SafeHavenUpdateRequest request) {
        log.info("Updating SafeHaven with ID: {}", id);
        SafeHaven safeHaven = safeHavenRepository.findById(id)
                .orElseThrow(() -> new SafeHavenNotFoundException(
                        String.format("SafeHaven with ID %d not found", id)));

        // Business rule: suspended SafeHaven cannot be updated
        if (safeHaven.getStatus() == Status.SUSPENDED) {
            throw new IllegalOperationException(
                    String.format("Cannot update SafeHaven with ID %d because it is suspended", id));
        }

        // Business rule: balance must not be negative
        if (request.getBalance() != null && request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalOperationException("Balance must not be negative");
        }

        safeHavenMapper.updateEntityFromRequest(request, safeHaven);
        safeHaven = safeHavenRepository.save(safeHaven);

        log.info("SafeHaven with ID {} updated successfully", id);
        return safeHavenMapper.toResponse(safeHaven);
    }

    @Override
    public SafeHavenResponse suspendSafeHaven(Long id) {
        log.info("Suspending SafeHaven with ID: {}", id);
        SafeHaven safeHaven = safeHavenRepository.findById(id)
                .orElseThrow(() -> new SafeHavenNotFoundException(
                        String.format("SafeHaven with ID %d not found", id)));

        safeHaven.setStatus(Status.SUSPENDED);
        safeHaven = safeHavenRepository.save(safeHaven);

        log.info("SafeHaven with ID {} suspended successfully", id);
        return safeHavenMapper.toResponse(safeHaven);
    }
}
