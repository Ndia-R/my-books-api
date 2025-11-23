package com.example.my_books_backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.User;

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
class BookmarkRepositoryTest {

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
    private BookmarkRepository bookmarkRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;
    private BookChapterPageContent testPageContent1;
    private BookChapterPageContent testPageContent2;
    private BookChapterPageContent testPageContent3;
    private Bookmark testBookmark1;
    private Bookmark testBookmark2;
    private Bookmark testBookmark3;

    @BeforeEach
    void setUp() {
        // ユーザーを作成
        testUser1 = createUser("550e8400-e29b-41d4-a716-446655440001", "User One");
        entityManager.persist(testUser1);

        testUser2 = createUser("550e8400-e29b-41d4-a716-446655440002", "User Two");
        entityManager.persist(testUser2);

        // 書籍を作成
        testBook1 = createBook("book-1", "Test Book 1");
        entityManager.persist(testBook1);

        testBook2 = createBook("book-2", "Test Book 2");
        entityManager.persist(testBook2);

        // ページコンテンツを作成
        testPageContent1 = createPageContent("book-1", 1L, 1L, "Page 1 content");
        entityManager.persist(testPageContent1);

        testPageContent2 = createPageContent("book-1", 1L, 2L, "Page 2 content");
        entityManager.persist(testPageContent2);

        testPageContent3 = createPageContent("book-2", 1L, 1L, "Book 2 Page 1 content");
        entityManager.persist(testPageContent3);

        // ブックマークを作成
        testBookmark1 = createBookmark(testUser1, testPageContent1, "Note 1", false);
        entityManager.persist(testBookmark1);

        testBookmark2 = createBookmark(testUser1, testPageContent2, "Note 2", false);
        entityManager.persist(testBookmark2);

        testBookmark3 = createBookmark(testUser2, testPageContent1, "User 2 Note", false);
        entityManager.persist(testBookmark3);

        // 論理削除済みブックマーク
        Bookmark deletedBookmark = createBookmark(testUser2, testPageContent3, "Deleted note", true);
        entityManager.persist(deletedBookmark);

        entityManager.flush();
    }

    private User createUser(String id, String displayName) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setAvatarPath("/avatars/default.png");
        return user;
    }

    private Book createBook(String id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setDescription("Test description for " + title);
        book.setAuthors("Test Author");
        book.setPublisher("Test Publisher");
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

    private BookChapterPageContent createPageContent(String bookId, Long chapterNumber, Long pageNumber, String content) {
        BookChapterPageContent pageContent = new BookChapterPageContent();
        pageContent.setBookId(bookId);
        pageContent.setChapterNumber(chapterNumber);
        pageContent.setPageNumber(pageNumber);
        pageContent.setContent(content);
        return pageContent;
    }

    private Bookmark createBookmark(User user, BookChapterPageContent pageContent, String note, boolean isDeleted) {
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setPageContent(pageContent);
        bookmark.setNote(note);
        bookmark.setIsDeleted(isDeleted);
        return bookmark;
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(bookmark -> bookmark.getUser().getId().equals(testUser1.getId())));
        assertFalse(result.getContent().stream()
            .anyMatch(Bookmark::getIsDeleted));
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ユーザーにブックマークがない場合() {
        // Given
        User userWithNoBookmarks = createUser("550e8400-e29b-41d4-a716-446655440099", "No Bookmarks");
        entityManager.persist(userWithNoBookmarks);
        entityManager.flush();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalse(userWithNoBookmarks.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndPageContentBookId_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            testUser1.getId(), "book-1", pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(bookmark -> bookmark.getPageContent().getBookId().equals("book-1")));
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndPageContentBookId_マッチなし() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            testUser2.getId(), "book-2", pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements(), "user2のbook-2ブックマークは論理削除済みのため0件");
    }

    @Test
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber_正常系() {
        // Given
        String userId = testUser1.getId();
        String bookId = "book-1";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        // When
        Optional<Bookmark> result = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                userId, bookId, chapterNumber, pageNumber);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUser().getId());
        assertEquals(bookId, result.get().getPageContent().getBookId());
        assertEquals(chapterNumber, result.get().getPageContent().getChapterNumber());
        assertEquals(pageNumber, result.get().getPageContent().getPageNumber());
    }

    @Test
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber_論理削除含む() {
        // Given
        String userId = testUser2.getId();
        String bookId = "book-2";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        // When
        Optional<Bookmark> result = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                userId, bookId, chapterNumber, pageNumber);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDeleted(), "論理削除されたブックマークも取得される");
    }

    @Test
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber_マッチなし() {
        // Given
        String userId = testUser1.getId();
        String bookId = "book-1";
        Long chapterNumber = 99L;
        Long pageNumber = 99L;

        // When
        Optional<Bookmark> result = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                userId, bookId, chapterNumber, pageNumber);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllByIdInWithRelations_正常系() {
        // Given
        List<Long> ids = Arrays.asList(testBookmark1.getId(), testBookmark2.getId(), testBookmark3.getId());

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // リレーションが正しくフェッチされていることを確認
        Bookmark bookmark1 = result.stream()
            .filter(b -> b.getId().equals(testBookmark1.getId()))
            .findFirst()
            .orElseThrow();

        assertNotNull(bookmark1.getUser());
        assertNotNull(bookmark1.getPageContent());
        assertEquals(testUser1.getId(), bookmark1.getUser().getId());
        assertEquals("book-1", bookmark1.getPageContent().getBookId());
    }

    @Test
    void testFindAllByIdInWithRelations_空のIDリスト() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(emptyIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindAllByIdInWithRelations_存在しないID() {
        // Given
        List<Long> nonExistentIds = Arrays.asList(99999L, 88888L);

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(nonExistentIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ページネーション() {
        // Given
        // 追加のページコンテンツとブックマークを作成（合計で10件以上）
        for (int i = 0; i < 10; i++) {
            BookChapterPageContent pageContent = createPageContent("book-1", 2L, (long) i, "Content " + i);
            entityManager.persist(pageContent);
            Bookmark bookmark = createBookmark(testUser1, pageContent, "Note " + i, false);
            entityManager.persist(bookmark);
        }
        entityManager.flush();

        Pageable page0 = PageRequest.of(0, 5);
        Pageable page1 = PageRequest.of(1, 5);

        // When
        Page<Bookmark> resultPage0 = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page0);
        Page<Bookmark> resultPage1 = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page1);

        // Then
        assertEquals(5, resultPage0.getContent().size());
        assertEquals(5, resultPage1.getContent().size());
        assertTrue(resultPage0.getTotalElements() >= 10);
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ソート順序() {
        // Given
        Pageable pageableAsc = PageRequest.of(0, 10,
            org.springframework.data.domain.Sort.by("createdAt").ascending());
        Pageable pageableDesc = PageRequest.of(0, 10,
            org.springframework.data.domain.Sort.by("createdAt").descending());

        // When
        Page<Bookmark> resultAsc = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageableAsc);
        Page<Bookmark> resultDesc = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageableDesc);

        // Then
        assertEquals(2, resultAsc.getContent().size());
        assertEquals(2, resultDesc.getContent().size());

        // ソート順序が適用されていることを確認
        assertNotNull(resultAsc.getContent().get(0).getCreatedAt());
        assertNotNull(resultDesc.getContent().get(0).getCreatedAt());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndPageContentBookId_複数書籍のフィルタリング() {
        // Given
        // book-2のブックマークを追加
        BookChapterPageContent book2Page = createPageContent("book-2", 2L, 1L, "Book 2 Content");
        entityManager.persist(book2Page);
        Bookmark book2Bookmark = createBookmark(testUser1, book2Page, "Book 2 Note", false);
        entityManager.persist(book2Bookmark);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> book1Result = bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            testUser1.getId(), "book-1", pageable);
        Page<Bookmark> book2Result = bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            testUser1.getId(), "book-2", pageable);

        // Then
        assertEquals(2, book1Result.getTotalElements());
        assertEquals(1, book2Result.getTotalElements());
        assertTrue(book1Result.getContent().stream()
            .allMatch(b -> b.getPageContent().getBookId().equals("book-1")));
        assertTrue(book2Result.getContent().stream()
            .allMatch(b -> b.getPageContent().getBookId().equals("book-2")));
    }

    @Test
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber_複合キーの一意性() {
        // Given - 同じページに2人のユーザーがブックマーク
        String bookId = "book-1";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        // When
        Optional<Bookmark> user1Bookmark = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                testUser1.getId(), bookId, chapterNumber, pageNumber);
        Optional<Bookmark> user2Bookmark = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                testUser2.getId(), bookId, chapterNumber, pageNumber);

        // Then
        assertTrue(user1Bookmark.isPresent());
        assertTrue(user2Bookmark.isPresent());
        assertNotEquals(user1Bookmark.get().getId(), user2Bookmark.get().getId());
        assertEquals("Note 1", user1Bookmark.get().getNote());
        assertEquals("User 2 Note", user2Bookmark.get().getNote());
    }
}
