package com.example.my_books_api.controller;

import com.example.my_books_api.dto.favorite.FavoriteRequest;
import com.example.my_books_api.dto.favorite.FavoriteResponse;
import com.example.my_books_api.service.FavoriteService;
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
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorite", description = "お気に入り")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(description = "お気に入り追加")
    @PostMapping("")
    public ResponseEntity<FavoriteResponse> createFavorite(
        @Valid @RequestBody FavoriteRequest request
    ) {
        FavoriteResponse response = favoriteService.createFavorite(request);
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
        favoriteService.deleteFavorite(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "お気に入り削除（書籍ID指定）")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> deleteFavoriteByBookId(
        @PathVariable String bookId
    ) {
        favoriteService.deleteFavoriteByBookId(bookId);
        return ResponseEntity.noContent().build();
    }
}
