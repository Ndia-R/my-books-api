package com.example.my_books_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.entity.User;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;
    private User deletedUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        // テストユーザー1（アクティブ）
        testUser1 = new User();
        testUser1.setId("user-uuid-001");
        testUser1.setDisplayName("テストユーザー1");
        testUser1.setAvatarPath("/avatars/test1.jpg");
        testUser1.setIsDeleted(false);
        entityManager.persist(testUser1);

        // テストユーザー2（アクティブ）
        testUser2 = new User();
        testUser2.setId("user-uuid-002");
        testUser2.setDisplayName("テストユーザー2");
        testUser2.setAvatarPath("/avatars/test2.jpg");
        testUser2.setIsDeleted(false);
        entityManager.persist(testUser2);

        // 論理削除済みユーザー
        deletedUser = new User();
        deletedUser.setId("user-uuid-deleted");
        deletedUser.setDisplayName("削除済みユーザー");
        deletedUser.setAvatarPath("/avatars/deleted.jpg");
        deletedUser.setIsDeleted(true);
        entityManager.persist(deletedUser);

        // テスト用書籍
        testBook = new Book();
        testBook.setId("book-001");
        testBook.setTitle("テスト書籍");
        testBook.setDescription("テスト用の書籍です");
        testBook.setAuthors("テスト著者");
        testBook.setPublisher("テスト出版社");
        testBook.setIsbn("978-4-1234-5678-9");
        testBook.setImagePath("/images/book001.jpg");
        testBook.setIsDeleted(false);
        entityManager.persist(testBook);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在するアクティブユーザーを取得")
    void testFindByIdAndIsDeletedFalse_ActiveUser() {
        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(testUser1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-uuid-001");
        assertThat(result.get().getDisplayName()).isEqualTo("テストユーザー1");
        assertThat(result.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 論理削除済みユーザーは取得できない")
    void testFindByIdAndIsDeletedFalse_DeletedUser() {
        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(deletedUser.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在しないIDではEmptyを返す")
    void testFindByIdAndIsDeletedFalse_NonExistentUser() {
        // When
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse("non-existent-uuid");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIsDeletedFalse - アクティブユーザーのみ取得（ページネーション）")
    void testFindByIsDeletedFalse_Pagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2); // testUser1, testUser2のみ
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder("user-uuid-001", "user-uuid-002");
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - レビュー、お気に入り、ブックマーク数を取得")
    void testGetUserProfileCountsResponse() {
        // Given: testUser1にレビュー2件、お気に入り1件、ブックマーク3件を作成

        // レビュー作成（2件）
        Review review1 = new Review();
        review1.setUser(testUser1);
        review1.setBook(testBook);
        review1.setRating(5);
        review1.setComment("素晴らしい本です");
        review1.setIsDeleted(false);
        entityManager.persist(review1);

        Review review2 = new Review();
        review2.setUser(testUser1);
        review2.setBook(testBook);
        review2.setRating(4);
        review2.setComment("良い本です");
        review2.setIsDeleted(false);
        entityManager.persist(review2);

        // 論理削除済みレビュー（カウントされないはず）
        Review deletedReview = new Review();
        deletedReview.setUser(testUser1);
        deletedReview.setBook(testBook);
        deletedReview.setRating(3);
        deletedReview.setComment("削除済みレビュー");
        deletedReview.setIsDeleted(true);
        entityManager.persist(deletedReview);

        // お気に入り作成（1件）
        Favorite favorite = new Favorite();
        favorite.setUser(testUser1);
        favorite.setBook(testBook);
        favorite.setIsDeleted(false);
        entityManager.persist(favorite);

        // ブックマーク作成（3件）
        for (int i = 1; i <= 3; i++) {
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(testUser1);
            bookmark.setBook(testBook);
            bookmark.setChapterNumber((long) i);
            bookmark.setPageNumber((long) i);
            bookmark.setNote("ブックマーク" + i);
            bookmark.setIsDeleted(false);
            entityManager.persist(bookmark);
        }

        entityManager.flush();

        // When
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse(testUser1.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteCount()).isEqualTo(1);
        assertThat(response.getBookmarkCount()).isEqualTo(3);
        assertThat(response.getReviewCount()).isEqualTo(2); // 論理削除済みは除外
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - 関連データが0件の場合")
    void testGetUserProfileCountsResponse_NoRelatedData() {
        // When: testUser2は関連データなし
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse(testUser2.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteCount()).isEqualTo(0);
        assertThat(response.getBookmarkCount()).isEqualTo(0);
        assertThat(response.getReviewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - IDリストからユーザーを取得（2クエリ戦略）")
    void testFindAllByIdInWithRelations() {
        // Given
        List<String> ids = List.of(testUser1.getId(), testUser2.getId());

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(User::getId)
                .containsExactlyInAnyOrder("user-uuid-001", "user-uuid-002");
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 論理削除済みユーザーも取得される")
    void testFindAllByIdInWithRelations_IncludesDeletedUsers() {
        // Given: 論理削除済みユーザーのIDも含める
        List<String> ids = List.of(testUser1.getId(), deletedUser.getId());

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then: findAllByIdInWithRelationsは論理削除フィルタなし
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(User::getId)
                .containsExactlyInAnyOrder("user-uuid-001", "user-uuid-deleted");
    }

    @Test
    @DisplayName("論理削除されたユーザーを復活可能")
    void testResurrectDeletedUser() {
        // Given: 論理削除済みユーザー
        assertThat(deletedUser.getIsDeleted()).isTrue();

        // When: is_deletedをfalseに更新
        deletedUser.setIsDeleted(false);
        User resurrectedUser = userRepository.save(deletedUser);
        entityManager.flush();

        // Then: 復活したユーザーが取得可能
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(deletedUser.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getIsDeleted()).isFalse();
        assertThat(result.get().getId()).isEqualTo(deletedUser.getId());
    }

    @Test
    @DisplayName("save - 新規ユーザー作成")
    void testSave_NewUser() {
        // Given
        User newUser = new User();
        newUser.setId("user-uuid-new");
        newUser.setDisplayName("新規ユーザー");
        newUser.setAvatarPath("/avatars/new.jpg");
        newUser.setIsDeleted(false);

        // When
        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        // Then
        assertThat(savedUser.getId()).isEqualTo("user-uuid-new");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        // DB確認
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse("user-uuid-new");
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("save - 既存ユーザー更新")
    void testSave_UpdateUser() {
        // Given
        testUser1.setDisplayName("更新された表示名");
        testUser1.setAvatarPath("/avatars/updated.jpg");

        // When
        User updatedUser = userRepository.save(testUser1);
        entityManager.flush();

        // Then
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(testUser1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getDisplayName()).isEqualTo("更新された表示名");
        assertThat(result.get().getAvatarPath()).isEqualTo("/avatars/updated.jpg");
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }
}
