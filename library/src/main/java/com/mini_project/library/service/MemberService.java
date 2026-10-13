package com.mini_project.library.service;

import com.mini_project.library.dto.MemberRequest;
import com.mini_project.library.entity.Member;
import com.mini_project.library.exception.ApiException;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.MemberRepository;
import com.mini_project.library.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final OrganizationRepository organizationRepository;

    public MemberService(MemberRepository memberRepository,
                         LoanRepository loanRepository,
                         OrganizationRepository organizationRepository) {
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public Member addMember(Long orgId, MemberRequest request) {
        String email = AuthService.normalizeEmail(request.email());
        if (memberRepository.existsByOrganizationIdAndEmailIgnoreCase(orgId, email)) {
            throw ApiException.conflict("A member with this email already exists");
        }

        Member member = new Member();
        member.setOrganization(organizationRepository.getReferenceById(orgId));
        member.setName(request.name().trim());
        member.setEmail(email);
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateMember(Long orgId, Long id, MemberRequest request) {
        Member member = getMemberById(orgId, id);
        String email = AuthService.normalizeEmail(request.email());
        if (!email.equalsIgnoreCase(member.getEmail())
                && memberRepository.existsByOrganizationIdAndEmailIgnoreCase(orgId, email)) {
            throw ApiException.conflict("A member with this email already exists");
        }
        member.setName(request.name().trim());
        member.setEmail(email);
        return member;
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers(Long orgId) {
        return memberRepository.findByOrganizationIdOrderByNameAsc(orgId);
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long orgId, Long id) {
        return memberRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));
    }

    /** Deletes the member and their returned-loan history. Members with books out cannot be deleted. */
    @Transactional
    public void deleteMember(Long orgId, Long id) {
        Member member = getMemberById(orgId, id);
        if (loanRepository.countByMemberIdAndReturnDateIsNull(id) > 0) {
            throw ApiException.conflict("This member still has books on loan");
        }
        loanRepository.deleteByMemberId(id);
        memberRepository.delete(member);
    }
}
