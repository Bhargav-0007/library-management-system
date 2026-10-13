package com.mini_project.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A staff account that can sign in. Library members (patrons) are {@link Member}s
 * and do not log in.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AppUser() {
    }

    public AppUser(Organization organization, String email, String passwordHash, String fullName, Role role) {
        this.organization = organization;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Organization getOrganization() { return organization; }

    public Instant getCreatedAt() { return createdAt; }
}
