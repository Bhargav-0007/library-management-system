package com.mini_project.library.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A tenant. Every library that signs up gets its own organization, and all
 * books, members, loans and staff accounts belong to exactly one of them.
 */
@Entity
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int loanDurationDays = 14;

    @Column(nullable = false)
    private int maxActiveLoansPerMember = 5;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Organization() {
    }

    public Organization(String name) {
        this.name = name;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLoanDurationDays() { return loanDurationDays; }
    public void setLoanDurationDays(int loanDurationDays) { this.loanDurationDays = loanDurationDays; }

    public int getMaxActiveLoansPerMember() { return maxActiveLoansPerMember; }
    public void setMaxActiveLoansPerMember(int maxActiveLoansPerMember) { this.maxActiveLoansPerMember = maxActiveLoansPerMember; }

    public Instant getCreatedAt() { return createdAt; }
}
