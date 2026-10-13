package com.mini_project.library.service;

import com.mini_project.library.dto.DashboardResponse;
import com.mini_project.library.entity.Book;
import com.mini_project.library.entity.Loan;
import com.mini_project.library.entity.Member;
import com.mini_project.library.entity.Organization;
import com.mini_project.library.exception.ApiException;
import com.mini_project.library.repository.AppUserRepository;
import com.mini_project.library.repository.BookRepository;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.MemberRepository;
import com.mini_project.library.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       MemberRepository memberRepository,
                       OrganizationRepository organizationRepository,
                       AppUserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Loan borrowBook(Long orgId, Long bookId, Long memberId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"));

        Book book = bookRepository.lockByIdAndOrganizationId(bookId, orgId)
                .orElseThrow(() -> ApiException.notFound("Book not found"));

        Member member = memberRepository.findByIdAndOrganizationId(memberId, orgId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));

        if (loanRepository.countByBookIdAndReturnDateIsNull(bookId) >= book.getCopies()) {
            throw ApiException.conflict("No copies of this book are available");
        }
        if (loanRepository.existsByBookIdAndMemberIdAndReturnDateIsNull(bookId, memberId)) {
            throw ApiException.conflict("This member already has a copy of this book");
        }
        if (loanRepository.countByMemberIdAndReturnDateIsNull(memberId) >= org.getMaxActiveLoansPerMember()) {
            throw ApiException.conflict("This member has reached the limit of "
                    + org.getMaxActiveLoansPerMember() + " active loans");
        }

        Loan loan = new Loan();
        loan.setOrganization(org);
        loan.setBook(book);
        loan.setMember(member);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(org.getLoanDurationDays()));
        loan.setReturnDate(null);

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan returnBook(Long orgId, Long loanId) {
        Loan loan = loanRepository.findByIdAndOrganizationId(loanId, orgId)
                .orElseThrow(() -> ApiException.notFound("Loan not found"));

        if (loan.getReturnDate() != null) {
            throw ApiException.conflict("Book already returned");
        }

        loan.setReturnDate(LocalDate.now());
        return loan;
    }

    /** @param status one of {@code active}, {@code overdue}, {@code returned}, or null for all loans */
    @Transactional(readOnly = true)
    public List<Loan> getLoans(Long orgId, String status) {
        if (status == null || status.isBlank() || status.equals("all")) {
            return loanRepository.findByOrganizationIdOrderByIssueDateDescIdDesc(orgId);
        }
        return switch (status) {
            case "active" -> loanRepository.findByOrganizationIdAndReturnDateIsNullOrderByDueDateAsc(orgId);
            case "overdue" -> getOverdueLoans(orgId);
            case "returned" -> loanRepository.findByOrganizationIdAndReturnDateIsNotNullOrderByReturnDateDesc(orgId);
            default -> throw ApiException.badRequest("Unknown status: " + status);
        };
    }

    @Transactional(readOnly = true)
    public List<Loan> getOverdueLoans(Long orgId) {
        return loanRepository.findByOrganizationIdAndReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(
                orgId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long orgId) {
        LocalDate today = LocalDate.now();
        List<Loan> dueSoon = loanRepository.findByOrganizationIdAndReturnDateIsNullOrderByDueDateAsc(orgId)
                .stream().limit(5).toList();

        return new DashboardResponse(
                bookRepository.countByOrganizationId(orgId),
                bookRepository.sumCopiesByOrganizationId(orgId),
                memberRepository.countByOrganizationId(orgId),
                loanRepository.countByOrganizationIdAndReturnDateIsNull(orgId),
                loanRepository.countByOrganizationIdAndReturnDateIsNullAndDueDateBefore(orgId, today),
                userRepository.countByOrganizationId(orgId),
                dueSoon);
    }
}
