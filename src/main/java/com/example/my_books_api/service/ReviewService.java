package com.example.my_books_api.service;

import org.springframework.lang.NonNull;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.review.ReviewStatsResponse;
import com.example.my_books_api.dto.review.ReviewRequest;
import com.example.my_books_api.dto.review.ReviewResponse;

public interface ReviewService {
    /**
     * 書籍に対するレビュー数などを取得 （レビュー数・平均評価点）
     *
     * @param bookId 書籍ID
     * @return レビュー数など
     */
    ReviewStatsResponse getBookReviewStats(@NonNull String bookId);

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
     * ユーザーが投稿したレビューを取得（ページネーション用）
     *
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return レビューリスト
     */
    PageResponse<ReviewResponse> getUserReviews(
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * レビューを作成
     *
     * @param request レビュー作成リクエスト
     * @return 作成されたレビュー情報
     */
    ReviewResponse createReview(ReviewRequest request);

    /**
     * レビューを更新
     *
     * @param id 更新するレビューのID
     * @param request レビュー更新リクエスト
     * @return 更新されたレビュー情報
     */
    ReviewResponse updateReview(@NonNull Long id, ReviewRequest request);

    /**
     * レビューを削除
     *
     * @param id 削除するレビューのID
     */
    void deleteReview(@NonNull Long id);
}
