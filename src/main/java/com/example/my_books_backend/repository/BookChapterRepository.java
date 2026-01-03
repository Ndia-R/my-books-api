package com.example.my_books_backend.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterId;

@Repository
public interface BookChapterRepository extends JpaRepository<BookChapter, BookChapterId> {
    // 書籍IDから章情報を取得（複合主キーのbookIdフィールドを参照）
    List<BookChapter> findById_BookIdAndIsDeletedFalse(String bookId);

    // 複数の章IDから章情報を直接取得
    List<BookChapter> findByIdInAndIsDeletedFalse(Collection<BookChapterId> ids);

    // 書籍IDで章情報を一括ソフト削除
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BookChapter c SET c.isDeleted = true WHERE c.book.id = :bookId")
    void softDeleteAllByBookId(@Param("bookId") String bookId);
}
