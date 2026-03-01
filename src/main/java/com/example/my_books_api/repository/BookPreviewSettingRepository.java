package com.example.my_books_api.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_api.entity.BookPreviewSetting;

@Repository
public interface BookPreviewSettingRepository extends JpaRepository<BookPreviewSetting, Long> {
    // 全件取得（論理削除されていないもののみ）
    List<BookPreviewSetting> findByIsDeletedFalse();

    // ページネーション対応全件取得（論理削除されていないもののみ）
    Page<BookPreviewSetting> findByIsDeletedFalse(Pageable pageable);

    // 2クエリ戦略用: IDリストから試し読み設定を取得（リレーション含む）
    @Query("""
        SELECT DISTINCT bps FROM BookPreviewSetting bps
        LEFT JOIN FETCH bps.book
        WHERE bps.id IN :ids
        AND bps.isDeleted = false
        """)
    List<BookPreviewSetting> findAllByIdInWithRelations(@Param("ids") List<Long> ids);

    // 1件取得
    Optional<BookPreviewSetting> findByIdAndIsDeletedFalse(Long id);

    // 1件取得（書籍ID指定）
    Optional<BookPreviewSetting> findByBookIdAndIsDeletedFalse(String bookId);

    // 1件取得（書籍ID指定）（論理削除を含む）
    Optional<BookPreviewSetting> findByBookId(String bookId);

    // 指定された章・ページが試し読み範囲内かチェック
    // 「-1 = 無制限」のセンチネル値に基づいた判定ロジック
    @Query("""
        SELECT CASE
            WHEN bps.maxChapter = -1 THEN true
            WHEN :chapterNumber < bps.maxChapter THEN true
            WHEN :chapterNumber = bps.maxChapter AND (bps.maxPage = -1 OR :pageNumber <= bps.maxPage) THEN true
            ELSE false
        END
        FROM BookPreviewSetting bps
        WHERE bps.book.id = :bookId
        AND bps.isDeleted = false
        """)
    Optional<Boolean> isPreviewAllowed(
        @Param("bookId") String bookId,
        @Param("chapterNumber") Long chapterNumber,
        @Param("pageNumber") Long pageNumber
    );
}
