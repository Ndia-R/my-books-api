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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_api.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.Favorite;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.entity.User;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class FavoriteRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private User testUser2;
    private Book testBook;
    private Book testBook2;
    private Favorite favorite1;
    private Favorite favorite2;
    private Favorite deletedFavorite;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE favorites;").executeUpdate();
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

        testUser2 = new User();
        testUser2.setId("user-uuid-002");
        testUser2.setDisplayName("テストユーザー2");
        testUser2.setAvatarPath("/avatars/user2.jpg");
        testUser2.setIsDeleted(false);
        entityManager.persist(testUser2);

        // ジャンルの作成
        Genre genre = new Genre();
        genre.setName("テストジャンル");
        genre.setDescription("テスト用のジャンル");
        entityManager.persist(genre);

        // 書籍1の作成
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

        // 書籍2の作成
        testBook2 = new Book();
        testBook2.setId("book-uuid-002");
        testBook2.setTitle("テスト書籍2");
        testBook2.setDescription("テスト用の書籍2");
        testBook2.setGenres(List.of(genre));
        testBook2.setAuthors("テスト著者2");
        testBook2.setPublisher("テスト出版社2");
        testBook2.setPublicationDate(Date.valueOf("2024-02-01"));
        testBook2.setPrice(1500L);
        testBook2.setPageCount(350L);
        testBook2.setIsbn("978-4-1234-5679-6");
        testBook2.setImagePath("/images/test2.jpg");
        testBook2.setReviewCount(0L);
        testBook2.setAverageRating(0.0);
        testBook2.setPopularity(0.0);
        testBook2.setIsDeleted(false);
        entityManager.persist(testBook2);

        // お気に入り1: testUser × testBook
        favorite1 = new Favorite();
        favorite1.setUser(testUser);
        favorite1.setBook(testBook);
        favorite1.setIsDeleted(false);
        entityManager.persist(favorite1);

        // お気に入り2: testUser × testBook2
        favorite2 = new Favorite();
        favorite2.setUser(testUser);
        favorite2.setBook(testBook2);
        favorite2.setIsDeleted(false);
        entityManager.persist(favorite2);

        // 削除済みお気に入り: testUser2 × testBook
        deletedFavorite = new Favorite();
        deletedFavorite.setUser(testUser2);
        deletedFavorite.setBook(testBook);
        deletedFavorite.setIsDeleted(true);
        entityManager.persist(deletedFavorite);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(favoriteRepository).isNotNull();
    }

    @Test
    @DisplayName("save - 新規お気に入り作成")
    void testSave_NewFavorite() {
        // Given
        User newUser = new User();
        newUser.setId("user-uuid-003");
        newUser.setDisplayName("新規ユーザー");
        newUser.setAvatarPath("/avatars/new.jpg");
        newUser.setIsDeleted(false);
        entityManager.persist(newUser);

        Favorite newFavorite = new Favorite();
        newFavorite.setUser(newUser);
        newFavorite.setBook(testBook);
        newFavorite.setIsDeleted(false);

        // When
        Favorite savedFavorite = favoriteRepository.save(newFavorite);
        entityManager.flush();

        // Then
        assertThat(savedFavorite.getId()).isNotNull();
        assertThat(savedFavorite.getUser().getId()).isEqualTo("user-uuid-003");
        assertThat(savedFavorite.getBook().getId()).isEqualTo(testBook.getId());
        assertThat(savedFavorite.getCreatedAt()).isNotNull();
        assertThat(savedFavorite.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalse - ユーザーのお気に入り一覧取得")
    void testFindByUserIdAndIsDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

        // Then: favorite1, favorite2のみ（deletedFavoriteはtestUser2なので除外）
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(Favorite::getId)
            .containsExactlyInAnyOrder(favorite1.getId(), favorite2.getId());
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalse - 別ユーザーのお気に入りは含まれない")
    void testFindByUserIdAndIsDeletedFalse_DifferentUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalse(testUser2.getId(), pageable);

        // Then: testUser2のアクティブなお気に入りは0件
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalseAndBookId - ユーザー×書籍のお気に入り一覧")
    void testFindByUserIdAndIsDeletedFalseAndBookId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Favorite> result = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser.getId(),
            testBook.getId(),
            pageable
        );

        // Then: favorite1のみ
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(favorite1.getId());
    }

    @Test
    @DisplayName("findByUserIdAndBookId - アクティブお気に入り取得（重複チェック用）")
    void testFindByUserIdAndBookId_ActiveFavorite() {
        // When
        Optional<Favorite> result = favoriteRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(favorite1.getId());
    }

    @Test
    @DisplayName("findByUserIdAndBookId - 削除済みお気に入りも取得される（重複チェック用）")
    void testFindByUserIdAndBookId_DeletedFavorite() {
        // When
        Optional<Favorite> result = favoriteRepository.findByUserIdAndBookId(testUser2.getId(), testBook.getId());

        // Then: is_deletedに関係なく取得される
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(deletedFavorite.getId());
        assertThat(result.get().getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("findByUserIdAndBookId - 存在しない場合")
    void testFindByUserIdAndBookId_NotFound() {
        // When
        Optional<Favorite> result = favoriteRepository.findByUserIdAndBookId("non-existent-user", testBook.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - IDリストからJOIN FETCHで取得")
    void testFindAllByIdInWithRelations() {
        // Given
        List<Long> ids = List.of(favorite1.getId(), favorite2.getId());

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Favorite::getId)
            .containsExactlyInAnyOrder(favorite1.getId(), favorite2.getId());

        // 関連エンティティがFETCHされていることを確認
        Favorite fetchedFavorite = result.stream()
            .filter(f -> f.getId().equals(favorite1.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(fetchedFavorite.getUser()).isNotNull();
        assertThat(fetchedFavorite.getBook()).isNotNull();
        assertThat(fetchedFavorite.getBook().getGenres()).isNotEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 空のIDリスト")
    void testFindAllByIdInWithRelations_EmptyList() {
        // Given
        List<Long> ids = List.of();

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getFavoriteStatsResponse - お気に入り統計取得")
    void testGetFavoriteStatsResponse() {
        // When
        FavoriteStatsResponse result = favoriteRepository.getFavoriteStatsResponse(testBook.getId());

        // Then: favorite1とdeletedFavoriteの2件（deletedFavoriteは論理削除されているので除外され、favorite1のみカウント）
        assertThat(result).isNotNull();
        assertThat(result.getBookId()).isEqualTo(testBook.getId());
        assertThat(result.getFavoriteCount()).isEqualTo(1L); // favorite1のみ（deletedFavoriteは除外）
    }

    @Test
    @DisplayName("getFavoriteStatsResponse - お気に入り0件の場合（COALESCE検証）")
    void testGetFavoriteStatsResponse_NoFavorites() {
        // Given: 新しい書籍（お気に入りなし）
        Book newBook = new Book();
        newBook.setId("book-uuid-999");
        newBook.setTitle("お気に入りなし書籍");
        newBook.setDescription("テスト");
        newBook.setGenres(List.of());
        newBook.setAuthors("著者");
        newBook.setPublisher("出版社");
        newBook.setPublicationDate(Date.valueOf("2024-01-01"));
        newBook.setPrice(1000L);
        newBook.setPageCount(100L);
        newBook.setIsbn("978-0-0000-0000-0");
        newBook.setImagePath("/images/none.jpg");
        newBook.setReviewCount(0L);
        newBook.setAverageRating(0.0);
        newBook.setPopularity(0.0);
        newBook.setIsDeleted(false);
        entityManager.persist(newBook);
        entityManager.flush();

        // When
        FavoriteStatsResponse result = favoriteRepository.getFavoriteStatsResponse(newBook.getId());

        // Then: COALESCEにより0が返される
        assertThat(result).isNotNull();
        assertThat(result.getBookId()).isEqualTo(newBook.getId());
        assertThat(result.getFavoriteCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 書籍のお気に入りを一括論理削除")
    void testSoftDeleteAllByBookId() {
        // Given: favorite1がアクティブ
        Optional<Favorite> beforeDelete = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser.getId(),
            testBook.getId(),
            PageRequest.of(0, 10)
        ).stream().findFirst();
        assertThat(beforeDelete).isPresent();

        // When
        favoriteRepository.softDeleteAllByBookId(testBook.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 論理削除される
        Page<Favorite> afterDelete = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser.getId(),
            testBook.getId(),
            PageRequest.of(0, 10)
        );
        assertThat(afterDelete.getContent()).isEmpty();

        // DBから直接確認（is_deleted = true になっている）
        Favorite deletedFavorite1 = entityManager.find(Favorite.class, favorite1.getId());
        assertThat(deletedFavorite1.getIsDeleted()).isTrue();

        // 他の書籍のお気に入りは影響を受けない
        Page<Favorite> otherBookFavorites = favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            testUser.getId(),
            testBook2.getId(),
            PageRequest.of(0, 10)
        );
        assertThat(otherBookFavorites.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - JOIN FETCHでLazyLoad回避を実証")
    void testFindAllByIdInWithRelations_AvoidLazyLoadException() {
        // Given
        List<Long> ids = List.of(favorite1.getId(), favorite2.getId());

        // When
        List<Favorite> result = favoriteRepository.findAllByIdInWithRelations(ids);
        entityManager.clear(); // セッションをクリア

        // Then: JOIN FETCHにより、セッション外でも関連エンティティにアクセス可能
        assertThat(result).hasSize(2);

        Favorite fetchedFavorite = result.get(0);
        assertThat(fetchedFavorite.getUser()).isNotNull();
        assertThat(fetchedFavorite.getUser().getDisplayName()).isNotNull(); // LazyLoadExceptionなし
        assertThat(fetchedFavorite.getBook()).isNotNull();
        assertThat(fetchedFavorite.getBook().getTitle()).isNotNull(); // LazyLoadExceptionなし
        assertThat(fetchedFavorite.getBook().getGenres()).isNotEmpty(); // LazyLoadExceptionなし
    }
}
