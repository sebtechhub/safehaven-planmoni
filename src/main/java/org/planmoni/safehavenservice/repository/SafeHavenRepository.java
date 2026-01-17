package org.planmoni.safehavenservice.repository;

import org.planmoni.safehavenservice.entity.SafeHaven;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SafeHavenRepository extends JpaRepository<SafeHaven, Long> {

    Optional<SafeHaven> findByReference(String reference);

    boolean existsByReference(String reference);
}
