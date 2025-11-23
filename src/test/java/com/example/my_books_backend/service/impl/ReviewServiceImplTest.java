package com.example.my_books_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.ReviewMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.BookStatsService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookStatsService bookStatsService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Book testBook;
    private Review testReview;
    private String testUserId;
    private String testBookId;

    @BeforeEach
    void setUp() {
        testUserId = "550e8400-e29b-41d4-a716-446655440000";
        testBookId = "test-book-123";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setDisplayName("Test User");

        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Test Book");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setUser(testUser);
        testReview.setBook(testBook);
        testReview.setRating(4.5);
        testReview.setComment("Great book!");
        testReview.setIsDeleted(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserReviews_正常系_bookIdなし() {
        // Given
        List<Review> reviews = Arrays.asList(testReview);
        Page<Review> page = new PageImpl<>(reviews);
        PageResponse<ReviewResponse> expectedResponse = new PageResponse<>();

        when(reviewRepository.findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class)))
            .thenReturn(page);
        when(reviewRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(reviews);
        when(reviewMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<ReviewResponse> result = reviewService.getUserReviews(
            testUserId, 1L, 20L, "updatedAt.desc", null
        );

        // Then
        assertNotNull(result);
        verify(reviewRepository).findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class));
        verify(reviewRepository).findAllByIdInWithRelations(anyList());
        verify(reviewMapper).toPageResponse(any(Page.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserReviews_正常系_bookIdあり() {
        // Given
        List<Review> reviews = Arrays.asList(testReview);
        Page<Review> page = new PageImpl<>(reviews);
        PageResponse<ReviewResponse> expectedResponse = new PageResponse<>();

        when(reviewRepository.findByUserIdAndIsDeletedFalseAndBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class)))
            .thenReturn(page);
        when(reviewRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(reviews);
        when(reviewMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<ReviewResponse> result = reviewService.getUserReviews(
            testUserId, 1L, 20L, "updatedAt.desc", testBookId
        );

        // Then
        assertNotNull(result);
        verify(reviewRepository).findByUserIdAndIsDeletedFalseAndBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class));
        verify(reviewRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBookReviews_正常系() {
        // Given
        List<Review> reviews = Arrays.asList(testReview);
        Page<Review> page = new PageImpl<>(reviews);
        PageResponse<ReviewResponse> expectedResponse = new PageResponse<>();

        when(reviewRepository.findByBookIdAndIsDeletedFalse(eq(testBookId), any(Pageable.class)))
            .thenReturn(page);
        when(reviewRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(reviews);
        when(reviewMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<ReviewResponse> result = reviewService.getBookReviews(
            testBookId, 1L, 3L, "updatedAt.desc"
        );

        // Then
        assertNotNull(result);
        verify(reviewRepository).findByBookIdAndIsDeletedFalse(eq(testBookId), any(Pageable.class));
        verify(reviewRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    void testGetBookReviewStats_正常系() {
        // Given
        ReviewStatsResponse expectedStats = new ReviewStatsResponse();
        expectedStats.setReviewCount(10L);
        expectedStats.setAverageRating(4.5);

        when(reviewRepository.getReviewStatsResponse(testBookId))
            .thenReturn(expectedStats);

        // When
        ReviewStatsResponse result = reviewService.getBookReviewStats(testBookId);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getReviewCount());
        assertEquals(4.5, result.getAverageRating());
        verify(reviewRepository).getReviewStatsResponse(testBookId);
    }

    @Test
    void testCreateReviewByUserId_正常系_新規作成() {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setBookId(testBookId);
        request.setRating(5.0);
        request.setComment("Excellent!");

        ReviewResponse expectedResponse = new ReviewResponse();
        expectedResponse.setId(1L);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class)))
            .thenReturn(testReview);
        when(reviewMapper.toReviewResponse(any(Review.class)))
            .thenReturn(expectedResponse);

        // When
        ReviewResponse result = reviewService.createReviewByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        verify(bookRepository).findById(testBookId);
        verify(reviewRepository).findByUserIdAndBookId(testUserId, testBookId);
        verify(userRepository).findById(testUserId);
        verify(reviewRepository).save(any(Review.class));
        verify(bookStatsService).updateBookStats(testBookId);
    }

    @Test
    void testCreateReviewByUserId_正常系_論理削除済みレビューの復活() {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setBookId(testBookId);
        request.setRating(5.0);
        request.setComment("Excellent!");

        Review deletedReview = new Review();
        deletedReview.setId(1L);
        deletedReview.setUser(testUser);
        deletedReview.setBook(testBook);
        deletedReview.setIsDeleted(true);

        ReviewResponse expectedResponse = new ReviewResponse();

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.of(deletedReview));
        when(reviewRepository.save(any(Review.class)))
            .thenReturn(deletedReview);
        when(reviewMapper.toReviewResponse(any(Review.class)))
            .thenReturn(expectedResponse);

        // When
        ReviewResponse result = reviewService.createReviewByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        assertFalse(deletedReview.getIsDeleted());
        verify(reviewRepository).save(deletedReview);
        verify(bookStatsService).updateBookStats(testBookId);
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void testCreateReviewByUserId_書籍が存在しない場合() {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setBookId(testBookId);
        request.setRating(5.0);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            reviewService.createReviewByUserId(request, testUserId);
        });
        verify(bookRepository).findById(testBookId);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReviewByUserId_既存レビューが存在する場合() {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setBookId(testBookId);
        request.setRating(5.0);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.of(testReview));

        // When & Then
        assertThrows(ConflictException.class, () -> {
            reviewService.createReviewByUserId(request, testUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReviewByUserId_ユーザーが存在しない場合() {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setBookId(testBookId);
        request.setRating(5.0);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            reviewService.createReviewByUserId(request, testUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testUpdateReviewByUserId_正常系_コメントと評価の両方を更新() {
        // Given
        Long reviewId = 1L;
        ReviewRequest request = new ReviewRequest();
        request.setRating(5.0);
        request.setComment("Updated comment");

        ReviewResponse expectedResponse = new ReviewResponse();

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class)))
            .thenReturn(testReview);
        when(reviewMapper.toReviewResponse(any(Review.class)))
            .thenReturn(expectedResponse);

        // When
        ReviewResponse result = reviewService.updateReviewByUserId(reviewId, request, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(5.0, testReview.getRating());
        assertEquals("Updated comment", testReview.getComment());
        verify(reviewRepository).save(testReview);
        verify(bookStatsService).updateBookStats(testBookId);
    }

    @Test
    void testUpdateReviewByUserId_正常系_評価のみ更新() {
        // Given
        Long reviewId = 1L;
        ReviewRequest request = new ReviewRequest();
        request.setRating(3.0);

        ReviewResponse expectedResponse = new ReviewResponse();

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class)))
            .thenReturn(testReview);
        when(reviewMapper.toReviewResponse(any(Review.class)))
            .thenReturn(expectedResponse);

        // When
        ReviewResponse result = reviewService.updateReviewByUserId(reviewId, request, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3.0, testReview.getRating());
        verify(reviewRepository).save(testReview);
    }

    @Test
    void testUpdateReviewByUserId_レビューが存在しない場合() {
        // Given
        Long reviewId = 1L;
        ReviewRequest request = new ReviewRequest();
        request.setRating(5.0);

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            reviewService.updateReviewByUserId(reviewId, request, testUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testUpdateReviewByUserId_権限がない場合() {
        // Given
        Long reviewId = 1L;
        String otherUserId = "other-user-id";
        ReviewRequest request = new ReviewRequest();
        request.setRating(5.0);

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.of(testReview));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            reviewService.updateReviewByUserId(reviewId, request, otherUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testDeleteReviewByUserId_正常系() {
        // Given
        Long reviewId = 1L;

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class)))
            .thenReturn(testReview);

        // When
        reviewService.deleteReviewByUserId(reviewId, testUserId);

        // Then
        assertTrue(testReview.getIsDeleted());
        verify(reviewRepository).save(testReview);
        verify(bookStatsService).updateBookStats(testBookId);
    }

    @Test
    void testDeleteReviewByUserId_レビューが存在しない場合() {
        // Given
        Long reviewId = 1L;

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            reviewService.deleteReviewByUserId(reviewId, testUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testDeleteReviewByUserId_権限がない場合() {
        // Given
        Long reviewId = 1L;
        String otherUserId = "other-user-id";

        when(reviewRepository.findById(reviewId))
            .thenReturn(Optional.of(testReview));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            reviewService.deleteReviewByUserId(reviewId, otherUserId);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
