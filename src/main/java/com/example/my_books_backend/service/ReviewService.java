package com.example.my_books_backend.service;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.dto.review.ReviewRequest;
import com.example.my_books_backend.dto.review.ReviewResponse;

public interface ReviewService {
    /**
     * ユーザーが投稿したレビューを取得（ページネーション用）
     * 
     * @param user ユーザーエンティティ
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return レビューリスト
     */
    PageResponse<ReviewResponse> getUserReviews(
        User user,
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
     * @param user ユーザーエンティティ
     * @return 作成されたレビュー情報
     */
    ReviewResponse createReview(ReviewRequest request, User user);

    /**
     * レビューを更新
     * 
     * @param id 更新するレビューのID
     * @param request レビュー更新リクエスト
     * @param user ユーザーエンティティ
     * @return 更新されたレビュー情報
     */
    ReviewResponse updateReview(Long id, ReviewRequest request, User user);

    /**
     * レビューを削除
     * 
     * @param id 削除するレビューのID
     * @param user ユーザーエンティティ
     */
    void deleteReview(Long id, User user);
}
