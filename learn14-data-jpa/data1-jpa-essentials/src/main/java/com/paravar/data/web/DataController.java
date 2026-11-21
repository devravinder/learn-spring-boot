package com.paravar.data.web;

import com.paravar.data.model.Book;
import com.paravar.data.service.AuditService;
import com.paravar.data.service.OrderService;
import com.paravar.data.service.PagingService;
import com.paravar.data.service.QueryService;
import com.paravar.data.service.StockService;
import com.paravar.data.support.SqlRecorder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
public class DataController {

    private final QueryService queries;
    private final PagingService paging;
    private final StockService stock;
    private final OrderService orders;
    private final AuditService audit;

    public DataController(QueryService queries, PagingService paging, StockService stock, OrderService orders, AuditService audit) {
        this.queries = queries;
        this.paging = paging;
        this.stock = stock;
        this.orders = orders;
        this.audit = audit;
    }

    // every response reports the SQL it caused
    private static Map<String, Object> measured(Supplier<Object> work) {
        SqlRecorder.start();
        Object result = work.get();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", result);
        body.put("sqlCount", SqlRecorder.statements().size());
        body.put("sql", SqlRecorder.statements());
        return body;
    }

    @GetMapping("/queries/n-plus-one")
    public Map<String, Object> nPlusOne() {
        return measured(queries::nPlusOne);
    }

    @GetMapping("/queries/fetch-join")
    public Map<String, Object> fetchJoin() {
        return measured(queries::fetchJoin);
    }

    @GetMapping("/queries/entity-graph")
    public Map<String, Object> entityGraph() {
        return measured(queries::entityGraph);
    }

    @GetMapping("/queries/projection")
    public Map<String, Object> projection(@RequestParam(defaultValue = "200") long maxPrice) {
        return measured(() -> queries.cheapBooks(maxPrice));
    }

    @GetMapping("/queries/dto")
    public Map<String, Object> dto(@RequestParam String author) {
        return measured(() -> queries.summaries(author));
    }

    @GetMapping("/queries/dynamic/{authorId}")
    public Map<String, Object> dynamic(@PathVariable long authorId) {
        return measured(() -> queries.dynamic(authorId));
    }

    @GetMapping("/books/offset")
    public Map<String, Object> offset(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return measured(() -> paging.offset(page, size));
    }

    @GetMapping("/books/slice")
    public Map<String, Object> slice(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return measured(() -> paging.slice(page, size));
    }

    @GetMapping("/books/cursor")
    public Map<String, Object> cursor(@RequestParam(required = false) String after, @RequestParam(defaultValue = "10") int size) {
        return measured(() -> paging.cursor(after, size));
    }

    @PostMapping("/stock/{id}/{strategy}")
    public Map<String, Object> take(@PathVariable long id, @PathVariable String strategy) {
        return switch (strategy) {
            case "optimistic" -> Map.of("stock", stock.optimistic(id));
            case "pessimistic" -> Map.of("stock", stock.pessimistic(id));
            case "atomic" -> Map.of("taken", stock.atomic(id));
            default -> throw new IllegalArgumentException("unknown strategy " + strategy);
        };
    }

    @PostMapping("/stock/{id}/reset")
    public Map<String, Object> reset(@PathVariable long id, @RequestParam int to) {
        stock.reset(id, to);
        return Map.of("stock", to);
    }

    @GetMapping("/books/{id}")
    public Map<String, Object> book(@PathVariable long id) {
        Book b = stock.get(id);
        return Map.of("id", b.getId(), "price", b.getPrice(), "stock", b.getStock(), "version", b.getVersion(),
                "createdAt", String.valueOf(b.getCreatedAt()), "updatedAt", String.valueOf(b.getUpdatedAt()));
    }

    @PostMapping("/books")
    public Map<String, Object> create(@RequestParam String title, @RequestParam long price) {
        return book(stock.create(title, price).getId());
    }

    @PutMapping("/books/{id}/price")
    public Map<String, Object> price(@PathVariable long id, @RequestParam long price) {
        stock.changePrice(id, price);
        return book(id);
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> order(@RequestParam long bookId, @RequestParam(defaultValue = "false") boolean fail,
                                                     @RequestParam(defaultValue = "runtime") String mode) {
        try {
            switch (mode) {
                case "checked-default" -> orders.orderCheckedDefault(bookId);
                case "checked-rollback" -> orders.orderCheckedRollback(bookId);
                default -> orders.order(bookId, fail);
            }
            return ResponseEntity.ok(Map.of("ordered", true));
        } catch (OrderService.PaymentDeclined | IllegalStateException e) {
            return ResponseEntity.unprocessableContent().body(Map.of("ordered", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/audit")
    public Object auditLog() {
        return audit.all();
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflict(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "changed by someone else, reload and retry");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
