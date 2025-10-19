package com.example.my_books_backend.service.impl;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.ReviewMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;
import com.example.my_books_backend.service.BookStatsService;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    private final BookRepository bookRepository;
    private final BookStatsService bookStatsService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<ReviewResponse> getUserReviews(
        User user,
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.REVIEW_ALLOWED_FIELDS
        );
        Page<Review> pageObj = (bookId == null)
            ? reviewRepository.findByUserAndIsDeletedFalse(user, pageable)
            : reviewRepository.findByUserAndIsDeletedFalseAndBookId(user, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Review> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            reviewRepository::findAllByIdInWithRelations,
            Review::getId
        );

        return reviewMapper.toPageResponse(updatedPageObj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewStatsResponse getBookReviewStats(String bookId) {
        return reviewRepository.getReviewStatsResponse(bookId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, User user) {
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new NotFoundException("Book not found"));

        Optional<Review> existingReview = reviewRepository.findByUserAndBook(user, book);

        Review review = new Review();
        if (existingReview.isPresent()) {
            review = existingReview.get();
            if (review.getIsDeleted()) {
                review.setIsDeleted(false);
            } else {
                throw new ConflictException("すでにこの書籍にはレビューが登録されています。");
            }
        }
        review.setUser(user);
        review.setBook(book);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(savedReview.getBook().getId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request, User user) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このレビューを編集する権限がありません。");
        }

        String comment = request.getComment();
        Double rating = request.getRating();

        if (comment != null) {
            review.setComment(comment);
        }

        if (rating != null) {
            review.setRating(rating);
        }

        Review savedReview = reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(savedReview.getBook().getId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteReview(Long id, User user) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このレビューを削除する権限がありません");
        }

        review.setIsDeleted(true);
        reviewRepository.save(review);

        // 書籍の統計情報（レビュー数、平均評価、人気度）を更新
        bookStatsService.updateBookStats(review.getBook().getId());
    }
}
