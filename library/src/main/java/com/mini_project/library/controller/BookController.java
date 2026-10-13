package com.mini_project.library.controller;

import com.mini_project.library.dto.BookRequest;
import com.mini_project.library.dto.BookResponse;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse addBook(@AuthenticationPrincipal AuthUser user, @RequestBody @Valid BookRequest request) {
        return bookService.addBook(user.organizationId(), request);
    }

    @GetMapping
    public List<BookResponse> getBooks(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false) String q) {
        return bookService.getBooks(user.organizationId(), q);
    }

    @GetMapping("/{id}")
    public BookResponse getBookById(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookService.getBook(user.organizationId(), id);
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @RequestBody @Valid BookRequest request) {
        return bookService.updateBook(user.organizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        bookService.deleteBook(user.organizationId(), id);
    }
}
