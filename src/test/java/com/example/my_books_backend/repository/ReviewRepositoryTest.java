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

import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Review;
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
class ReviewRepositoryTest {

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
    private ReviewRepository reviewRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;
    private Review testReview1;
    private Review testReview2;
    private Review testReview3;

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

        // レビューを作成
        testReview1 = createReview(testUser1, testBook1, 5.0, "Excellent book!", false);
        entityManager.persist(testReview1);

        testReview2 = createReview(testUser1, testBook2, 4.0, "Good book!", false);
        entityManager.persist(testReview2);

        testReview3 = createReview(testUser2, testBook1, 3.5, "Average book", false);
        entityManager.persist(testReview3);

        // 論理削除済みレビュー
        Review deletedReview = createReview(testUser2, testBook2, 2.0, "Deleted review", true);
        entityManager.persist(deletedReview);

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

    private Review createReview(User user, Book book, Double rating, String comment, boolean isDeleted) {
        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        review.setIsDeleted(isDeleted);
        return review;
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(review -> review.getUser().getId().equals(testUser1.getId())));
        assertFalse(result.getContent().stream()
            .anyMatch(Review::getIsDeleted));
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ユーザーにレビューがない場合() {
        // Given
        User userWithNoReviews = createUser("550e8400-e29b-41d4-a716-446655440099", "No Reviews");
        entityManager.persist(userWithNoReviews);
        entityManager.flush();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalse(userWithNoReviews.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindByBookIdAndIsDeletedFalse_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByBookIdAndIsDeletedFalse(testBook1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(review -> review.getBook().getId().equals(testBook1.getId())));
        assertFalse(result.getContent().stream()
            .anyMatch(Review::getIsDeleted));
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndBookId_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser1.getId(), testBook1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        Review review = result.getContent().get(0);
        assertEquals(testUser1.getId(), review.getUser().getId());
        assertEquals(testBook1.getId(), review.getBook().getId());
        assertFalse(review.getIsDeleted());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndBookId_マッチなし() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser2.getId(), testBook2.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements(), "user2のbook2レビューは論理削除済みのため0件");
    }

    @Test
    void testFindByUserIdAndBookId_正常系_論理削除含む() {
        // Given
        String userId = testUser2.getId();
        String bookId = testBook2.getId();

        // When
        Optional<Review> result = reviewRepository.findByUserIdAndBookId(userId, bookId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUser().getId());
        assertEquals(bookId, result.get().getBook().getId());
        assertTrue(result.get().getIsDeleted(), "論理削除されたレビューも取得される");
    }

    @Test
    void testFindByUserIdAndBookId_マッチなし() {
        // Given
        String userId = testUser1.getId();
        String nonExistentBookId = "non-existent-book";

        // When
        Optional<Review> result = reviewRepository.findByUserIdAndBookId(userId, nonExistentBookId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllByIdInWithRelations_正常系() {
        // Given
        List<Long> ids = Arrays.asList(testReview1.getId(), testReview2.getId(), testReview3.getId());

        // When
        List<Review> result = reviewRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // リレーションが正しくフェッチされていることを確認
        Review review1 = result.stream()
            .filter(r -> r.getId().equals(testReview1.getId()))
            .findFirst()
            .orElseThrow();

        assertNotNull(review1.getUser());
        assertNotNull(review1.getBook());
        assertEquals(testUser1.getId(), review1.getUser().getId());
        assertEquals(testBook1.getId(), review1.getBook().getId());
    }

    @Test
    void testFindAllByIdInWithRelations_空のIDリスト() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        List<Review> result = reviewRepository.findAllByIdInWithRelations(emptyIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindAllByIdInWithRelations_存在しないID() {
        // Given
        List<Long> nonExistentIds = Arrays.asList(99999L, 88888L);

        // When
        List<Review> result = reviewRepository.findAllByIdInWithRelations(nonExistentIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetReviewStatsResponse_正常系_複数レビュー() {
        // Given
        String bookId = testBook1.getId();

        // When
        ReviewStatsResponse result = reviewRepository.getReviewStatsResponse(bookId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getReviewCount(), "book-1には2件のレビューがある");
        // 平均評価: (5.0 + 3.5) / 2 = 4.25
        assertEquals(4.25, result.getAverageRating(), 0.01);
    }

    @Test
    void testGetReviewStatsResponse_正常系_レビューなし() {
        // Given
        Book bookWithNoReviews = createBook("book-no-reviews", "No Reviews Book");
        entityManager.persist(bookWithNoReviews);
        entityManager.flush();

        // When
        ReviewStatsResponse result = reviewRepository.getReviewStatsResponse(bookWithNoReviews.getId());

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getReviewCount());
        assertEquals(0.0, result.getAverageRating());
    }

    @Test
    void testGetReviewStatsResponse_論理削除されたレビューは除外() {
        // Given
        String bookId = testBook2.getId();

        // When
        ReviewStatsResponse result = reviewRepository.getReviewStatsResponse(bookId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getReviewCount(), "book-2には1件の有効なレビュー（1件は論理削除済み）");
        assertEquals(4.0, result.getAverageRating(), 0.01);
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ページネーション() {
        // Given
        // 追加のレビューを作成（合計で10件以上）
        for (int i = 0; i < 10; i++) {
            Review review = createReview(testUser1, testBook1, 4.0, "Review " + i, false);
            entityManager.persist(review);
        }
        entityManager.flush();

        Pageable page0 = PageRequest.of(0, 5);
        Pageable page1 = PageRequest.of(1, 5);

        // When
        Page<Review> resultPage0 = reviewRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page0);
        Page<Review> resultPage1 = reviewRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page1);

        // Then
        assertEquals(5, resultPage0.getContent().size());
        assertEquals(5, resultPage1.getContent().size());
        assertTrue(resultPage0.getTotalElements() >= 10);
    }

    @Test
    void testFindByBookIdAndIsDeletedFalse_ソート順序() {
        // Given
        Pageable pageableAsc = PageRequest.of(0, 10,
            org.springframework.data.domain.Sort.by("rating").ascending());
        Pageable pageableDesc = PageRequest.of(0, 10,
            org.springframework.data.domain.Sort.by("rating").descending());

        // When
        Page<Review> resultAsc = reviewRepository.findByBookIdAndIsDeletedFalse(testBook1.getId(), pageableAsc);
        Page<Review> resultDesc = reviewRepository.findByBookIdAndIsDeletedFalse(testBook1.getId(), pageableDesc);

        // Then
        assertEquals(2, resultAsc.getContent().size());
        assertEquals(2, resultDesc.getContent().size());

        // 昇順: 3.5 → 5.0
        assertEquals(3.5, resultAsc.getContent().get(0).getRating());
        assertEquals(5.0, resultAsc.getContent().get(1).getRating());

        // 降順: 5.0 → 3.5
        assertEquals(5.0, resultDesc.getContent().get(0).getRating());
        assertEquals(3.5, resultDesc.getContent().get(1).getRating());
    }
}
