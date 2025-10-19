package com.example.my_books_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.User;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    // ユーザーが追加したブックマークを取得
    Page<Bookmark> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    // ユーザーが追加したブックマークを取得（書籍ID指定）
    Page<Bookmark> findByUserAndIsDeletedFalseAndPageContent_BookId(User user, String bookId, Pageable pageable);

    // ユーザーが追加したブックマークを取得（ページコンテンツ指定）
    Optional<Bookmark> findByUserAndPageContent(User user, BookChapterPageContent pageContent);

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
}
