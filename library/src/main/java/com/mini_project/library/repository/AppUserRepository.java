package com.mini_project.library.repository;

import com.mini_project.library.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "organization")
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "organization")
    Optional<AppUser> findWithOrganizationById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    Optional<AppUser> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationId(Long organizationId);
}
