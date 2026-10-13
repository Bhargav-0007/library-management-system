package com.mini_project.library.service;

import com.mini_project.library.dto.BookRequest;
import com.mini_project.library.dto.BookResponse;
import com.mini_project.library.entity.Book;
import com.mini_project.library.exception.ApiException;
import com.mini_project.library.repository.BookRepository;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final OrganizationRepository organizationRepository;

    public BookService(BookRepository bookRepository,
                       LoanRepository loanRepository,
                       OrganizationRepository organizationRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public BookResponse addBook(Long orgId, BookRequest request) {
        String isbn = request.isbn().trim();
        if (bookRepository.existsByOrganizationIdAndIsbn(orgId, isbn)) {
            throw ApiException.conflict("A book with this ISBN already exists");
        }

        Book book = new Book();
        book.setOrganization(organizationRepository.getReferenceById(orgId));
        apply(book, request);
        return BookResponse.of(bookRepository.save(book), 0);
    }

    @Transactional
    public BookResponse updateBook(Long orgId, Long id, BookRequest request) {
        Book book = findBook(orgId, id);
        String isbn = request.isbn().trim();
        if (!isbn.equals(book.getIsbn()) && bookRepository.existsByOrganizationIdAndIsbn(orgId, isbn)) {
            throw ApiException.conflict("A book with this ISBN already exists");
        }

        long activeLoans = loanRepository.countByBookIdAndReturnDateIsNull(id);
        int copies = request.copies() == null ? book.getCopies() : request.copies();
        if (copies < activeLoans) {
            throw ApiException.badRequest(activeLoans + " copies are on loan; copies cannot be lower than that");
        }

        apply(book, request);
        return BookResponse.of(book, activeLoans);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooks(Long orgId, String query) {
        List<Book> books = (query == null || query.isBlank())
                ? bookRepository.findByOrganizationIdOrderByTitleAsc(orgId)
                : bookRepository.search(orgId, query.trim());

        Map<Long, Long> activeLoans = new HashMap<>();
        for (Object[] row : loanRepository.countActiveLoansByBook(orgId)) {
            activeLoans.put((Long) row[0], (Long) row[1]);
        }

        return books.stream()
                .map(book -> BookResponse.of(book, activeLoans.getOrDefault(book.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(Long orgId, Long id) {
        Book book = findBook(orgId, id);
        return BookResponse.of(book, loanRepository.countByBookIdAndReturnDateIsNull(id));
    }

    /** Deletes the book and its returned-loan history. Books currently on loan cannot be deleted. */
    @Transactional
    public void deleteBook(Long orgId, Long id) {
        Book book = findBook(orgId, id);
        if (loanRepository.countByBookIdAndReturnDateIsNull(id) > 0) {
            throw ApiException.conflict("This book has copies on loan; return them before deleting");
        }
        loanRepository.deleteByBookId(id);
        bookRepository.delete(book);
    }

    private Book findBook(Long orgId, Long id) {
        return bookRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> ApiException.notFound("Book not found"));
    }

    private static void apply(Book book, BookRequest request) {
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setIsbn(request.isbn().trim());
        if (request.copies() != null) {
            book.setCopies(request.copies());
        }
    }
}
