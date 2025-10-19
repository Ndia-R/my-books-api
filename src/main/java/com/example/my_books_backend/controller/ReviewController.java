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
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.ReviewService;
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

    @Operation(description = "レビュー作成")
    @PostMapping("")
    public ResponseEntity<ReviewResponse> createReview(
        @Valid @RequestBody ReviewRequest request
    ) {
        User user = getDemoUser();
        ReviewResponse response = reviewService.createReview(request, user);
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
        User user = getDemoUser();
        ReviewResponse response = reviewService.updateReview(id, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "レビュー削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable Long id
    ) {
        User user = getDemoUser();
        reviewService.deleteReview(id, user);
        return ResponseEntity.noContent().build();
    }
}
