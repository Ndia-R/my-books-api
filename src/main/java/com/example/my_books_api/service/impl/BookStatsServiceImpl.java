package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.review.ReviewStatsResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.repository.BookRepository;
import com.example.my_books_api.repository.ReviewRepository;
import com.example.my_books_api.service.BookStatsService;
import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookStatsServiceImpl implements BookStatsService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void updateBookStats(@NonNull String bookId) {
        // 削除されていない有効な本か確認（削除済みの本は統計更新の対象から外す）
        Book book = bookRepository.findByIdAndIsDeletedFalse(bookId)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        // レビュー統計の一括取得
        ReviewStatsResponse reviewStats = reviewRepository.getReviewStatsResponse(bookId);
        long reviewCount = reviewStats.getReviewCount();
        double averageRating = reviewStats.getAverageRating();

        // 人気度の計算
        double popularity = calculatePopularity(reviewCount, averageRating);

        // 数値のセット（小数点以下2桁で丸め）
        book.setReviewCount(reviewCount);
        book.setAverageRating(Math.round(averageRating * 100.0) / 100.0);
        book.setPopularity(Math.round(popularity * 100.0) / 100.0);

        bookRepository.save(book);
    }

    /**
     * レビューに基づく人気度計算
     * 計算式: 平均評価 × log1p(レビュー数) × 20
     * @param reviewCount 有効なレビュー数
     * @param averageRating 平均評価（0.0-5.0）
     * @return 人気度スコア
     */
    private double calculatePopularity(long reviewCount, double averageRating) {
        if (reviewCount == 0 || averageRating == 0.0) {
            return 0.0;
        }

        // 基本的な重み付きスコア
        double logWeight = Math.log1p(reviewCount);
        double popularity = averageRating * logWeight * 20;

        return popularity;
    }
}