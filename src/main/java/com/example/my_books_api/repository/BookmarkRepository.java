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
import com.example.my_books_api.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    // 1件取得
    Optional<Bookmark> findByIdAndIsDeletedFalse(Long id);

    // ユーザーが追加したブックマークを取得
    Page<Bookmark> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    // ユーザーが追加したブックマークを取得（書籍ID指定）
    Page<Bookmark> findByUserIdAndIsDeletedFalseAndPageContentBookId(String userId, String bookId, Pageable pageable);

    // ユーザーが追加したブックマークを取得（ページコンテンツ指定）
    Optional<Bookmark> findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
        String userId,
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );

    // 2クエリ戦略用：IDリストから関連データを含むリストを取得
    @Query("""
        SELECT DISTINCT b
        FROM Bookmark b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.pageContent pc
        LEFT JOIN FETCH pc.book book
        LEFT JOIN FETCH book.genres
        WHERE b.id IN :ids
        """)
    List<Bookmark> findAllByIdInWithRelations(@Param("ids") List<Long> ids);

    // 書籍IDでブックマークを一括ソフト削除
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Bookmark b SET b.isDeleted = true WHERE b.pageContent.id IN (SELECT pc.id FROM BookChapterPageContent pc WHERE pc.bookId = :bookId)")
    void softDeleteAllByBookId(@Param("bookId") String bookId);
}