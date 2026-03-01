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
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_api.dto.user.UserProfileCountsResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookChapter;
import com.example.my_books_api.entity.BookChapterId;
import com.example.my_books_api.entity.BookChapterPageContent;
import com.example.my_books_api.entity.Bookmark;
import com.example.my_books_api.entity.Favorite;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.entity.Review;
import com.example.my_books_api.entity.User;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class UserRepositoryTest {

    @Container
    @SuppressWarnings("resource")
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

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE favorites;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE bookmarks;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE reviews;").executeUpdate();
        entityManager.getEntityManager()
            .createNativeQuery("TRUNCATE TABLE book_chapter_page_contents;")
            .executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_chapters;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE books;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE users;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

        testUser1 = new User();
        testUser1.setId("user-uuid-001");
        testUser1.setDisplayName("テストユーザー1");
        testUser1.setAvatarPath("/avatars/test1.jpg");
        testUser1.setIsDeleted(false);
        entityManager.persist(testUser1);

        testUser2 = new User();
        testUser2.setId("user-uuid-002");
        testUser2.setDisplayName("テストユーザー2");
        testUser2.setAvatarPath("/avatars/test2.jpg");
        testUser2.setIsDeleted(false);
        entityManager.persist(testUser2);

        deletedUser = new User();
        deletedUser.setId("user-uuid-deleted");
        deletedUser.setDisplayName("削除済みユーザー");
        deletedUser.setAvatarPath("/avatars/deleted.jpg");
        deletedUser.setIsDeleted(true);
        entityManager.persist(deletedUser);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
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
    @DisplayName("findByIsDeletedFalse - displayNameでソート（昇順）")
    void testFindByIsDeletedFalse_SortByDisplayNameAsc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("displayName").ascending());

        // When
        Page<User> result = userRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDisplayName()).isEqualTo("テストユーザー1");
        assertThat(result.getContent().get(1).getDisplayName()).isEqualTo("テストユーザー2");
    }

    @Test
    @DisplayName("save - 既存ユーザー更新")
    void testSave_UpdateUser() {
        // Given
        testUser1.setDisplayName("更新された表示名");
        testUser1.setAvatarPath("/avatars/updated.jpg");

        // When
        userRepository.save(testUser1);
        entityManager.flush();

        // Then
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(testUser1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getDisplayName()).isEqualTo("更新された表示名");
        assertThat(result.get().getAvatarPath()).isEqualTo("/avatars/updated.jpg");
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("論理削除されたユーザーを復活可能")
    void testResurrectDeletedUser() {
        // Given: 論理削除済みユーザー
        assertThat(deletedUser.getIsDeleted()).isTrue();

        // When: is_deletedをfalseに更新
        deletedUser.setIsDeleted(false);
        userRepository.save(deletedUser);
        entityManager.flush();

        // Then: 復活したユーザーが取得可能
        Optional<User> result = userRepository.findByIdAndIsDeletedFalse(deletedUser.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getIsDeleted()).isFalse();
        assertThat(result.get().getId()).isEqualTo(deletedUser.getId());
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - ユーザーの統計情報取得")
    void testGetUserProfileCountsResponse() {
        // Given: テストデータ作成
        Genre genre = new Genre();
        genre.setName("統計テスト用ジャンル");
        genre.setDescription("統計テスト用");
        entityManager.persist(genre);

        Book book1 = new Book();
        book1.setId("book-uuid-001");
        book1.setTitle("テスト書籍1");
        book1.setDescription("説明");
        book1.setGenres(List.of(genre));
        book1.setAuthors("著者");
        book1.setPublisher("出版社");
        book1.setPublicationDate(Date.valueOf("2024-01-01"));
        book1.setPrice(1000L);
        book1.setPageCount(100L);
        book1.setIsbn("978-0-0000-0000-0");
        book1.setImagePath("/img.jpg");
        book1.setReviewCount(0L);
        book1.setAverageRating(0.0);
        book1.setPopularity(0.0);
        book1.setIsDeleted(false);
        entityManager.persist(book1);

        Book book2 = new Book();
        book2.setId("book-uuid-002");
        book2.setTitle("テスト書籍2");
        book2.setDescription("説明2");
        book2.setGenres(List.of(genre));
        book2.setAuthors("著者2");
        book2.setPublisher("出版社2");
        book2.setPublicationDate(Date.valueOf("2024-01-02"));
        book2.setPrice(2000L);
        book2.setPageCount(200L);
        book2.setIsbn("978-0-0000-0000-1");
        book2.setImagePath("/img2.jpg");
        book2.setReviewCount(0L);
        book2.setAverageRating(0.0);
        book2.setPopularity(0.0);
        book2.setIsDeleted(false);
        entityManager.persist(book2);

        Book book3 = new Book();
        book3.setId("book-uuid-003");
        book3.setTitle("テスト書籍3");
        book3.setDescription("説明3");
        book3.setGenres(List.of(genre));
        book3.setAuthors("著者3");
        book3.setPublisher("出版社3");
        book3.setPublicationDate(Date.valueOf("2024-01-03"));
        book3.setPrice(3000L);
        book3.setPageCount(300L);
        book3.setIsbn("978-0-0000-0000-2");
        book3.setImagePath("/img3.jpg");
        book3.setReviewCount(0L);
        book3.setAverageRating(0.0);
        book3.setPopularity(0.0);
        book3.setIsDeleted(false);
        entityManager.persist(book3);

        // お気に入り2件
        Favorite fav1 = new Favorite();
        fav1.setUser(testUser1);
        fav1.setBook(book1);
        fav1.setIsDeleted(false);
        entityManager.persist(fav1);

        Favorite fav2 = new Favorite();
        fav2.setUser(testUser1);
        fav2.setBook(book2);
        fav2.setIsDeleted(false);
        entityManager.persist(fav2);

        // ブックマーク3件（異なるページコンテンツ）
        BookChapterId chapterId1 = new BookChapterId(book1.getId(), 1L);
        BookChapter chapter1 = new BookChapter();
        chapter1.setId(chapterId1);
        chapter1.setTitle("第1章");
        chapter1.setBook(book1);
        chapter1.setIsDeleted(false);
        entityManager.persist(chapter1);

        BookChapterPageContent page1 = new BookChapterPageContent();
        page1.setBookId(book1.getId());
        page1.setChapterNumber(1L);
        page1.setPageNumber(1L);
        page1.setContent("内容1");
        page1.setIsDeleted(false);
        entityManager.persist(page1);

        BookChapterPageContent page2 = new BookChapterPageContent();
        page2.setBookId(book1.getId());
        page2.setChapterNumber(1L);
        page2.setPageNumber(2L);
        page2.setContent("内容2");
        page2.setIsDeleted(false);
        entityManager.persist(page2);

        BookChapterPageContent page3 = new BookChapterPageContent();
        page3.setBookId(book1.getId());
        page3.setChapterNumber(1L);
        page3.setPageNumber(3L);
        page3.setContent("内容3");
        page3.setIsDeleted(false);
        entityManager.persist(page3);

        Bookmark bm1 = new Bookmark();
        bm1.setUser(testUser1);
        bm1.setPageContent(page1);
        bm1.setNote("note1");
        bm1.setIsDeleted(false);
        entityManager.persist(bm1);

        Bookmark bm2 = new Bookmark();
        bm2.setUser(testUser1);
        bm2.setPageContent(page2);
        bm2.setNote("note2");
        bm2.setIsDeleted(false);
        entityManager.persist(bm2);

        Bookmark bm3 = new Bookmark();
        bm3.setUser(testUser1);
        bm3.setPageContent(page3);
        bm3.setNote("note3");
        bm3.setIsDeleted(false);
        entityManager.persist(bm3);

        // レビュー1件
        Review rev1 = new Review();
        rev1.setUser(testUser1);
        rev1.setBook(book1);
        rev1.setRating(5.0);
        rev1.setComment("良い");
        rev1.setIsDeleted(false);
        entityManager.persist(rev1);

        entityManager.flush();

        // When
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse(testUser1.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteCount()).isEqualTo(2L);
        assertThat(response.getBookmarkCount()).isEqualTo(3L);
        assertThat(response.getReviewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - 統計情報0件の場合")
    void testGetUserProfileCountsResponse_NoData() {
        // When: testUser2はお気に入り・ブックマーク・レビューなし
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse(testUser2.getId());

        // Then: すべて0が返される
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteCount()).isEqualTo(0L);
        assertThat(response.getBookmarkCount()).isEqualTo(0L);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - 存在しないユーザーでも0を返す")
    void testGetUserProfileCountsResponse_NonExistentUser() {
        // When
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse("non-existent-uuid");

        // Then: 存在しないユーザーでも0を返す
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteCount()).isEqualTo(0L);
        assertThat(response.getBookmarkCount()).isEqualTo(0L);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getUserProfileCountsResponse - 論理削除済みデータは除外される")
    void testGetUserProfileCountsResponse_ExcludesDeletedData() {
        // Given: testUser1のアクティブデータ + testUser2の削除済みデータ
        Genre genre = new Genre();
        genre.setName("削除テスト用ジャンル");
        genre.setDescription("削除テスト用");
        entityManager.persist(genre);

        Book book1 = new Book();
        book1.setId("book-uuid-001");
        book1.setTitle("テスト書籍1");
        book1.setDescription("説明");
        book1.setGenres(List.of(genre));
        book1.setAuthors("著者");
        book1.setPublisher("出版社");
        book1.setPublicationDate(Date.valueOf("2024-01-01"));
        book1.setPrice(1000L);
        book1.setPageCount(100L);
        book1.setIsbn("978-0-0000-0000-0");
        book1.setImagePath("/img.jpg");
        book1.setReviewCount(0L);
        book1.setAverageRating(0.0);
        book1.setPopularity(0.0);
        book1.setIsDeleted(false);
        entityManager.persist(book1);

        // testUser1のアクティブお気に入り
        Favorite fav1 = new Favorite();
        fav1.setUser(testUser1);
        fav1.setBook(book1);
        fav1.setIsDeleted(false);
        entityManager.persist(fav1);

        // testUser2の削除済みお気に入り（カウントされないはず）
        Favorite fav2Deleted = new Favorite();
        fav2Deleted.setUser(testUser2);
        fav2Deleted.setBook(book1);
        fav2Deleted.setIsDeleted(true);
        entityManager.persist(fav2Deleted);

        entityManager.flush();

        // When
        UserProfileCountsResponse response = userRepository.getUserProfileCountsResponse(testUser1.getId());

        // Then: testUser1のアクティブデータのみカウント
        assertThat(response.getFavoriteCount()).isEqualTo(1L);
        assertThat(response.getBookmarkCount()).isEqualTo(0L);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - IDリストからユーザー取得")
    void testFindAllByIdInWithRelations() {
        // Given
        List<String> ids = List.of(testUser1.getId(), testUser2.getId());

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(User::getId)
            .containsExactlyInAnyOrder(testUser1.getId(), testUser2.getId());
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 空のIDリスト")
    void testFindAllByIdInWithRelations_EmptyList() {
        // Given
        List<String> ids = List.of();

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 論理削除済みも取得される（IDリストベース）")
    void testFindAllByIdInWithRelations_IncludesDeletedUsers() {
        // Given: deletedUserも含める
        List<String> ids = List.of(testUser1.getId(), deletedUser.getId());

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then: 論理削除済みも取得される（is_deletedフィルタなし）
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(User::getId)
            .containsExactlyInAnyOrder(testUser1.getId(), deletedUser.getId());
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 存在しないIDは無視される")
    void testFindAllByIdInWithRelations_NonExistentIds() {
        // Given
        List<String> ids = List.of(testUser1.getId(), "non-existent-uuid");

        // When
        List<User> result = userRepository.findAllByIdInWithRelations(ids);

        // Then: 存在するIDのみ返される
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testUser1.getId());
    }
}
