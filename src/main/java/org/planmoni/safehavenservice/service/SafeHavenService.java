package org.planmoni.safehavenservice.service;

import org.planmoni.safehavenservice.dto.request.SafeHavenCreateRequest;
import org.planmoni.safehavenservice.dto.request.SafeHavenUpdateRequest;
import org.planmoni.safehavenservice.dto.response.SafeHavenResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SafeHavenService {

    SafeHavenResponse createSafeHaven(SafeHavenCreateRequest request);

    SafeHavenResponse getSafeHavenById(Long id);

    SafeHavenResponse getSafeHavenByReference(String reference);

    Page<SafeHavenResponse> getAllSafeHavens(Pageable pageable);

    SafeHavenResponse updateSafeHaven(Long id, SafeHavenUpdateRequest request);

    SafeHavenResponse suspendSafeHaven(Long id);
}