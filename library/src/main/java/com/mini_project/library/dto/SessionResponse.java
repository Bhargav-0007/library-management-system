package com.mini_project.library.dto;

import com.mini_project.library.entity.AppUser;
import com.mini_project.library.entity.Organization;
import com.mini_project.library.entity.Role;

/** Who is signed in and which organization they are working in. */
public record SessionResponse(String token, UserInfo user, Organization organization) {

    public record UserInfo(Long id, String email, String fullName, Role role) {
        public static UserInfo of(AppUser user) {
            return new UserInfo(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }
}
