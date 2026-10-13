package com.mini_project.library.controller;

import com.mini_project.library.dto.DashboardResponse;
import com.mini_project.library.dto.OrganizationRequest;
import com.mini_project.library.dto.SessionResponse;
import com.mini_project.library.dto.StaffRequest;
import com.mini_project.library.entity.Organization;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.service.LoanService;
import com.mini_project.library.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final LoanService loanService;

    public OrganizationController(OrganizationService organizationService, LoanService loanService) {
        this.organizationService = organizationService;
        this.loanService = loanService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AuthUser user) {
        return loanService.dashboard(user.organizationId());
    }

    @GetMapping("/organization")
    public Organization getOrganization(@AuthenticationPrincipal AuthUser user) {
        return organizationService.getOrganization(user.organizationId());
    }

    @PutMapping("/organization")
    public Organization updateOrganization(@AuthenticationPrincipal AuthUser user,
                                           @RequestBody @Valid OrganizationRequest request) {
        return organizationService.updateOrganization(user.organizationId(), request);
    }

    @GetMapping("/team")
    public List<SessionResponse.UserInfo> listStaff(@AuthenticationPrincipal AuthUser user) {
        return organizationService.listStaff(user.organizationId());
    }

    @PostMapping("/team")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse.UserInfo addStaff(@AuthenticationPrincipal AuthUser user,
                                             @RequestBody @Valid StaffRequest request) {
        return organizationService.addStaff(user, request);
    }

    @DeleteMapping("/team/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaff(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        organizationService.removeStaff(user, id);
    }
}
