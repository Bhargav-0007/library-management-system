package com.mini_project.library.service;

import com.mini_project.library.dto.OrganizationRequest;
import com.mini_project.library.dto.SessionResponse;
import com.mini_project.library.dto.StaffRequest;
import com.mini_project.library.entity.AppUser;
import com.mini_project.library.entity.Organization;
import com.mini_project.library.entity.Role;
import com.mini_project.library.exception.ApiException;
import com.mini_project.library.repository.AppUserRepository;
import com.mini_project.library.repository.OrganizationRepository;
import com.mini_project.library.security.AuthUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Organization settings and staff (team) management. */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(OrganizationRepository organizationRepository,
                               AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Organization getOrganization(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"));
    }

    @Transactional
    public Organization updateOrganization(Long orgId, OrganizationRequest request) {
        Organization org = getOrganization(orgId);
        org.setName(request.name().trim());
        org.setLoanDurationDays(request.loanDurationDays());
        org.setMaxActiveLoansPerMember(request.maxActiveLoansPerMember());
        return org;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse.UserInfo> listStaff(Long orgId) {
        return userRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .map(SessionResponse.UserInfo::of)
                .toList();
    }

    @Transactional
    public SessionResponse.UserInfo addStaff(AuthUser actor, StaffRequest request) {
        if (request.role() == Role.OWNER) {
            throw ApiException.badRequest("An organization can only have one owner");
        }
        String email = AuthService.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Organization org = organizationRepository.getReferenceById(actor.organizationId());
        AppUser user = userRepository.save(new AppUser(org, email,
                passwordEncoder.encode(request.password()), request.fullName().trim(), request.role()));
        return SessionResponse.UserInfo.of(user);
    }

    @Transactional
    public void removeStaff(AuthUser actor, Long userId) {
        AppUser user = userRepository.findByIdAndOrganizationId(userId, actor.organizationId())
                .orElseThrow(() -> ApiException.notFound("Staff member not found"));

        if (user.getId().equals(actor.userId())) {
            throw ApiException.badRequest("You cannot remove yourself");
        }
        if (user.getRole() == Role.OWNER) {
            throw ApiException.forbidden("The organization owner cannot be removed");
        }
        if (user.getRole() == Role.ADMIN && actor.role() != Role.OWNER) {
            throw ApiException.forbidden("Only the owner can remove admins");
        }
        userRepository.delete(user);
    }
}
