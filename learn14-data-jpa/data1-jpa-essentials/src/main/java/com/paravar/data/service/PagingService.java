package com.paravar.data.service;

import com.paravar.data.model.Book;
import com.paravar.data.repo.BookRepository;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PagingService {

    private static final Sort BY_PRICE_THEN_ID = Sort.by("price", "id"); // id breaks ties so the order is total

    private final BookRepository books;

    public PagingService(BookRepository books) {
        this.books = books;
    }

    public record Item(long id, String title, long price) {
    }

    public record OffsetPage(List<Item> items, int page, long totalElements, int totalPages) {
    }

    public record SlicePage(List<Item> items, int page, boolean hasNext) {
    }

    public record CursorPage(List<Item> items, String nextCursor) {
    }

    // OFFSET n LIMIT size + SELECT COUNT(*): simple, but slow deep pages and shifts on inserts
    public OffsetPage offset(int page, int size) {
        Page<Book> p = books.findAll(PageRequest.of(page, size, BY_PRICE_THEN_ID));
        return new OffsetPage(items(p.getContent()), page, p.getTotalElements(), p.getTotalPages());
    }

    public SlicePage slice(int page, int size) {
        Slice<Book> s = books.findSliceBy(PageRequest.of(page, size, BY_PRICE_THEN_ID));
        return new SlicePage(items(s.getContent()), page, s.hasNext());
    }

    // cursor = the sort keys of the last row seen, encoded so clients treat it as opaque
    public CursorPage cursor(String after, int size) {
        ScrollPosition position = after == null ? ScrollPosition.keyset() : decode(after);
        Window<Book> window = books.findByOrderByPriceAscIdAsc(position, Limit.of(size));
        String next = window.hasNext() ? encode((KeysetScrollPosition) window.positionAt(window.size() - 1)) : null;
        return new CursorPage(items(window.getContent()), next);
    }

    private static List<Item> items(List<Book> list) {
        return list.stream().map(b -> new Item(b.getId(), b.getTitle(), b.getPrice())).toList();
    }

    private static String encode(KeysetScrollPosition position) {
        Map<String, Object> keys = position.getKeys();
        String raw = keys.get("price") + ":" + keys.get("id");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static ScrollPosition decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":");
            return ScrollPosition.forward(Map.of("price", Long.parseLong(parts[0]), "id", Long.parseLong(parts[1])));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }
}
