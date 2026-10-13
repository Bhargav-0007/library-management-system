package com.mini_project.library.entity;

public enum Role {
    /** Created the organization; cannot be removed. */
    OWNER,
    /** Can manage staff and organization settings. */
    ADMIN,
    /** Day-to-day work: books, members, loans. */
    LIBRARIAN;

    public boolean canManageOrganization() {
        return this == OWNER || this == ADMIN;
    }
}
