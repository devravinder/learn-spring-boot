package com.paravar.data.repo;

import com.paravar.data.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // interface projection: Hibernate selects only these two columns
    interface TitleAndPrice {
        String getTitle();

        long getPrice();
    }

    record BookSummary(String title, String author) {
    }

    List<TitleAndPrice> findByPriceLessThanOrderByPriceAscIdAsc(long maxPrice);

    // DTO projection with a constructor expression
    @Query("select new com.paravar.data.repo.BookRepository$BookSummary(b.title, a.name) from Book b join b.author a where a.name = :author order by b.id")
    List<BookSummary> summariesByAuthor(@Param("author") String author);

    // dynamic projection: the caller picks the shape
    <T> List<T> findByAuthorIdOrderById(Long authorId, Class<T> type);

    // Slice: knows "is there a next page" without a COUNT(*) query
    Slice<Book> findSliceBy(Pageable pageable);

    // keyset scrolling: WHERE (price, id) > (:lastPrice, :lastId), no OFFSET
    Window<Book> findByOrderByPriceAscIdAsc(ScrollPosition position, Limit limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> lockById(@Param("id") Long id);

    // single atomic statement: the database does read-modify-write itself
    @Modifying
    @Query("update Book b set b.stock = b.stock - 1, b.version = b.version + 1 where b.id = :id and b.stock > 0")
    int decrementStock(@Param("id") Long id);
}
