package com.paravar.data.service;

import com.paravar.data.model.Author;
import com.paravar.data.repo.AuthorRepository;
import com.paravar.data.repo.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // read-only: no dirty checking, and the driver may route to a replica
public class QueryService {

    private final AuthorRepository authors;
    private final BookRepository books;

    public QueryService(AuthorRepository authors, BookRepository books) {
        this.authors = authors;
        this.books = books;
    }

    public record AuthorBooks(String author, int books) {
    }

    // N+1: 1 query for authors, then 1 per author when .size() loads the lazy list
    public List<AuthorBooks> nPlusOne() {
        return map(authors.findAll());
    }

    public List<AuthorBooks> fetchJoin() {
        return map(authors.findAllWithBooks());
    }

    public List<AuthorBooks> entityGraph() {
        return map(authors.findAllWithBooksGraph());
    }

    public List<BookRepository.TitleAndPrice> cheapBooks(long maxPrice) {
        return books.findByPriceLessThanOrderByPriceAscIdAsc(maxPrice);
    }

    public List<BookRepository.BookSummary> summaries(String author) {
        return books.summariesByAuthor(author);
    }

    public List<BookRepository.TitleAndPrice> dynamic(long authorId) {
        return books.findByAuthorIdOrderById(authorId, BookRepository.TitleAndPrice.class);
    }

    private static List<AuthorBooks> map(List<Author> list) {
        return list.stream().map(a -> new AuthorBooks(a.getName(), a.getBooks().size())).toList();
    }
}
