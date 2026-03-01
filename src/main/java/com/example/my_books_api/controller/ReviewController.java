package com.example.my_books_api.controller;

import com.example.my_books_api.dto.review.ReviewRequest;
import com.example.my_books_api.dto.review.ReviewResponse;
import com.example.my_books_api.service.ReviewService;
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
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "レビュー")
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(description = "レビュー作成")
    @PostMapping("")
    public ResponseEntity<ReviewResponse> createReview(
        @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse response = reviewService.createReview(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "レビュー更新")
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
        @PathVariable @NonNull Long id,
        @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "レビュー削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable @NonNull Long id
    ) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}