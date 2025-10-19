package com.example.my_books_backend.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterId;

@Repository
public interface BookChapterRepository extends JpaRepository<BookChapter, BookChapterId> {
    // 書籍IDから章情報を取得
    List<BookChapter> findByBookIdAndIsDeletedFalse(String bookId);

    // 複数の章IDから章情報を直接取得
    List<BookChapter> findByIdInAndIsDeletedFalse(Collection<BookChapterId> ids);
}
