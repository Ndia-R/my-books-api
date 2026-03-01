package com.example.my_books_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_api.dto.book_chapter.BookChapterResponse;
import com.example.my_books_api.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookChapter;
import com.example.my_books_api.entity.BookChapterPageContent;
import com.example.my_books_api.entity.BookChapterId;
import com.example.my_books_api.entity.Genre;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookChapterPageContentRepositoryTest {

        @Container
        @SuppressWarnings("resource")
        static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        @Autowired
        private BookChapterPageContentRepository bookChapterPageContentRepository;

        @Autowired
        private TestEntityManager entityManager;

        private Book testBook;
        private BookChapter chapter1;
        private BookChapter chapter2;
        private BookChapterPageContent page1_1;
        private BookChapterPageContent page1_2;
        private BookChapterPageContent page2_1;
        private BookChapterPageContent deletedPage;

        @BeforeEach
        void setUp() {
                entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
                entityManager.getEntityManager()
                        .createNativeQuery("TRUNCATE TABLE book_chapter_page_contents;")
                        .executeUpdate();
                entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_chapters;").executeUpdate();
                entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_genres;").executeUpdate();
                entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE books;").executeUpdate();
                entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
                entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

                // ジャンルの作成
                Genre genre = new Genre();
                genre.setName("テストジャンル");
                genre.setDescription("テスト用のジャンル");
                entityManager.persist(genre);

                // 書籍の作成
                testBook = new Book();
                testBook.setId("book-uuid-001");
                testBook.setTitle("テスト書籍");
                testBook.setDescription("テスト用の書籍");
                testBook.setGenres(List.of(genre));
                testBook.setAuthors("テスト著者");
                testBook.setPublisher("テスト出版社");
                testBook.setPublicationDate(Date.valueOf("2024-01-01"));
                testBook.setPrice(1000L);
                testBook.setPageCount(300L);
                testBook.setIsbn("978-4-1234-5678-9");
                testBook.setImagePath("/images/test.jpg");
                testBook.setReviewCount(0L);
                testBook.setAverageRating(0.0);
                testBook.setPopularity(0.0);
                testBook.setIsDeleted(false);
                entityManager.persist(testBook);

                // 章1
                BookChapterId chapterId1 = new BookChapterId("book-uuid-001", 1L);
                chapter1 = new BookChapter();
                chapter1.setId(chapterId1);
                chapter1.setTitle("第1章：始まり");
                chapter1.setBook(testBook);
                chapter1.setIsDeleted(false);
                entityManager.persist(chapter1);

                // 章2
                BookChapterId chapterId2 = new BookChapterId("book-uuid-001", 2L);
                chapter2 = new BookChapter();
                chapter2.setId(chapterId2);
                chapter2.setTitle("第2章：展開");
                chapter2.setBook(testBook);
                chapter2.setIsDeleted(false);
                entityManager.persist(chapter2);

                // ページコンテンツ: 第1章 ページ1
                page1_1 = new BookChapterPageContent();
                page1_1.setBookId("book-uuid-001");
                page1_1.setChapterNumber(1L);
                page1_1.setPageNumber(1L);
                page1_1.setContent("第1章 ページ1の内容");
                page1_1.setIsDeleted(false);
                entityManager.persist(page1_1);

                // ページコンテンツ: 第1章 ページ2
                page1_2 = new BookChapterPageContent();
                page1_2.setBookId("book-uuid-001");
                page1_2.setChapterNumber(1L);
                page1_2.setPageNumber(2L);
                page1_2.setContent("第1章 ページ2の内容");
                page1_2.setIsDeleted(false);
                entityManager.persist(page1_2);

                // ページコンテンツ: 第2章 ページ1
                page2_1 = new BookChapterPageContent();
                page2_1.setBookId("book-uuid-001");
                page2_1.setChapterNumber(2L);
                page2_1.setPageNumber(1L);
                page2_1.setContent("第2章 ページ1の内容");
                page2_1.setIsDeleted(false);
                entityManager.persist(page2_1);

                // 削除済みページ
                deletedPage = new BookChapterPageContent();
                deletedPage.setBookId("book-uuid-001");
                deletedPage.setChapterNumber(1L);
                deletedPage.setPageNumber(3L);
                deletedPage.setContent("削除済みページの内容");
                deletedPage.setIsDeleted(true);
                entityManager.persist(deletedPage);

                entityManager.flush();
                entityManager.clear();
        }

        @Test
        void contextLoads() {
                assertThat(bookChapterPageContentRepository).isNotNull();
        }

        @Test
        @DisplayName("save - 新規ページコンテンツ作成")
        void testSave_NewBookChapterPageContent() {
                // Given
                BookChapterPageContent newPage = new BookChapterPageContent();
                newPage.setBookId("book-uuid-001");
                newPage.setChapterNumber(2L);
                newPage.setPageNumber(2L);
                newPage.setContent("第2章 ページ2の新規内容");
                newPage.setIsDeleted(false);

                // When
                BookChapterPageContent savedPage = bookChapterPageContentRepository.save(newPage);
                entityManager.flush();

                // Then
                assertThat(savedPage.getId()).isNotNull();
                assertThat(savedPage.getBookId()).isEqualTo("book-uuid-001");
                assertThat(savedPage.getChapterNumber()).isEqualTo(2L);
                assertThat(savedPage.getPageNumber()).isEqualTo(2L);
                assertThat(savedPage.getContent()).isEqualTo("第2章 ページ2の新規内容");
                assertThat(savedPage.getCreatedAt()).isNotNull();
                assertThat(savedPage.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("findByBookIdAndChapterNumberAndPageNumber - 書籍・章・ページで取得成功")
        void testFindByBookIdAndChapterNumberAndPageNumber_Found() {
                // When
                Optional<BookChapterPageContent> result = bookChapterPageContentRepository
                        .findByBookIdAndChapterNumberAndPageNumber("book-uuid-001", 1L, 1L);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getContent()).isEqualTo("第1章 ページ1の内容");
        }

        @Test
        @DisplayName("findByBookIdAndChapterNumberAndPageNumber - 存在しない組み合わせ")
        void testFindByBookIdAndChapterNumberAndPageNumber_NotFound() {
                // When
                Optional<BookChapterPageContent> result = bookChapterPageContentRepository
                        .findByBookIdAndChapterNumberAndPageNumber("book-uuid-001", 1L, 999L);

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByBookIdAndChapterNumberAndPageNumber - 論理削除済み")
        void testFindByBookIdAndChapterNumberAndPageNumber_DeletedContent() {
                // When: 削除済みページを検索
                Optional<BookChapterPageContent> result = bookChapterPageContentRepository
                        .findByBookIdAndChapterNumberAndPageNumber("book-uuid-001", 1L, 3L);

                // Then: 論理削除済みでも取得される（このメソッドはisDeletedでフィルタリングしない）
                assertThat(result).isPresent();
                assertThat(result.get().getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("findChapterResponsesByBookId - 複数章の統計情報を取得")
        void testFindChapterResponsesByBookId_MultipleChapters() {
                // When
                List<BookChapterResponse> result = bookChapterPageContentRepository
                        .findChapterResponsesByBookId("book-uuid-001");

                // Then: 第1章（2ページ）、第2章（1ページ）
                assertThat(result).hasSize(2);

                BookChapterResponse chapter1Response = result.stream()
                        .filter(r -> r.getChapterNumber().equals(1L))
                        .findFirst()
                        .orElseThrow();
                assertThat(chapter1Response.getChapterTitle()).isEqualTo("第1章：始まり");
                assertThat(chapter1Response.getTotalPages()).isEqualTo(2L); // ページ1, 2（削除済みは除外）

                BookChapterResponse chapter2Response = result.stream()
                        .filter(r -> r.getChapterNumber().equals(2L))
                        .findFirst()
                        .orElseThrow();
                assertThat(chapter2Response.getChapterTitle()).isEqualTo("第2章：展開");
                assertThat(chapter2Response.getTotalPages()).isEqualTo(1L); // ページ1のみ
        }

        @Test
        @DisplayName("findChapterResponsesByBookId - 章なし")
        void testFindChapterResponsesByBookId_NoChapters() {
                // When: 存在しない書籍IDで検索
                List<BookChapterResponse> result = bookChapterPageContentRepository
                        .findChapterResponsesByBookId("non-existent-book");

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findChapterResponsesByBookId - 論理削除済みのみ")
        void testFindChapterResponsesByBookId_OnlyDeletedChapters() {
                // Given: 全ページを削除
                page1_1.setIsDeleted(true);
                page1_2.setIsDeleted(true);
                page2_1.setIsDeleted(true);
                bookChapterPageContentRepository.save(page1_1);
                bookChapterPageContentRepository.save(page1_2);
                bookChapterPageContentRepository.save(page2_1);
                entityManager.flush();
                entityManager.clear();

                // When
                List<BookChapterResponse> result = bookChapterPageContentRepository
                        .findChapterResponsesByBookId("book-uuid-001");

                // Then: 論理削除済みは除外されるので結果なし
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findChapterPageContentResponse - サブクエリMAX検証")
        void testFindChapterPageContentResponse_WithMaxPageNumber() {
                // When: 第1章のページ1を取得
                Optional<BookChapterPageContentResponse> result = bookChapterPageContentRepository
                        .findChapterPageContentResponse("book-uuid-001", 1L, 1L);

                // Then
                assertThat(result).isPresent();
                BookChapterPageContentResponse response = result.get();
                assertThat(response.getBookId()).isEqualTo("book-uuid-001");
                assertThat(response.getChapterNumber()).isEqualTo(1L);
                assertThat(response.getChapterTitle()).isEqualTo("第1章：始まり");
                assertThat(response.getPageNumber()).isEqualTo(1L);
                assertThat(response.getTotalPagesInChapter()).isEqualTo(2L); // MAX(pageNumber) = 2
                assertThat(response.getContent()).isEqualTo("第1章 ページ1の内容");
        }

        @Test
        @DisplayName("findChapterPageContentResponse - 存在しないページ")
        void testFindChapterPageContentResponse_NotFound() {
                // When: 存在しないページを検索
                Optional<BookChapterPageContentResponse> result = bookChapterPageContentRepository
                        .findChapterPageContentResponse("book-uuid-001", 1L, 999L);

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findChapterPageContentResponse - 論理削除済みページ")
        void testFindChapterPageContentResponse_DeletedContent() {
                // When: 削除済みページを検索
                Optional<BookChapterPageContentResponse> result = bookChapterPageContentRepository
                        .findChapterPageContentResponse("book-uuid-001", 1L, 3L);

                // Then: isDeleted=falseでフィルタリングされるので取得できない
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("softDeleteAllByBookId - 書籍削除時の一括ソフト削除成功")
        void testSoftDeleteAllByBookId_Success() {
                // When: 書籍IDでページコンテンツを一括ソフト削除
                bookChapterPageContentRepository.softDeleteAllByBookId("book-uuid-001");
                entityManager.flush();
                entityManager.clear();

                // Then: 全てのページがソフト削除される
                List<BookChapterResponse> chapters = bookChapterPageContentRepository
                        .findChapterResponsesByBookId("book-uuid-001");
                assertThat(chapters).isEmpty();

                // DB確認: データは物理的に残っている
                List<BookChapterPageContent> allPages = entityManager.getEntityManager()
                        .createQuery(
                                "SELECT p FROM BookChapterPageContent p WHERE p.bookId = :bookId",
                                BookChapterPageContent.class
                        )
                        .setParameter("bookId", "book-uuid-001")
                        .getResultList();
                assertThat(allPages).hasSize(4); // 全てのデータが残っている（削除済み含む）
                assertThat(allPages).allMatch(BookChapterPageContent::getIsDeleted); // 全てisDeleted=true
        }

        @Test
        @DisplayName("softDeleteAllByBookId - 既に削除済み")
        void testSoftDeleteAllByBookId_AlreadyDeleted() {
                // Given: 全ページを先にソフト削除
                bookChapterPageContentRepository.softDeleteAllByBookId("book-uuid-001");
                entityManager.flush();
                entityManager.clear();

                // When: 再度ソフト削除
                bookChapterPageContentRepository.softDeleteAllByBookId("book-uuid-001");
                entityManager.flush();

                // Then: エラーなく終了
                List<BookChapterResponse> chapters = bookChapterPageContentRepository
                        .findChapterResponsesByBookId("book-uuid-001");
                assertThat(chapters).isEmpty();
        }
}
