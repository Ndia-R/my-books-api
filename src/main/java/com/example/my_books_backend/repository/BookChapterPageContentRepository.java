package com.example.my_books_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.dto.book_chapter.BookChapterResponse;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.entity.BookChapterPageContent;

@Repository
public interface BookChapterPageContentRepository extends JpaRepository<BookChapterPageContent, Long> {
    // 書籍ID・章番号・ページ番号から書籍ページコンテンツを取得
    Optional<BookChapterPageContent> findByBookIdAndChapterNumberAndPageNumber(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );

    // 書籍IDから章一覧情報を取得（章番号、章タイトル、ページ数）
    @Query("""
        SELECT NEW com.example.my_books_backend.dto.book_chapter.BookChapterResponse(
            p.chapterNumber,
            c.title,
            COUNT(p.pageNumber)
        )
        FROM BookChapterPageContent p
        JOIN BookChapter c ON p.bookId = c.id.bookId
                           AND p.chapterNumber = c.id.chapterNumber
        WHERE p.bookId = :bookId
        AND p.isDeleted = false
        GROUP BY p.chapterNumber, c.title
        ORDER BY p.chapterNumber
        """)
    List<BookChapterResponse> findChapterResponsesByBookId(String bookId);

    // 書籍ページコンテンツの詳細情報を取得（章タイトル、最大ページ数、コンテンツ含む）
    @Query("""
        SELECT NEW com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse(
            p.bookId,
            p.chapterNumber,
            c.title,
            p.pageNumber,
            (SELECT MAX(p2.pageNumber)
             FROM BookChapterPageContent p2
             WHERE p2.bookId = p.bookId
             AND p2.chapterNumber = p.chapterNumber
             AND p2.isDeleted = false),
            p.content
        )
        FROM BookChapterPageContent p
        JOIN BookChapter c ON p.bookId = c.id.bookId
                           AND p.chapterNumber = c.id.chapterNumber
        WHERE p.bookId = :bookId
        AND p.chapterNumber = :chapterNumber
        AND p.pageNumber = :pageNumber
        AND p.isDeleted = false
        """)
    Optional<BookChapterPageContentResponse> findChapterPageContentResponse(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );
}
