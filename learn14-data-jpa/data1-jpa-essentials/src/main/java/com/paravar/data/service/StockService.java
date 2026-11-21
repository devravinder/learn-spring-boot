package com.paravar.data.service;

import com.paravar.data.model.Book;
import com.paravar.data.repo.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// three ways to take one unit of stock when many requests race for it
@Service
public class StockService {

    private final BookRepository books;

    public StockService(BookRepository books) {
        this.books = books;
    }

    // optimistic: no lock; the @Version check makes the loser fail at commit
    @Transactional
    public int optimistic(long id) {
        Book book = books.findById(id).orElseThrow();
        pause();
        book.setStock(book.getStock() - 1);
        return book.getStock();
    }

    // pessimistic: SELECT ... FOR UPDATE; others wait for the row lock
    @Transactional
    public int pessimistic(long id) {
        Book book = books.lockById(id).orElseThrow();
        pause();
        book.setStock(book.getStock() - 1);
        return book.getStock();
    }

    // atomic: one UPDATE, no read in Java at all
    @Transactional
    public boolean atomic(long id) {
        return books.decrementStock(id) == 1;
    }

    @Transactional
    public void reset(long id, int stock) {
        books.findById(id).orElseThrow().setStock(stock);
    }

    @Transactional(readOnly = true)
    public Book get(long id) {
        return books.findById(id).orElseThrow();
    }

    @Transactional
    public Book changePrice(long id, long price) {
        Book book = books.findById(id).orElseThrow();
        book.setPrice(price);
        return books.saveAndFlush(book); // flush now so version and updatedAt are visible in the reply
    }

    @Transactional
    public Book create(String title, long price) {
        Book first = books.findById(1L).orElseThrow();
        return books.save(new Book(title, price, 10, first.getAuthor()));
    }

    // widen the race window so the demo shows conflicts reliably
    private static void pause() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
