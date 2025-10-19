package com.example.my_books_backend.controller;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "ブックマーク")
public class BookmarkController {
    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    // application.propertiesからデモユーザーIDを取得
    @Value("${app.demo.user.id}")
    private Long DEMO_USER_ID;

    /**
     * デモ用ユーザーを取得
     */
    private User getDemoUser() {
        return userRepository.findById(DEMO_USER_ID)
            .orElseThrow(() -> new RuntimeException("Demo user not found"));
    }

    @Operation(description = "ブックマーク追加")
    @PostMapping("")
    public ResponseEntity<BookmarkResponse> createBookmark(
        @Valid @RequestBody BookmarkRequest request
    ) {
        User user = getDemoUser();
        BookmarkResponse response = bookmarkService.createBookmark(request, user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "ブックマーク更新")
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(
        @PathVariable Long id,
        @Valid @RequestBody BookmarkRequest request
    ) {
        User user = getDemoUser();
        BookmarkResponse response = bookmarkService.updateBookmark(id, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ブックマーク削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
        @PathVariable Long id
    ) {
        User user = getDemoUser();
        bookmarkService.deleteBookmark(id, user);
        return ResponseEntity.noContent().build();
    }
}
