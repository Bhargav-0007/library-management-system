package com.mini_project.library.security;

import com.mini_project.library.entity.AppUser;
import com.mini_project.library.entity.Role;

/**
 * The authenticated principal for a request. {@code organizationId} is the tenant
 * every data access in the request must be scoped to.
 */
public record AuthUser(Long userId, Long organizationId, String email, String fullName, Role role) {

    public static AuthUser from(AppUser user) {
        return new AuthUser(user.getId(), user.getOrganization().getId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }
}
