package com.example.my_books_api.controller;

import com.example.my_books_api.dto.bookmark.BookmarkRequest;
import com.example.my_books_api.dto.bookmark.BookmarkResponse;
import com.example.my_books_api.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "ブックマーク")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @Operation(description = "ブックマーク追加")
    @PostMapping("")
    public ResponseEntity<BookmarkResponse> createBookmark(
        @Valid @RequestBody BookmarkRequest request
    ) {
        BookmarkResponse response = bookmarkService.createBookmark(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "ブックマーク更新")
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(
        @PathVariable @NonNull Long id,
        @Valid @RequestBody BookmarkRequest request
    ) {
        BookmarkResponse response = bookmarkService.updateBookmark(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ブックマーク削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
        @PathVariable @NonNull Long id
    ) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.noContent().build();
    }
}
