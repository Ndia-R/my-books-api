package com.example.my_books_backend.service;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;

public interface ReviewService {
    /**
     * ユーザーが投稿したレビューを取得（ページネーション用）
     *
     * @param userId ユーザーID
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return レビューリスト
     */
    PageResponse<ReviewResponse> getUserReviews(
        String userId,
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * 書籍に対するレビューを取得（ページネーション用）
     * 
     * @param bookId 書籍ID
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @return レビューリスト
     */
    PageResponse<ReviewResponse> getBookReviews(
        String bookId,
        Long page,
        Long size,
        String sortString
    );

    /**
     * 書籍に対するレビュー数などを取得 （レビュー数・平均評価点）
     * 
     * @param bookId 書籍ID
     * @return レビュー数など
     */
    ReviewStatsResponse getBookReviewStats(String bookId);

    /**
     * レビューを作成
     *
     * @param request レビュー作成リクエスト
     * @param userId ユーザーID
     * @return 作成されたレビュー情報
     */
    ReviewResponse createReviewByUserId(ReviewRequest request, String userId);

    /**
     * レビューを更新
     *
     * @param id 更新するレビューのID
     * @param request レビュー更新リクエスト
     * @param userId ユーザーID
     * @return 更新されたレビュー情報
     */
    ReviewResponse updateReviewByUserId(Long id, ReviewRequest request, String userId);

    /**
     * レビューを削除
     *
     * @param id 削除するレビューのID
     * @param userId ユーザーID
     */
    void deleteReviewByUserId(Long id, String userId);
}
