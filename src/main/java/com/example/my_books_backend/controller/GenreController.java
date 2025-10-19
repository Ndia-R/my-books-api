package com.example.my_books_backend.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.genre.GenreRequest;
import com.example.my_books_backend.dto.genre.GenreResponse;
import com.example.my_books_backend.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
@Tag(name = "Genre", description = "ジャンル")
public class GenreController {
    private final GenreService genreService;

    @Operation(description = "すべてのジャンル取得")
    @GetMapping("")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<GenreResponse> response = genreService.getAllGenres();
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定のジャンル取得")
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable Long id) {
        GenreResponse response = genreService.getGenreById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ジャンル作成")
    @PostMapping("")
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        GenreResponse response = genreService.createGenre(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "ジャンル更新")
    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> updateGenre(
        @PathVariable Long id,
        @Valid @RequestBody GenreRequest request
    ) {
        GenreResponse respons = genreService.updateGenre(id, request);
        return ResponseEntity.ok(respons);
    }

    @Operation(description = "ジャンル削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}
