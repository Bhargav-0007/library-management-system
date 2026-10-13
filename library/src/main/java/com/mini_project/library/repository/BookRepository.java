package com.mini_project.library.repository;

import com.mini_project.library.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByOrganizationIdOrderByTitleAsc(Long organizationId);

    @Query("""
            select b from Book b
            where b.organization.id = :orgId
              and (lower(b.title) like lower(concat('%', :q, '%'))
                   or lower(b.author) like lower(concat('%', :q, '%'))
                   or lower(b.isbn) like lower(concat('%', :q, '%')))
            order by b.title asc
            """)
    List<Book> search(@Param("orgId") Long organizationId, @Param("q") String query);

    Optional<Book> findByIdAndOrganizationId(Long id, Long organizationId);

    /** Row-locks the book so concurrent borrows can't hand out more copies than exist. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id and b.organization.id = :orgId")
    Optional<Book> lockByIdAndOrganizationId(@Param("id") Long id, @Param("orgId") Long organizationId);

    boolean existsByOrganizationIdAndIsbn(Long organizationId, String isbn);

    long countByOrganizationId(Long organizationId);

    @Query("select coalesce(sum(b.copies), 0) from Book b where b.organization.id = :orgId")
    long sumCopiesByOrganizationId(@Param("orgId") Long organizationId);
}
