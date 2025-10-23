package com.example.my_books_backend.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "レビュー")
public class ReviewController {
    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @Operation(description = "レビュー作成")
    @PostMapping("")
    public ResponseEntity<ReviewResponse> createReview(
        @Valid @RequestBody ReviewRequest request
    ) {
        String userId = securityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReviewByUserId(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "レビュー更新")
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
        @PathVariable Long id,
        @Valid @RequestBody ReviewRequest request
    ) {
        String userId = securityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.updateReviewByUserId(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "レビュー削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable Long id
    ) {
        String userId = securityUtils.getCurrentUserId();
        reviewService.deleteReviewByUserId(id, userId);
        return ResponseEntity.noContent().build();
    }
}