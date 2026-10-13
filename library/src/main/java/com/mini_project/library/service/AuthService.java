package com.mini_project.library.service;

import com.mini_project.library.dto.LoginRequest;
import com.mini_project.library.dto.SessionResponse;
import com.mini_project.library.dto.SignupRequest;
import com.mini_project.library.entity.AppUser;
import com.mini_project.library.entity.Organization;
import com.mini_project.library.entity.Role;
import com.mini_project.library.exception.ApiException;
import com.mini_project.library.repository.AppUserRepository;
import com.mini_project.library.repository.OrganizationRepository;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Creates a new organization (tenant) with the caller as its owner. */
    @Transactional
    public SessionResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Organization org = organizationRepository.save(new Organization(request.organizationName().trim()));
        AppUser owner = userRepository.save(new AppUser(org, email,
                passwordEncoder.encode(request.password()), request.fullName().trim(), Role.OWNER));

        return session(owner);
    }

    @Transactional(readOnly = true)
    public SessionResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        return session(user);
    }

    @Transactional(readOnly = true)
    public SessionResponse me(AuthUser authUser) {
        AppUser user = userRepository.findWithOrganizationById(authUser.userId())
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        return new SessionResponse(null, SessionResponse.UserInfo.of(user), user.getOrganization());
    }

    private SessionResponse session(AppUser user) {
        return new SessionResponse(jwtService.issueToken(user), SessionResponse.UserInfo.of(user),
                user.getOrganization());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
