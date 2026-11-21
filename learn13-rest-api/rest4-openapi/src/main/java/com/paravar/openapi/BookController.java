package com.paravar.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Browse and manage the catalog")
public class BookController {

    private final Map<Long, Book> books = new ConcurrentHashMap<>(Map.of(
            1L, new Book(1L, "Domain-Driven Design", "Eric Evans", "9780321125217", 4500, Book.Status.AVAILABLE)));
    private final AtomicLong ids = new AtomicLong(1);

    @GetMapping
    @Operation(summary = "List books", description = "Optionally filtered by author")
    public List<Book> list(@Parameter(description = "Part of the author name") @RequestParam(required = false) String author,
                           @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return books.values().stream().filter(b -> author == null || b.author().contains(author)).limit(size).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one book")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "No such book",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public Book get(@PathVariable long id) {
        Book book = books.get(id);
        if (book == null) {
            throw new ErrorResponseException(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No book " + id), null);
        }
        return book;
    }

    @PostMapping
    @Operation(summary = "Add a book", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
        Book saved = new Book(ids.incrementAndGet(), book.title(), book.author(), book.isbn(), book.price(), Book.Status.AVAILABLE);
        books.put(saved.id(), saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
