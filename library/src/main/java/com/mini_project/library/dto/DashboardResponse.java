package com.mini_project.library.dto;

import com.mini_project.library.entity.Loan;

import java.util.List;

public record DashboardResponse(
        long titles,
        long totalCopies,
        long members,
        long activeLoans,
        long overdueLoans,
        long staff,
        List<Loan> dueSoon
) {
}
