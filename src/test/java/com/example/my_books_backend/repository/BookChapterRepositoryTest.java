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

import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterId;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookChapterRepositoryTest {

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
    private BookChapterRepository bookChapterRepository;

    private Book testBook;
    private BookChapter chapter1;
    private BookChapter chapter2;
    private BookChapter chapter3;

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

        chapter3 = createChapter(testBook, 3L, "第3章：結末");
        entityManager.persist(chapter3);

        // 削除済みの章を作成
        BookChapter deletedChapter = createChapter(testBook, 4L, "削除された章");
        deletedChapter.setIsDeleted(true);
        entityManager.persist(deletedChapter);

        // 別の書籍の章を作成
        Book anotherBook = createBook("test-book-2", "別の書籍");
        entityManager.persist(anotherBook);

        BookChapter anotherBookChapter = createChapter(anotherBook, 1L, "別の書籍の章");
        entityManager.persist(anotherBookChapter);

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

    @Test
    void testFindById_存在する章() {
        // Given
        BookChapterId id = new BookChapterId(testBook.getId(), 1L);

        // When
        Optional<BookChapter> found = bookChapterRepository.findById(id);

        // Then
        assertTrue(found.isPresent(), "章が見つかること");
        assertEquals("第1章：始まり", found.get().getTitle());
        assertEquals(testBook.getId(), found.get().getId().getBookId());
        assertEquals(1L, found.get().getId().getChapterNumber());
    }

    @Test
    void testFindById_存在しない章() {
        // Given
        BookChapterId id = new BookChapterId("non-existent-book", 1L);

        // When
        Optional<BookChapter> found = bookChapterRepository.findById(id);

        // Then
        assertFalse(found.isPresent(), "存在しない章は見つからないこと");
    }

    @Test
    void testFindById_BookIdAndIsDeletedFalse_アクティブな章のみ取得() {
        // When
        List<BookChapter> chapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse(testBook.getId());

        // Then
        assertEquals(3, chapters.size(), "削除済みを除く3つの章が取得されること");
        
        // 章番号順にソートされているか確認
        assertEquals(1L, chapters.get(0).getId().getChapterNumber());
        assertEquals(2L, chapters.get(1).getId().getChapterNumber());
        assertEquals(3L, chapters.get(2).getId().getChapterNumber());
        
        // タイトルの確認
        assertEquals("第1章：始まり", chapters.get(0).getTitle());
        assertEquals("第2章：展開", chapters.get(1).getTitle());
        assertEquals("第3章：結末", chapters.get(2).getTitle());
    }

    @Test
    void testFindById_BookIdAndIsDeletedFalse_存在しない書籍() {
        // When
        List<BookChapter> chapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse("non-existent-book");

        // Then
        assertTrue(chapters.isEmpty(), "存在しない書籍の章は空リスト");
    }

    @Test
    void testFindByIdInAndIsDeletedFalse_複数の章ID指定() {
        // Given
        BookChapterId id1 = new BookChapterId(testBook.getId(), 1L);
        BookChapterId id2 = new BookChapterId(testBook.getId(), 3L);
        List<BookChapterId> ids = Arrays.asList(id1, id2);

        // When
        List<BookChapter> chapters = bookChapterRepository.findByIdInAndIsDeletedFalse(ids);

        // Then
        assertEquals(2, chapters.size(), "指定した2つの章が取得されること");
        
        // 取得した章のチェック
        assertTrue(chapters.stream().anyMatch(c -> c.getTitle().equals("第1章：始まり")));
        assertTrue(chapters.stream().anyMatch(c -> c.getTitle().equals("第3章：結末")));
    }

    @Test
    void testFindByIdInAndIsDeletedFalse_削除済み章を含む() {
        // Given
        BookChapterId id1 = new BookChapterId(testBook.getId(), 1L);
        BookChapterId id4 = new BookChapterId(testBook.getId(), 4L); // 削除済み
        List<BookChapterId> ids = Arrays.asList(id1, id4);

        // When
        List<BookChapter> chapters = bookChapterRepository.findByIdInAndIsDeletedFalse(ids);

        // Then
        assertEquals(1, chapters.size(), "削除済み章は除外され、1つの章のみ取得されること");
        assertEquals("第1章：始まり", chapters.get(0).getTitle());
    }

    @Test
    void testFindByIdInAndIsDeletedFalse_空のIDリスト() {
        // When
        List<BookChapter> chapters = bookChapterRepository.findByIdInAndIsDeletedFalse(Arrays.asList());

        // Then
        assertTrue(chapters.isEmpty(), "空のIDリストでは空リストが返されること");
    }

    @Test
    void testSave_新規章作成() {
        // Given
        BookChapterId newId = new BookChapterId(testBook.getId(), 5L);
        BookChapter newChapter = new BookChapter();
        newChapter.setId(newId);
        newChapter.setTitle("第5章：新章");
        newChapter.setBook(testBook);
        newChapter.setIsDeleted(false);

        // When
        BookChapter saved = bookChapterRepository.save(newChapter);

        // Then
        assertEquals(newId, saved.getId());
        assertEquals("第5章：新章", saved.getTitle());

        // データベースから取得して検証
        Optional<BookChapter> found = bookChapterRepository.findById(newId);
        assertTrue(found.isPresent());
        assertEquals("第5章：新章", found.get().getTitle());
    }

    @Test
    void testSave_既存章更新() {
        // Given
        chapter1.setTitle("第1章：更新されたタイトル");

        // When
        BookChapter updated = bookChapterRepository.save(chapter1);

        // Then
        assertEquals(chapter1.getId(), updated.getId());
        assertEquals("第1章：更新されたタイトル", updated.getTitle());

        // データベースから取得して検証
        entityManager.clear();
        Optional<BookChapter> found = bookChapterRepository.findById(chapter1.getId());
        assertTrue(found.isPresent());
        assertEquals("第1章：更新されたタイトル", found.get().getTitle());
    }

    @Test
    void testDelete_論理削除() {
        // Given
        BookChapterId id = chapter1.getId();

        // When - 論理削除
        chapter1.setIsDeleted(true);
        bookChapterRepository.save(chapter1);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<BookChapter> found = bookChapterRepository.findById(id);
        assertTrue(found.isPresent(), "データは残っていること");
        assertTrue(found.get().getIsDeleted(), "論理削除フラグが立っていること");

        // findById_BookIdAndIsDeletedFalseでは除外されること
        List<BookChapter> activeChapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse(testBook.getId());
        assertEquals(2, activeChapters.size(), "論理削除された章は除外されること");
    }

    @Test
    void testCount_全章数() {
        // When
        long count = bookChapterRepository.count();

        // Then
        assertEquals(5, count, "全書籍の全章数（削除済み含む）");
    }

    @Test
    void testEntityBase_タイムスタンプ自動設定() {
        // Given
        BookChapterId newId = new BookChapterId(testBook.getId(), 6L);
        BookChapter newChapter = new BookChapter();
        newChapter.setId(newId);
        newChapter.setTitle("第6章：タイムスタンプテスト");
        newChapter.setBook(testBook);
        newChapter.setIsDeleted(false);

        // When
        BookChapter saved = bookChapterRepository.save(newChapter);
        entityManager.flush();

        // Then
        assertNotNull(saved.getCreatedAt(), "createdAtが自動設定されること");
        assertNotNull(saved.getUpdatedAt(), "updatedAtが自動設定されること");
    }

    @Test
    void testComplexId_複合主キーの動作確認() {
        // Given
        BookChapterId id1 = new BookChapterId("book-1", 1L);
        BookChapterId id2 = new BookChapterId("book-1", 1L);
        BookChapterId id3 = new BookChapterId("book-2", 1L);

        // Then
        assertEquals(id1, id2, "同じbookIdとchapterNumberなら等しいこと");
        assertNotEquals(id1, id3, "bookIdが異なれば等しくないこと");
        assertEquals(id1.hashCode(), id2.hashCode(), "等しいIDはhashCodeも等しいこと");
    }
}
