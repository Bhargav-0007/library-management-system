package com.mini_project.library.repository;

import com.mini_project.library.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<Member> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndEmailIgnoreCase(Long organizationId, String email);

    long countByOrganizationId(Long organizationId);
}
