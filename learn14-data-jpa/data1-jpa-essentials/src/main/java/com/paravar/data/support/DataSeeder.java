package com.paravar.data.support;

import com.paravar.data.model.Author;
import com.paravar.data.model.Book;
import com.paravar.data.repo.AuthorRepository;
import com.paravar.data.repo.BookRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 20 authors x 5 books; prices repeat on purpose so the cursor needs a tie-breaker (id)
@Component
public class DataSeeder implements ApplicationRunner {

    private final AuthorRepository authors;
    private final BookRepository books;

    public DataSeeder(AuthorRepository authors, BookRepository books) {
        this.authors = authors;
        this.books = books;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (int a = 1; a <= 20; a++) {
            Author author = authors.save(new Author("Author " + a));
            for (int b = 1; b <= 5; b++) {
                books.save(new Book("Book " + a + "-" + b, 100L * ((a + b) % 7 + 1), 100, author));
            }
        }
    }
}
