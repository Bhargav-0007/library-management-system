package com.mini_project.library.dto;

import com.mini_project.library.entity.Book;

public record BookResponse(Long id, String title, String author, String isbn, int copies, int availableCopies) {

    public static BookResponse of(Book book, long activeLoans) {
        int available = (int) Math.max(0, book.getCopies() - activeLoans);
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getCopies(), available);
    }
}
