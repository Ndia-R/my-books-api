package com.example.my_books_backend.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    // 書籍一覧取得
    Page<Book> findByIsDeletedFalse(Pageable pageable);

    // タイトル検索
    Page<Book> findByTitleContainingAndIsDeletedFalse(String keyword, Pageable pageable);

    // 指定されたジャンルIDのリストを取得（OR条件）
    Page<Book> findDistinctByGenres_IdInAndIsDeletedFalse(List<Long> genreIds, Pageable pageable);

    // 指定されたジャンルIDのリストを取得（AND条件）
    @Query("""
        SELECT DISTINCT b
        FROM Book b
        WHERE b.id IN (
            SELECT b2.id
            FROM Book b2
            JOIN b2.genres bg
            WHERE bg.id IN :genreIds
            GROUP BY b2.id
            HAVING COUNT(DISTINCT bg.id) = :size
        )
        """)
    Page<Book> findBooksHavingAllGenres(
        @Param("genreIds") List<Long> genreIds,
        @Param("size") Long size,
        Pageable pageable
    );

    // 2クエリ戦略用：IDリストから関連データを含むリストを取得
    @Query("""
        SELECT DISTINCT b
        FROM Book b
        LEFT JOIN FETCH b.genres
        WHERE b.id IN :ids
        """)
    List<Book> findAllByIdInWithRelations(@Param("ids") List<String> ids);
}
