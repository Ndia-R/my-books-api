package com.example.my_books_backend.repository;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Genre;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class ReviewRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Book testBook;
    private Review review1;
    private Review review2;
    private Review deletedReview;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE reviews;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE books;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE users;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

        // ユーザーの作成
        testUser = new User();
        testUser.setId("user-uuid-001");
        testUser.setDisplayName("テストユーザー");
        testUser.setAvatarPath("/avatars/user.jpg");
        testUser.setIsDeleted(false);
        entityManager.persist(testUser);

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

        // レビュー1
        review1 = new Review();
        review1.setUser(testUser);
        review1.setBook(testBook);
        review1.setComment("とても面白い本でした");
        review1.setRating(5.0);
        review1.setIsDeleted(false);
        entityManager.persist(review1);

        // レビュー2
        review2 = new Review();
        review2.setUser(testUser);
        review2.setBook(testBook);
        review2.setComment("まあまあです");
        review2.setRating(3.0);
        review2.setIsDeleted(false);
        entityManager.persist(review2);

        // 削除済みレビュー
        deletedReview = new Review();
        deletedReview.setUser(testUser);
        deletedReview.setBook(testBook);
        deletedReview.setComment("削除されたレビュー");
        deletedReview.setRating(1.0);
        deletedReview.setIsDeleted(true);
        entityManager.persist(deletedReview);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(reviewRepository).isNotNull();
    }

    @Test
    @DisplayName("save - 新規レビュー作成")
    void testSave_NewReview() {
        // Given
        Review newReview = new Review();
        newReview.setUser(testUser);
        newReview.setBook(testBook);
        newReview.setComment("新しいレビュー");
        newReview.setRating(4.5);
        newReview.setIsDeleted(false);

        // When
        Review savedReview = reviewRepository.save(newReview);
        entityManager.flush();

        // Then
        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getComment()).isEqualTo("新しいレビュー");
        assertThat(savedReview.getRating()).isEqualTo(4.5);
        assertThat(savedReview.getCreatedAt()).isNotNull();
        assertThat(savedReview.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save - レビュー更新")
    void testSave_UpdateReview() {
        // Given
        review1.setComment("更新されたコメント");
        review1.setRating(4.0);

        // When
        reviewRepository.save(review1);
        entityManager.flush();

        // Then
        Optional<Review> result = reviewRepository.findByIdAndIsDeletedFalse(review1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getComment()).isEqualTo("更新されたコメント");
        assertThat(result.get().getRating()).isEqualTo(4.0);
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在するアクティブレビューを取得")
    void testFindByIdAndIsDeletedFalse_ActiveReview() {
        // When
        Optional<Review> result = reviewRepository.findByIdAndIsDeletedFalse(review1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getComment()).isEqualTo("とても面白い本でした");
        assertThat(result.get().getRating()).isEqualTo(5.0);
        assertThat(result.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 論理削除済みレビューは取得できない")
    void testFindByIdAndIsDeletedFalse_DeletedReview() {
        // When
        Optional<Review> result = reviewRepository.findByIdAndIsDeletedFalse(deletedReview.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalse - ユーザーのレビュー一覧取得")
    void testFindByUserIdAndIsDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

        // Then: review1, review2のみ（deletedReviewは除外）
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Review::getId)
                .containsExactlyInAnyOrder(review1.getId(), review2.getId());
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalseAndBookId - ユーザー×書籍のレビュー取得")
    void testFindByUserIdAndIsDeletedFalseAndBookId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByUserIdAndIsDeletedFalseAndBookId(
                testUser.getId(), testBook.getId(), pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByUserIdAndBookId - アクティブレビュー取得（重複チェック用）")
    void testFindByUserIdAndBookId_ActiveReview() {
        // When
        Optional<Review> result = reviewRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId());

        // Then: 最初に見つかったレビューを取得（論理削除も含む）
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByUserIdAndBookId - 論理削除済みレビューも取得")
    void testFindByUserIdAndBookId_DeletedReview() {
        // Given: アクティブなレビューを全て削除
        review1.setIsDeleted(true);
        review2.setIsDeleted(true);
        reviewRepository.save(review1);
        reviewRepository.save(review2);
        entityManager.flush();

        // When: 削除済みのみの場合
        Optional<Review> result = reviewRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId());

        // Then: 削除済みレビューも取得される
        assertThat(result).isPresent();
        assertThat(result.get().getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("findByBookIdAndIsDeletedFalse - 書籍のレビュー一覧取得")
    void testFindByBookIdAndIsDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByBookIdAndIsDeletedFalse(testBook.getId(), pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 関連エンティティFETCH")
    void testFindAllByIdInWithRelations_WithUserAndBook() {
        // Given
        List<Long> ids = List.of(review1.getId(), review2.getId());

        // When
        List<Review> result = reviewRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Review::getId)
                .containsExactlyInAnyOrder(review1.getId(), review2.getId());

        // 関連エンティティがFETCHされていることを確認
        Review fetchedReview = result.get(0);
        assertThat(fetchedReview.getUser()).isNotNull();
        assertThat(fetchedReview.getBook()).isNotNull();
        assertThat(fetchedReview.getBook().getGenres()).isNotEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 空のIDリスト")
    void testFindAllByIdInWithRelations_EmptyList() {
        // Given
        List<Long> ids = List.of();

        // When
        List<Review> result = reviewRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getReviewStatsResponse - 複数レビューの統計")
    void testGetReviewStatsResponse_WithMultipleReviews() {
        // When
        ReviewStatsResponse stats = reviewRepository.getReviewStatsResponse(testBook.getId());

        // Then
        assertThat(stats.getBookId()).isEqualTo(testBook.getId());
        assertThat(stats.getReviewCount()).isEqualTo(2L); // review1, review2のみ
        assertThat(stats.getAverageRating()).isEqualTo(4.0); // (5.0 + 3.0) / 2 = 4.0
    }

    @Test
    @DisplayName("getReviewStatsResponse - レビュー0件の場合（COALESCE検証）")
    void testGetReviewStatsResponse_NoReviews() {
        // Given: 新しい書籍（レビューなし）
        Book newBook = new Book();
        newBook.setId("book-uuid-new");
        newBook.setTitle("レビューなし書籍");
        newBook.setDescription("レビューがない書籍");
        newBook.setGenres(List.of());
        newBook.setAuthors("著者");
        newBook.setPublisher("出版社");
        newBook.setPublicationDate(Date.valueOf("2024-01-01"));
        newBook.setPrice(1000L);
        newBook.setPageCount(200L);
        newBook.setIsbn("978-4-0000-0000-0");
        newBook.setReviewCount(0L);
        newBook.setAverageRating(0.0);
        newBook.setPopularity(0.0);
        newBook.setIsDeleted(false);
        entityManager.persist(newBook);
        entityManager.flush();

        // When
        ReviewStatsResponse stats = reviewRepository.getReviewStatsResponse(newBook.getId());

        // Then: COALESCEでデフォルト値が返される
        assertThat(stats.getBookId()).isEqualTo(newBook.getId());
        assertThat(stats.getReviewCount()).isEqualTo(0L);
        assertThat(stats.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getReviewStatsResponse - 論理削除済みのみ")
    void testGetReviewStatsResponse_OnlyDeletedReviews() {
        // Given: 全レビューを削除
        review1.setIsDeleted(true);
        review2.setIsDeleted(true);
        reviewRepository.save(review1);
        reviewRepository.save(review2);
        entityManager.flush();

        // When
        ReviewStatsResponse stats = reviewRepository.getReviewStatsResponse(testBook.getId());

        // Then: 削除済みは除外されるので0件
        assertThat(stats.getBookId()).isEqualTo(testBook.getId());
        assertThat(stats.getReviewCount()).isEqualTo(0L);
        assertThat(stats.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 書籍削除時の一括ソフト削除成功")
    void testSoftDeleteAllByBookId_Success() {
        // When: 書籍IDでレビューを一括ソフト削除
        reviewRepository.softDeleteAllByBookId(testBook.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 全てのレビューがソフト削除される
        Page<Review> activeReviews = reviewRepository.findByBookIdAndIsDeletedFalse(
                testBook.getId(), PageRequest.of(0, 10));
        assertThat(activeReviews.getTotalElements()).isEqualTo(0);

        // DB確認: データは物理的に残っている
        List<Review> allReviews = entityManager.getEntityManager()
                .createQuery("SELECT r FROM Review r WHERE r.book.id = :bookId", Review.class)
                .setParameter("bookId", testBook.getId())
                .getResultList();
        assertThat(allReviews).hasSize(3); // 全てのデータが残っている
        assertThat(allReviews).allMatch(Review::getIsDeleted); // 全てisDeleted=true
    }

    @Test
    @DisplayName("softDeleteAllByBookId - レビューなし")
    void testSoftDeleteAllByBookId_NoReviews() {
        // When: レビューのない書籍IDで一括削除
        reviewRepository.softDeleteAllByBookId("non-existent-book-id");
        entityManager.flush();

        // Then: エラーなく終了
        Page<Review> existingReviews = reviewRepository.findByBookIdAndIsDeletedFalse(
                testBook.getId(), PageRequest.of(0, 10));
        assertThat(existingReviews.getTotalElements()).isEqualTo(2); // 既存レビューは影響なし
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 既に削除済み")
    void testSoftDeleteAllByBookId_AlreadyDeleted() {
        // Given: 全レビューを先にソフト削除
        reviewRepository.softDeleteAllByBookId(testBook.getId());
        entityManager.flush();
        entityManager.clear();

        // When: 再度ソフト削除
        reviewRepository.softDeleteAllByBookId(testBook.getId());
        entityManager.flush();

        // Then: エラーなく終了
        Page<Review> activeReviews = reviewRepository.findByBookIdAndIsDeletedFalse(
                testBook.getId(), PageRequest.of(0, 10));
        assertThat(activeReviews.getTotalElements()).isEqualTo(0);
    }
}
