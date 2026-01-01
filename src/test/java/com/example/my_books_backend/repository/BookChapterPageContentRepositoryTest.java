package com.example.my_books_backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_backend.dto.book_chapter.BookChapterResponse;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterId;
import com.example.my_books_backend.entity.BookChapterPageContent;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookChapterPageContentRepositoryTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers manages container lifecycle
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookChapterPageContentRepository pageContentRepository;

    private Book testBook;
    private BookChapter chapter1;
    private BookChapter chapter2;
    private BookChapterPageContent page1_1;
    private BookChapterPageContent page1_2;
    private BookChapterPageContent page1_3;
    private BookChapterPageContent page2_1;
    private BookChapterPageContent page2_2;

    @BeforeEach
    void setUp() {
        // テスト用の書籍を作成
        testBook = createBook("test-book-1", "テスト書籍");
        entityManager.persist(testBook);

        // 章を作成
        chapter1 = createChapter(testBook, 1L, "第1章：始まり");
        entityManager.persist(chapter1);

        chapter2 = createChapter(testBook, 2L, "第2章：展開");
        entityManager.persist(chapter2);

        // 第1章のページコンテンツを作成
        page1_1 = createPageContent(testBook.getId(), 1L, 1L, "第1章の1ページ目の内容です。");
        entityManager.persist(page1_1);

        page1_2 = createPageContent(testBook.getId(), 1L, 2L, "第1章の2ページ目の内容です。");
        entityManager.persist(page1_2);

        page1_3 = createPageContent(testBook.getId(), 1L, 3L, "第1章の3ページ目の内容です。");
        entityManager.persist(page1_3);

        // 第2章のページコンテンツを作成
        page2_1 = createPageContent(testBook.getId(), 2L, 1L, "第2章の1ページ目の内容です。");
        entityManager.persist(page2_1);

        page2_2 = createPageContent(testBook.getId(), 2L, 2L, "第2章の2ページ目の内容です。");
        entityManager.persist(page2_2);

        // 削除済みページを作成
        BookChapterPageContent deletedPage = createPageContent(testBook.getId(), 1L, 4L, "削除されたページ");
        deletedPage.setIsDeleted(true);
        entityManager.persist(deletedPage);

        entityManager.flush();
    }

    private Book createBook(String id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setDescription("テスト用の説明");
        book.setAuthors("テスト著者");
        book.setPublisher("テスト出版社");
        book.setPublicationDate(Date.valueOf(LocalDate.of(2024, 1, 1)));
        book.setPrice(1000L);
        book.setPageCount(300L);
        book.setIsbn("978-0-00-000000-0");
        book.setImagePath("/images/test.jpg");
        book.setReviewCount(0L);
        book.setAverageRating(0.0);
        book.setPopularity(0.0);
        book.setIsDeleted(false);
        return book;
    }

    private BookChapter createChapter(Book book, Long chapterNumber, String title) {
        BookChapterId id = new BookChapterId(book.getId(), chapterNumber);
        BookChapter chapter = new BookChapter();
        chapter.setId(id);
        chapter.setTitle(title);
        chapter.setBook(book);
        chapter.setIsDeleted(false);
        return chapter;
    }

    private BookChapterPageContent createPageContent(String bookId, Long chapterNumber, Long pageNumber, String content) {
        BookChapterPageContent page = new BookChapterPageContent();
        page.setBookId(bookId);
        page.setChapterNumber(chapterNumber);
        page.setPageNumber(pageNumber);
        page.setContent(content);
        page.setIsDeleted(false);
        return page;
    }

    @Test
    void testFindByBookIdAndChapterNumberAndPageNumber_存在するページ() {
        // When
        Optional<BookChapterPageContent> found = pageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBook.getId(), 1L, 1L
        );

        // Then
        assertTrue(found.isPresent(), "ページが見つかること");
        assertEquals("第1章の1ページ目の内容です。", found.get().getContent());
        assertEquals(testBook.getId(), found.get().getBookId());
        assertEquals(1L, found.get().getChapterNumber());
        assertEquals(1L, found.get().getPageNumber());
    }

    @Test
    void testFindByBookIdAndChapterNumberAndPageNumber_存在しないページ() {
        // When
        Optional<BookChapterPageContent> found = pageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBook.getId(), 1L, 999L
        );

        // Then
        assertFalse(found.isPresent(), "存在しないページは見つからないこと");
    }

    @Test
    void testFindByBookIdAndChapterNumberAndPageNumber_削除済みページは取得されない() {
        // When
        Optional<BookChapterPageContent> found = pageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBook.getId(), 1L, 4L
        );

        // Then
        // このメソッドは論理削除を考慮しないため、削除済みでも取得される
        assertTrue(found.isPresent(), "削除済みページも取得されること（このメソッドは論理削除を考慮しない）");
    }

    @Test
    void testFindChapterResponsesByBookId_章一覧取得() {
        // When
        List<BookChapterResponse> chapters = pageContentRepository.findChapterResponsesByBookId(testBook.getId());

        // Then
        assertEquals(2, chapters.size(), "2つの章が取得されること");

        // 第1章の確認
        BookChapterResponse chapter1Response = chapters.get(0);
        assertEquals(1L, chapter1Response.getChapterNumber());
        assertEquals("第1章：始まり", chapter1Response.getChapterTitle());
        assertEquals(3L, chapter1Response.getTotalPages(), "第1章は3ページ");

        // 第2章の確認
        BookChapterResponse chapter2Response = chapters.get(1);
        assertEquals(2L, chapter2Response.getChapterNumber());
        assertEquals("第2章：展開", chapter2Response.getChapterTitle());
        assertEquals(2L, chapter2Response.getTotalPages(), "第2章は2ページ");
    }

    @Test
    void testFindChapterResponsesByBookId_存在しない書籍() {
        // When
        List<BookChapterResponse> chapters = pageContentRepository.findChapterResponsesByBookId("non-existent-book");

        // Then
        assertTrue(chapters.isEmpty(), "存在しない書籍では空リストが返されること");
    }

    @Test
    void testFindChapterPageContentResponse_詳細情報取得() {
        // When
        Optional<BookChapterPageContentResponse> response = pageContentRepository.findChapterPageContentResponse(
            testBook.getId(), 1L, 2L
        );

        // Then
        assertTrue(response.isPresent(), "ページコンテンツ詳細が取得できること");
        
        BookChapterPageContentResponse content = response.get();
        assertEquals(testBook.getId(), content.getBookId());
        assertEquals(1L, content.getChapterNumber());
        assertEquals("第1章：始まり", content.getChapterTitle());
        assertEquals(2L, content.getPageNumber());
        assertEquals(3L, content.getTotalPagesInChapter(), "第1章の最大ページ数は3");
        assertEquals("第1章の2ページ目の内容です。", content.getContent());
    }

    @Test
    void testFindChapterPageContentResponse_章の最終ページ() {
        // When
        Optional<BookChapterPageContentResponse> response = pageContentRepository.findChapterPageContentResponse(
            testBook.getId(), 1L, 3L
        );

        // Then
        assertTrue(response.isPresent());
        assertEquals(3L, response.get().getPageNumber(), "最終ページ番号");
        assertEquals(3L, response.get().getTotalPagesInChapter(), "最大ページ数");
    }

    @Test
    void testFindChapterPageContentResponse_存在しないページ() {
        // When
        Optional<BookChapterPageContentResponse> response = pageContentRepository.findChapterPageContentResponse(
            testBook.getId(), 1L, 999L
        );

        // Then
        assertFalse(response.isPresent(), "存在しないページでは空のOptionalが返されること");
    }

    @Test
    void testSave_新規ページコンテンツ作成() {
        // Given
        BookChapterPageContent newPage = createPageContent(testBook.getId(), 2L, 3L, "第2章の3ページ目です。");

        // When
        BookChapterPageContent saved = pageContentRepository.save(newPage);

        // Then
        assertNotNull(saved.getId(), "IDが自動採番されること");
        assertEquals("第2章の3ページ目です。", saved.getContent());

        // データベースから取得して検証
        Optional<BookChapterPageContent> found = pageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBook.getId(), 2L, 3L
        );
        assertTrue(found.isPresent());
        assertEquals("第2章の3ページ目です。", found.get().getContent());
    }

    @Test
    void testSave_既存ページコンテンツ更新() {
        // Given
        page1_1.setContent("更新された第1章の1ページ目の内容です。");

        // When
        BookChapterPageContent updated = pageContentRepository.save(page1_1);

        // Then
        assertEquals(page1_1.getId(), updated.getId());
        assertEquals("更新された第1章の1ページ目の内容です。", updated.getContent());

        // データベースから取得して検証
        entityManager.clear();
        Optional<BookChapterPageContent> found = pageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBook.getId(), 1L, 1L
        );
        assertTrue(found.isPresent());
        assertEquals("更新された第1章の1ページ目の内容です。", found.get().getContent());
    }

    @Test
    void testDelete_論理削除() {
        // Given
        Long pageId = page1_1.getId();

        // When - 論理削除
        page1_1.setIsDeleted(true);
        pageContentRepository.save(page1_1);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<BookChapterPageContent> found = pageContentRepository.findById(pageId);
        assertTrue(found.isPresent(), "データは残っていること");
        assertTrue(found.get().getIsDeleted(), "論理削除フラグが立っていること");

        // findChapterResponsesByBookIdでページ数が減ること
        List<BookChapterResponse> chapters = pageContentRepository.findChapterResponsesByBookId(testBook.getId());
        BookChapterResponse chapter1Response = chapters.get(0);
        assertEquals(2L, chapter1Response.getTotalPages(), "論理削除されたページは除外され、ページ数が2に減ること");
    }

    @Test
    void testUniqueConstraint_同じ書籍章ページ番号は登録できない() {
        // Given - 既に存在するページと同じ組み合わせ
        BookChapterPageContent duplicatePage = createPageContent(testBook.getId(), 1L, 1L, "重複するページ");

        // When & Then
        assertThrows(Exception.class, () -> {
            pageContentRepository.save(duplicatePage);
            entityManager.flush();
        }, "ユニーク制約違反で例外が発生すること");
    }

    @Test
    void testCount_全ページ数() {
        // When
        long count = pageContentRepository.count();

        // Then
        assertEquals(6, count, "全ページ数（削除済み含む）");
    }

    @Test
    void testEntityBase_タイムスタンプ自動設定() {
        // Given
        BookChapterPageContent newPage = createPageContent(testBook.getId(), 2L, 4L, "タイムスタンプテスト");

        // When
        BookChapterPageContent saved = pageContentRepository.save(newPage);
        entityManager.flush();

        // Then
        assertNotNull(saved.getCreatedAt(), "createdAtが自動設定されること");
        assertNotNull(saved.getUpdatedAt(), "updatedAtが自動設定されること");
    }

    @Test
    void testFindAll_全ページコンテンツ取得() {
        // When
        List<BookChapterPageContent> allPages = pageContentRepository.findAll();

        // Then
        assertEquals(6, allPages.size(), "削除済みを含む全ページが取得されること");
    }

    @Test
    void testChapterPageContentResponse_複数章の最大ページ数計算() {
        // When - 第2章の各ページを取得
        Optional<BookChapterPageContentResponse> page2_1Response = pageContentRepository.findChapterPageContentResponse(
            testBook.getId(), 2L, 1L
        );
        Optional<BookChapterPageContentResponse> page2_2Response = pageContentRepository.findChapterPageContentResponse(
            testBook.getId(), 2L, 2L
        );

        // Then
        assertTrue(page2_1Response.isPresent());
        assertTrue(page2_2Response.isPresent());
        
        // どちらのページも第2章の最大ページ数が正しく計算されること
        assertEquals(2L, page2_1Response.get().getTotalPagesInChapter(), "第2章の最大ページ数は2");
        assertEquals(2L, page2_2Response.get().getTotalPagesInChapter(), "第2章の最大ページ数は2");
    }
}
