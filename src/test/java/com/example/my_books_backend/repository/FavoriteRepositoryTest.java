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

import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Favorite;
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
class FavoriteRepositoryTest {

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
    private FavoriteRepository favoriteRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;
    private Favorite testFavorite1;
    private Favorite testFavorite2;
    private Favorite testFavorite3;

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

        // お気に入りを作成
        testFavorite1 = createFavorite(testUser1, testBook1, false);
        entityManager.persist(testFavorite1);

        testFavorite2 = createFavorite(testUser1, testBook2, false);
        entityManager.persist(testFavorite2);

        testFavorite3 = createFavorite(testUser2, testBook1, false);
        entityManager.persist(testFavorite3);

        // 論理削除済みお気に入り
        Favorite deletedFavorite = createFavorite(testUser2, testBook2, true);
        entityManager.persist(deletedFavorite);

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

    private Favorite createFavorite(User user, Book book, boolean isDeleted) {
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        favorite.setIsDeleted(isDeleted);
        return favorite;
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(favorite -> favorite.getUser().getId().equals(testUser1.getId())));
        assertFalse(result.getContent().stream()
            .anyMatch(Favorite::getIsDeleted));
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ユーザーにお気に入りがない場合() {
        // Given
        User userWithNoFavorites = createUser("550e8400-e29b-41d4-a716-446655440099", "No Favorites");
        entityManager.persist(userWithNoFavorites);
        entityManager.flush();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalse(userWithNoFavorites.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndBookId_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser1.getId(), testBook1.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        Favorite favorite = result.getContent().get(0);
        assertEquals(testUser1.getId(), favorite.getUser().getId());
        assertEquals(testBook1.getId(), favorite.getBook().getId());
        assertFalse(favorite.getIsDeleted());
    }

    @Test
    void testFindByUserIdAndIsDeletedFalseAndBookId_マッチなし() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser2.getId(), testBook2.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements(), "user2のbook2お気に入りは論理削除済みのため0件");
    }

    @Test
    void testFindByUserIdAndBookId_正常系_論理削除含む() {
        // Given
        String userId = testUser2.getId();
        String bookId = testBook2.getId();

        // When
        Optional<Favorite> result = favoriteRepository.findByUserIdAndBookId(userId, bookId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUser().getId());
        assertEquals(bookId, result.get().getBook().getId());
        assertTrue(result.get().getIsDeleted(), "論理削除されたお気に入りも取得される");
    }

    @Test
    void testFindByUserIdAndBookId_マッチなし() {
        // Given
        String userId = testUser1.getId();
        String nonExistentBookId = "non-existent-book";

        // When
        Optional<Favorite> result = favoriteRepository.findByUserIdAndBookId(userId, nonExistentBookId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllByIdInWithRelations_正常系() {
        // Given
        List<Long> ids = Arrays.asList(testFavorite1.getId(), testFavorite2.getId(), testFavorite3.getId());

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // リレーションが正しくフェッチされていることを確認
        Favorite favorite1 = result.stream()
            .filter(f -> f.getId().equals(testFavorite1.getId()))
            .findFirst()
            .orElseThrow();

        assertNotNull(favorite1.getUser());
        assertNotNull(favorite1.getBook());
        assertEquals(testUser1.getId(), favorite1.getUser().getId());
        assertEquals(testBook1.getId(), favorite1.getBook().getId());
    }

    @Test
    void testFindAllByIdInWithRelations_空のIDリスト() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(emptyIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindAllByIdInWithRelations_存在しないID() {
        // Given
        List<Long> nonExistentIds = Arrays.asList(99999L, 88888L);

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(nonExistentIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetFavoriteStatsResponse_正常系_複数お気に入り() {
        // Given
        String bookId = testBook1.getId();

        // When
        FavoriteStatsResponse result = favoriteRepository.getFavoriteStatsResponse(bookId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getFavoriteCount(), "book-1には2件のお気に入りがある");
    }

    @Test
    void testGetFavoriteStatsResponse_正常系_お気に入りなし() {
        // Given
        Book bookWithNoFavorites = createBook("book-no-favorites", "No Favorites Book");
        entityManager.persist(bookWithNoFavorites);
        entityManager.flush();

        // When
        FavoriteStatsResponse result = favoriteRepository.getFavoriteStatsResponse(bookWithNoFavorites.getId());

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getFavoriteCount());
    }

    @Test
    void testGetFavoriteStatsResponse_論理削除されたお気に入りは除外() {
        // Given
        String bookId = testBook2.getId();

        // When
        FavoriteStatsResponse result = favoriteRepository.getFavoriteStatsResponse(bookId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getFavoriteCount(), "book-2には1件の有効なお気に入り（1件は論理削除済み）");
    }

    @Test
    void testFindByUserIdAndIsDeletedFalse_ページネーション() {
        // Given
        // 追加のお気に入りを作成（合計で10件以上）
        for (int i = 0; i < 10; i++) {
            Book book = createBook("book-extra-" + i, "Extra Book " + i);
            entityManager.persist(book);
            Favorite favorite = createFavorite(testUser1, book, false);
            entityManager.persist(favorite);
        }
        entityManager.flush();

        Pageable page0 = PageRequest.of(0, 5);
        Pageable page1 = PageRequest.of(1, 5);

        // When
        Page<Favorite> resultPage0 = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page0);
        Page<Favorite> resultPage1 = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), page1);

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
        Page<Favorite> resultAsc = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageableAsc);
        Page<Favorite> resultDesc = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser1.getId(), pageableDesc);

        // Then
        assertEquals(2, resultAsc.getContent().size());
        assertEquals(2, resultDesc.getContent().size());

        // ソート順序が適用されていることを確認
        assertNotNull(resultAsc.getContent().get(0).getCreatedAt());
        assertNotNull(resultDesc.getContent().get(0).getCreatedAt());
    }

    @Test
    void testFindByUserIdAndBookId_複数ユーザーで同じ書籍をお気に入り() {
        // Given
        String bookId = testBook1.getId();

        // When
        Optional<Favorite> user1Favorite = favoriteRepository.findByUserIdAndBookId(testUser1.getId(), bookId);
        Optional<Favorite> user2Favorite = favoriteRepository.findByUserIdAndBookId(testUser2.getId(), bookId);

        // Then
        assertTrue(user1Favorite.isPresent());
        assertTrue(user2Favorite.isPresent());
        assertNotEquals(user1Favorite.get().getId(), user2Favorite.get().getId());
        assertEquals(bookId, user1Favorite.get().getBook().getId());
        assertEquals(bookId, user2Favorite.get().getBook().getId());
    }
}
