package com.paravar.data.service;

import com.paravar.data.repo.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    public static class PaymentDeclined extends Exception {
        public PaymentDeclined() {
            super("payment declined");
        }
    }

    private final BookRepository books;
    private final AuditService audit;

    public OrderService(BookRepository books, AuditService audit) {
        this.books = books;
        this.audit = audit;
    }

    // RuntimeException rolls back; the REQUIRES_NEW audit row survives because it committed on its own
    @Transactional
    public void order(long bookId, boolean fail) {
        books.decrementStock(bookId);
        audit.log("order attempted for book " + bookId);
        if (fail) {
            throw new IllegalStateException("out of luck");
        }
    }

    // trap: a checked exception does NOT roll back by default, so the stock change commits
    @Transactional
    public void orderCheckedDefault(long bookId) throws PaymentDeclined {
        books.decrementStock(bookId);
        throw new PaymentDeclined();
    }

    @Transactional(rollbackFor = PaymentDeclined.class)
    public void orderCheckedRollback(long bookId) throws PaymentDeclined {
        books.decrementStock(bookId);
        throw new PaymentDeclined();
    }
}
