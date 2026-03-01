package com.example.my_books_api.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_api.dto.review.ReviewStatsResponse;
import com.example.my_books_api.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 1件取得
    Optional<Review> findByIdAndIsDeletedFalse(Long id);

    // ユーザーが投稿したレビューを取得
    Page<Review> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    // ユーザーが投稿したレビューを取得（書籍ID指定）
    Page<Review> findByUserIdAndIsDeletedFalseAndBookId(String userId, String bookId, Pageable pageable);

    // ユーザーが投稿したレビューを取得（書籍指定）
    Optional<Review> findByUserIdAndBookId(String userId, String bookId);

    // 特定の書籍のレビューを取得
    Page<Review> findByBookIdAndIsDeletedFalse(String bookId, Pageable pageable);

    // 2クエリ戦略用：IDリストから関連データを含むリストを取得
    @Query("""
        SELECT DISTINCT r
        FROM Review r
        LEFT JOIN FETCH r.user
        LEFT JOIN FETCH r.book b
        LEFT JOIN FETCH b.genres
        WHERE r.id IN :ids
        """)
    List<Review> findAllByIdInWithRelations(@Param("ids") List<Long> ids);

    // 特定の書籍に対するレビュー数と平均評価を取得
    @Query("""
        SELECT new com.example.my_books_api.dto.review.ReviewStatsResponse(
            :bookId,
            COALESCE(COUNT(r), 0L),
            COALESCE(AVG(r.rating), 0.0)
        )
        FROM Review r
        WHERE r.book.id = :bookId
        AND r.isDeleted = false
        """)
    ReviewStatsResponse getReviewStatsResponse(@Param("bookId") String bookId);

    // 書籍IDでレビューを一括ソフト削除
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.isDeleted = true WHERE r.book.id = :bookId")
    void softDeleteAllByBookId(@Param("bookId") String bookId);
}