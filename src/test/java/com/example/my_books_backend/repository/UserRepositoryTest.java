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

import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class UserRepositoryTest {

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
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;

    @BeforeEach
    void setUp() {
        // ユーザーを作成
        testUser1 = createUser("550e8400-e29b-41d4-a716-446655440001", "User One", false);
        entityManager.persist(testUser1);

        testUser2 = createUser("550e8400-e29b-41d4-a716-446655440002", "User Two", false);
        entityManager.persist(testUser2);

        // 論理削除されたユーザー
        User deletedUser = createUser("550e8400-e29b-41d4-a716-446655440099", "Deleted User", true);
        entityManager.persist(deletedUser);

        // 書籍を作成
        testBook1 = createBook("book-1", "Test Book 1");
        entityManager.persist(testBook1);

        testBook2 = createBook("book-2", "Test Book 2");
        entityManager.persist(testBook2);

        entityManager.flush();
    }

    private User createUser(String id, String displayName, boolean isDeleted) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setAvatarPath("/avatars/default.png");
        user.setIsDeleted(isDeleted);
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

    private Review createReview(User user, Book book, Double rating, String comment, boolean isDeleted) {
        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        review.setIsDeleted(isDeleted);
        return review;
    }

    private Favorite createFavorite(User user, Book book, boolean isDeleted) {
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        favorite.setIsDeleted(isDeleted);
        return favorite;
    }

    private Bookmark createBookmark(User user, BookChapterPageContent pageContent, String note, boolean isDeleted) {
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setPageContent(pageContent);
        bookmark.setNote(note);
        bookmark.setIsDeleted(isDeleted);
        return bookmark;
    }

    private BookChapterPageContent createPageContent(String bookId, Long chapterNumber, Long pageNumber, String content) {
        BookChapterPageContent pageContent = new BookChapterPageContent();
        pageContent.setBookId(bookId);
        pageContent.setChapterNumber(chapterNumber);
        pageContent.setPageNumber(pageNumber);
        pageContent.setContent(content);
        return pageContent;
    }

    @Test
    void testFindByIdAndIsDeletedFalse_正常系() {
        // Given
        String userId = testUser1.getId();

        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
        assertEquals("User One", result.get().getDisplayName());
        assertFalse(result.get().getIsDeleted());
    }

    @Test
    void testFindByIdAndIsDeletedFalse_論理削除されたユーザーは取得されない() {
        // Given
        String deletedUserId = "550e8400-e29b-41d4-a716-446655440099";

        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(deletedUserId);

        // Then
        assertFalse(result.isPresent(), "論理削除されたユーザーは取得されない");
    }

    @Test
    void testFindByIdAndIsDeletedFalse_存在しないユーザー() {
        // Given
        String nonExistentUserId = "non-existent-user-id";

        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(nonExistentUserId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testGetUserProfileCountsResponse_正常系_すべて0件() {
        // Given
        String userId = testUser1.getId();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getFavoriteCount());
        assertEquals(0L, result.getBookmarkCount());
        assertEquals(0L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_正常系_レビューのみ() {
        // Given
        String userId = testUser1.getId();

        // レビューを作成
        Review review1 = createReview(testUser1, testBook1, 5.0, "Great book!", false);
        entityManager.persist(review1);

        Review review2 = createReview(testUser1, testBook2, 4.0, "Good book!", false);
        entityManager.persist(review2);

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getFavoriteCount());
        assertEquals(0L, result.getBookmarkCount());
        assertEquals(2L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_正常系_お気に入りのみ() {
        // Given
        String userId = testUser1.getId();

        // お気に入りを作成
        Favorite favorite1 = createFavorite(testUser1, testBook1, false);
        entityManager.persist(favorite1);

        Favorite favorite2 = createFavorite(testUser1, testBook2, false);
        entityManager.persist(favorite2);

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getFavoriteCount());
        assertEquals(0L, result.getBookmarkCount());
        assertEquals(0L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_正常系_ブックマークのみ() {
        // Given
        String userId = testUser1.getId();

        // ページコンテンツを作成
        BookChapterPageContent pageContent1 = createPageContent("book-1", 1L, 1L, "Content 1");
        entityManager.persist(pageContent1);

        BookChapterPageContent pageContent2 = createPageContent("book-1", 1L, 2L, "Content 2");
        entityManager.persist(pageContent2);

        // ブックマークを作成
        Bookmark bookmark1 = createBookmark(testUser1, pageContent1, "Note 1", false);
        entityManager.persist(bookmark1);

        Bookmark bookmark2 = createBookmark(testUser1, pageContent2, "Note 2", false);
        entityManager.persist(bookmark2);

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getFavoriteCount());
        assertEquals(2L, result.getBookmarkCount());
        assertEquals(0L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_正常系_すべての種類() {
        // Given
        String userId = testUser1.getId();

        // レビューを作成
        Review review = createReview(testUser1, testBook1, 5.0, "Great!", false);
        entityManager.persist(review);

        // お気に入りを作成
        Favorite favorite = createFavorite(testUser1, testBook1, false);
        entityManager.persist(favorite);

        // ページコンテンツとブックマークを作成
        BookChapterPageContent pageContent = createPageContent("book-1", 1L, 1L, "Content");
        entityManager.persist(pageContent);
        Bookmark bookmark = createBookmark(testUser1, pageContent, "Note", false);
        entityManager.persist(bookmark);

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getFavoriteCount());
        assertEquals(1L, result.getBookmarkCount());
        assertEquals(1L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_論理削除されたものは除外() {
        // Given
        String userId = testUser1.getId();

        // 有効なデータ
        Review activeReview = createReview(testUser1, testBook1, 5.0, "Active", false);
        entityManager.persist(activeReview);

        Favorite activeFavorite = createFavorite(testUser1, testBook1, false);
        entityManager.persist(activeFavorite);

        BookChapterPageContent pageContent = createPageContent("book-1", 1L, 1L, "Content");
        entityManager.persist(pageContent);
        Bookmark activeBookmark = createBookmark(testUser1, pageContent, "Active Note", false);
        entityManager.persist(activeBookmark);

        // 論理削除されたデータ
        Review deletedReview = createReview(testUser1, testBook2, 3.0, "Deleted", true);
        entityManager.persist(deletedReview);

        Favorite deletedFavorite = createFavorite(testUser1, testBook2, true);
        entityManager.persist(deletedFavorite);

        BookChapterPageContent pageContent2 = createPageContent("book-2", 1L, 1L, "Content 2");
        entityManager.persist(pageContent2);
        Bookmark deletedBookmark = createBookmark(testUser1, pageContent2, "Deleted Note", true);
        entityManager.persist(deletedBookmark);

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getFavoriteCount(), "論理削除されたお気に入りは除外");
        assertEquals(1L, result.getBookmarkCount(), "論理削除されたブックマークは除外");
        assertEquals(1L, result.getReviewCount(), "論理削除されたレビューは除外");
    }

    @Test
    void testGetUserProfileCountsResponse_存在しないユーザー() {
        // Given
        String nonExistentUserId = "non-existent-user-id";

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(nonExistentUserId);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getFavoriteCount());
        assertEquals(0L, result.getBookmarkCount());
        assertEquals(0L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_大量データ() {
        // Given
        String userId = testUser1.getId();

        // 大量のレビューを作成
        for (int i = 0; i < 50; i++) {
            Review review = createReview(testUser1, testBook1, 4.0, "Review " + i, false);
            entityManager.persist(review);
        }

        // 大量のお気に入りを作成
        for (int i = 0; i < 30; i++) {
            Book book = createBook("book-extra-" + i, "Extra Book " + i);
            entityManager.persist(book);
            Favorite favorite = createFavorite(testUser1, book, false);
            entityManager.persist(favorite);
        }

        // 大量のブックマークを作成
        for (int i = 0; i < 20; i++) {
            BookChapterPageContent pageContent = createPageContent("book-1", 1L, (long) i, "Content " + i);
            entityManager.persist(pageContent);
            Bookmark bookmark = createBookmark(testUser1, pageContent, "Note " + i, false);
            entityManager.persist(bookmark);
        }

        entityManager.flush();

        // When
        UserProfileCountsResponse result = userRepository.getUserProfileCountsResponse(userId);

        // Then
        assertNotNull(result);
        assertEquals(30L, result.getFavoriteCount());
        assertEquals(20L, result.getBookmarkCount());
        assertEquals(50L, result.getReviewCount());
    }

    @Test
    void testGetUserProfileCountsResponse_複数ユーザーの独立性() {
        // Given
        String userId1 = testUser1.getId();
        String userId2 = testUser2.getId();

        // user1のデータ
        Review review1 = createReview(testUser1, testBook1, 5.0, "User 1 Review", false);
        entityManager.persist(review1);

        Favorite favorite1 = createFavorite(testUser1, testBook1, false);
        entityManager.persist(favorite1);

        // user2のデータ
        Review review2 = createReview(testUser2, testBook1, 4.0, "User 2 Review", false);
        entityManager.persist(review2);

        Review review3 = createReview(testUser2, testBook2, 3.0, "User 2 Review 2", false);
        entityManager.persist(review3);

        entityManager.flush();

        // When
        UserProfileCountsResponse result1 = userRepository.getUserProfileCountsResponse(userId1);
        UserProfileCountsResponse result2 = userRepository.getUserProfileCountsResponse(userId2);

        // Then
        // User1
        assertEquals(1L, result1.getFavoriteCount());
        assertEquals(0L, result1.getBookmarkCount());
        assertEquals(1L, result1.getReviewCount());

        // User2
        assertEquals(0L, result2.getFavoriteCount());
        assertEquals(0L, result2.getBookmarkCount());
        assertEquals(2L, result2.getReviewCount());
    }
}
