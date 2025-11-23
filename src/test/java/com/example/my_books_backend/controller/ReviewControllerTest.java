package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.util.JwtClaimExtractor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtClaimExtractor jwtClaimExtractor;

    @Test
    void testCreateReview_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":5.0,\"comment\":\"Great book!\"}";

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).createReviewByUserId(any(ReviewRequest.class), anyString());
    }

    @Test
    void testCreateReview_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":5.0,\"comment\":\"Great book!\"}";

        ReviewResponse response = new ReviewResponse();
        response.setId(1L);
        response.setUserId(userId);
        response.setRating(5.0);
        response.setComment("Great book!");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.createReviewByUserId(any(ReviewRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/reviews/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.rating").value(5.0))
            .andExpect(jsonPath("$.comment").value("Great book!"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).createReviewByUserId(any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testCreateReview_バリデーションエラー_評価なし() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"comment\":\"Great book!\"}";

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).createReviewByUserId(any(ReviewRequest.class), anyString());
    }

    @Test
    void testCreateReview_バリデーションエラー_bookIdなし() throws Exception {
        // Given
        String requestBody = "{\"rating\":5.0,\"comment\":\"Great book!\"}";

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).createReviewByUserId(any(ReviewRequest.class), anyString());
    }

    @Test
    void testCreateReview_既存レビューが存在_409Conflict() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":5.0,\"comment\":\"Great book!\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.createReviewByUserId(any(ReviewRequest.class), eq(userId)))
            .thenThrow(new ConflictException("既にレビューが存在します"));

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isConflict());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).createReviewByUserId(any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testCreateReview_存在しない書籍_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"non-existent-book\",\"rating\":5.0,\"comment\":\"Great book!\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.createReviewByUserId(any(ReviewRequest.class), eq(userId)))
            .thenThrow(new NotFoundException("書籍が見つかりません"));

        // When & Then
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).createReviewByUserId(any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testUpdateReview_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long reviewId = 1L;
        String requestBody = "{\"rating\":4.0,\"comment\":\"Updated comment\"}";

        // When & Then
        mockMvc.perform(put("/reviews/{id}", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).updateReviewByUserId(anyLong(), any(ReviewRequest.class), anyString());
    }

    @Test
    void testUpdateReview_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":4.0,\"comment\":\"Updated comment\"}";

        ReviewResponse response = new ReviewResponse();
        response.setId(reviewId);
        response.setUserId(userId);
        response.setRating(4.0);
        response.setComment("Updated comment");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/reviews/{id}", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reviewId))
            .andExpect(jsonPath("$.rating").value(4.0))
            .andExpect(jsonPath("$.comment").value("Updated comment"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testUpdateReview_存在しないレビュー_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 999L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":4.0,\"comment\":\"Updated comment\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId)))
            .thenThrow(new NotFoundException("レビューが見つかりません"));

        // When & Then
        mockMvc.perform(put("/reviews/{id}", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testUpdateReview_他人のレビュー_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":4.0,\"comment\":\"Updated comment\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId)))
            .thenThrow(new ForbiddenException("他人のレビューは編集できません"));

        // When & Then
        mockMvc.perform(put("/reviews/{id}", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId));
    }

    @Test
    void testDeleteReview_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long reviewId = 1L;

        // When & Then
        mockMvc.perform(delete("/reviews/{id}", reviewId))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).deleteReviewByUserId(anyLong(), anyString());
    }

    @Test
    void testDeleteReview_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doNothing().when(reviewService).deleteReviewByUserId(reviewId, userId);

        // When & Then
        mockMvc.perform(delete("/reviews/{id}", reviewId)
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).deleteReviewByUserId(reviewId, userId);
    }

    @Test
    void testDeleteReview_存在しないレビュー_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 999L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new NotFoundException("レビューが見つかりません"))
            .when(reviewService).deleteReviewByUserId(reviewId, userId);

        // When & Then
        mockMvc.perform(delete("/reviews/{id}", reviewId)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).deleteReviewByUserId(reviewId, userId);
    }

    @Test
    void testDeleteReview_他人のレビュー_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new ForbiddenException("他人のレビューは削除できません"))
            .when(reviewService).deleteReviewByUserId(reviewId, userId);

        // When & Then
        mockMvc.perform(delete("/reviews/{id}", reviewId)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).deleteReviewByUserId(reviewId, userId);
    }

    @Test
    void testCreateReview_評価のみでコメントなし() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":5.0}";

        // When & Then - コメントが必須のためバリデーションエラー
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).createReviewByUserId(any(ReviewRequest.class), anyString());
    }

    @Test
    void testUpdateReview_評価のみ更新() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long reviewId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"rating\":3.0,\"comment\":\"Updated\"}";

        ReviewResponse response = new ReviewResponse();
        response.setId(reviewId);
        response.setRating(3.0);

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/reviews/{id}", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(3.0));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).updateReviewByUserId(eq(reviewId), any(ReviewRequest.class), eq(userId));
    }
}
