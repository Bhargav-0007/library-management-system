package com.mini_project.library.controller;

import com.mini_project.library.dto.MemberRequest;
import com.mini_project.library.entity.Member;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member addMember(@AuthenticationPrincipal AuthUser user, @RequestBody @Valid MemberRequest request) {
        return memberService.addMember(user.organizationId(), request);
    }

    @GetMapping
    public List<Member> getAllMembers(@AuthenticationPrincipal AuthUser user) {
        return memberService.getAllMembers(user.organizationId());
    }

    @GetMapping("/{id}")
    public Member getMemberById(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return memberService.getMemberById(user.organizationId(), id);
    }

    @PutMapping("/{id}")
    public Member updateMember(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @RequestBody @Valid MemberRequest request) {
        return memberService.updateMember(user.organizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        memberService.deleteMember(user.organizationId(), id);
    }
}
