package com.mini_project.library.repository;

import com.mini_project.library.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByOrganizationIdOrderByIssueDateDescIdDesc(Long organizationId);

    List<Loan> findByOrganizationIdAndReturnDateIsNullOrderByDueDateAsc(Long organizationId);

    List<Loan> findByOrganizationIdAndReturnDateIsNotNullOrderByReturnDateDesc(Long organizationId);

    List<Loan> findByOrganizationIdAndReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(Long organizationId, LocalDate date);

    Optional<Loan> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByBookIdAndReturnDateIsNull(Long bookId);

    long countByMemberIdAndReturnDateIsNull(Long memberId);

    boolean existsByBookIdAndMemberIdAndReturnDateIsNull(Long bookId, Long memberId);

    void deleteByBookId(Long bookId);

    void deleteByMemberId(Long memberId);

    long countByOrganizationIdAndReturnDateIsNull(Long organizationId);

    long countByOrganizationIdAndReturnDateIsNullAndDueDateBefore(Long organizationId, LocalDate date);

    /** Rows of [bookId, activeLoanCount] for every book in the organization with an active loan. */
    @Query("""
            select l.book.id, count(l) from Loan l
            where l.organization.id = :orgId and l.returnDate is null
            group by l.book.id
            """)
    List<Object[]> countActiveLoansByBook(@Param("orgId") Long organizationId);
}
