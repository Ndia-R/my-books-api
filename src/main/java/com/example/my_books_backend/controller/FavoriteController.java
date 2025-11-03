package com.example.my_books_backend.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.util.JwtClaimExtractor;
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
    private final JwtClaimExtractor jwtClaimExtractor;

    @Operation(description = "お気に入り追加")
    @PostMapping("")
    public ResponseEntity<FavoriteResponse> createFavorite(
        @Valid @RequestBody FavoriteRequest request
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        FavoriteResponse response = favoriteService.createFavoriteByUserId(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "お気に入り削除（ID指定）")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(
        @PathVariable @NonNull Long id
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        favoriteService.deleteFavoriteByUserId(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "お気に入り削除（書籍ID指定）")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> deleteFavoriteByBookId(
        @PathVariable String bookId
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        favoriteService.deleteFavoriteByBookIdAndUserId(bookId, userId);
        return ResponseEntity.noContent().build();
    }
}