package com.example.my_books_backend.service.impl;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.ReviewMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.BookStatsService;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.util.JwtClaimExtractor;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("isAuthenticated()")
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    private final BookRepository bookRepository;
    private final BookStatsService bookStatsService;
    private final UserRepository userRepository;
    private final JwtClaimExtractor jwtClaimExtractor;

    @Override
    @PreAuthorize("permitAll()")
    public ReviewStatsResponse getBookReviewStats(@NonNull String bookId) {
        return reviewRepository.getReviewStatsResponse(bookId);
    }

    @Override
    @PreAuthorize("permitAll()")
    public PageResponse<ReviewResponse> getBookReviews(
        String bookId,
        Long page,
        Long size,
        String sortString
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.REVIEW_ALLOWED_FIELDS
        );
        Page<Review> pageObj = reviewRepository.findByBookIdAndIsDeletedFalse(bookId, pageable);

        // 2クエリ戦略を適用
        Page<Review> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            reviewRepository::findAllByIdInWithRelations,
            Review::getId
        );

        return reviewMapper.toPageResponse(updatedPageObj);
    }

    @Override
    public PageResponse<ReviewResponse> getUserReviews(
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();

        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.REVIEW_ALLOWED_FIELDS
        );
        Page<Review> pageObj = (bookId == null)
            ? reviewRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
            : reviewRepository.findByUserIdAndIsDeletedFalseAndBookId(userId, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Review> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            reviewRepository::findAllByIdInWithRelations,
            Review::getId
        );

        return reviewMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('review:write:own')")
    public ReviewResponse createReview(ReviewRequest request) {
        String userId = jwtClaimExtractor.getCurrentUserId();

        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new NotFoundException("Book not found"));

        Optional<Review> existingReview = reviewRepository.findByUserIdAndBookId(userId, request.getBookId());

        Review review;
        if (existingReview.isPresent()) {
            review = existingReview.get();
            if (review.getIsDeleted()) {
                review.setIsDeleted(false);
                review.setCreatedAt(LocalDateTime.now());
            } else {
                throw new ConflictException("すでにこの書籍にはレビューが登録されています。");
            }
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
            review = new Review();
            review.setUser(user);
        }
        review.setBook(book);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(savedReview.getBook().getId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    @Override
    @Transactional
    @PreAuthorize("@reviewService.isReviewOwner(#id, principal.claims['sub'])")
    public ReviewResponse updateReview(@NonNull Long id, ReviewRequest request) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }

        Review savedReview = reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(savedReview.getBook().getId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    @Override
    @Transactional
    // 「管理権限（delete:any）を持っている」または「ログインユーザーがレビューの所有者である」場合に許可
    @PreAuthorize("hasRole('review:delete:any') or @reviewService.isReviewOwner(#id, principal.claims['sub'])")
    public void deleteReview(@NonNull Long id) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        review.setIsDeleted(true);
        reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(review.getBook().getId());
    }

    @Transactional(readOnly = true)
    public boolean isReviewOwner(@NonNull Long reviewId, String userId) {
        return reviewRepository.findById(reviewId)
            .map(review -> review.getUser().getId().equals(userId))
            .orElse(false);
    }
}
