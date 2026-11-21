package com.paravar.data.repo;

import com.paravar.data.model.Author;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // fix 1: JOIN FETCH loads authors and books in one statement
    @Query("select distinct a from Author a left join fetch a.books order by a.id")
    List<Author> findAllWithBooks();

    // fix 2: the same, declared as an entity graph on a plain query
    @EntityGraph(attributePaths = "books")
    @Query("select a from Author a order by a.id")
    List<Author> findAllWithBooksGraph();
}
