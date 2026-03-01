package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.review.ReviewRequest;
import com.example.my_books_api.dto.review.ReviewResponse;
import com.example.my_books_api.dto.review.ReviewStatsResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.Review;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.ForbiddenException;
import com.example.my_books_api.exception.UpgradeRequiredException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.ReviewMapper;
import com.example.my_books_api.repository.BookRepository;
import com.example.my_books_api.repository.ReviewRepository;
import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.BookStatsService;
import com.example.my_books_api.service.ReviewService;
import com.example.my_books_api.service.SubscriptionService;
import com.example.my_books_api.util.JwtClaimExtractor;
import com.example.my_books_api.util.PageableUtils;
import com.example.my_books_api.util.SecurityContextUtils;
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
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    private final BookRepository bookRepository;
    private final BookStatsService bookStatsService;
    private final UserRepository userRepository;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final SecurityContextUtils securityContextUtils;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ReviewStatsResponse getBookReviewStats(@NonNull String bookId) {
        return reviewRepository.getReviewStatsResponse(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('review:read:all')")
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
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('review:manage:own', 'review:read:all')")
    public PageResponse<ReviewResponse> getUserReviews(
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        String userId = jwtClaimExtractor.getUserId();

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
    @PreAuthorize("hasAuthority('review:manage:own')")
    public ReviewResponse createReview(ReviewRequest request) {
        String userId = jwtClaimExtractor.getUserId();

        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "レビューの投稿にはPREMIUMプランへのアップグレードが必要です"
            );
        }

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
    @PreAuthorize("hasAuthority('review:manage:own')")
    public ReviewResponse updateReview(@NonNull Long id, ReviewRequest request) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        String userId = jwtClaimExtractor.getUserId();
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("更新する権限がありません");
        }

        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "レビューの更新にはPREMIUMプランへのアップグレードが必要です"
            );
        }

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
    @PreAuthorize("hasAnyAuthority('review:manage:own', 'review:delete:all')")
    public void deleteReview(@NonNull Long id) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        // review:delete:all 権限がない場合は、所有者チェック + subscriptionPlanチェックを行う
        if (!securityContextUtils.hasAuthority("review:delete:all")) {
            String userId = jwtClaimExtractor.getUserId();
            if (!review.getUser().getId().equals(userId)) {
                throw new ForbiddenException("削除する権限がありません");
            }
            if (!subscriptionService.isPremium(userId)) {
                throw new UpgradeRequiredException(
                    "レビューの削除にはPREMIUMプランへのアップグレードが必要です"
                );
            }
        }

        review.setIsDeleted(true);
        reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(review.getBook().getId());
    }
}
