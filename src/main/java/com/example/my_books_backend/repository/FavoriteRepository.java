package com.example.my_books_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.entity.User;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    // ユーザーが追加したお気に入りを取得
    Page<Favorite> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    // ユーザーが追加したお気に入りを取得（書籍ID指定）
    Page<Favorite> findByUserAndIsDeletedFalseAndBookId(User user, String bookId, Pageable pageable);

    // ユーザーが追加したお気に入りを取得（書籍指定）
    Optional<Favorite> findByUserAndBook(User user, Book book);

    // 2クエリ戦略用：IDリストから関連データを含むリストを取得
    @Query("""
        SELECT DISTINCT f
        FROM Favorite f
        LEFT JOIN FETCH f.user
        LEFT JOIN FETCH f.book b
        LEFT JOIN FETCH b.genres
        WHERE f.id IN :ids
        """)
    List<Favorite> findAllByIdInWithRelations(@Param("ids") List<Long> ids);

    // 特定の書籍に対するお気に入り数を取得
    @Query("""
        SELECT new com.example.my_books_backend.dto.favorite.FavoriteStatsResponse(
            :bookId,
            COALESCE(COUNT(f), 0L)
        )
        FROM Favorite f
        WHERE f.book.id = :bookId
        AND f.isDeleted = false
        """)
    FavoriteStatsResponse getFavoriteStatsResponse(@Param("bookId") String bookId);
}
