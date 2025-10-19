package com.example.my_books_backend.controller;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorite", description = "お気に入り")
public class FavoriteController {
    private final FavoriteService favoriteService;
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

    @Operation(description = "お気に入り追加")
    @PostMapping("")
    public ResponseEntity<FavoriteResponse> createFavorite(
        @Valid @RequestBody FavoriteRequest request
    ) {
        User user = getDemoUser();
        FavoriteResponse response = favoriteService.createFavorite(request, user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "お気に入り削除（ID指定）")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(
        @PathVariable Long id
    ) {
        User user = getDemoUser();
        favoriteService.deleteFavorite(id, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "お気に入り削除（書籍ID指定）")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> deleteFavoriteByBookId(
        @PathVariable String bookId
    ) {
        User user = getDemoUser();
        favoriteService.deleteFavoriteByBookId(bookId, user);
        return ResponseEntity.noContent().build();
    }
}
