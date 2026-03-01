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

import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookChapter;
import com.example.my_books_api.entity.BookChapterId;
import com.example.my_books_api.entity.BookChapterPageContent;
import com.example.my_books_api.entity.Bookmark;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.entity.User;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookmarkRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private User testUser2;
    private Book testBook;
    private BookChapterPageContent pageContent1;
    private BookChapterPageContent pageContent2;
    private Bookmark bookmark1;
    private Bookmark bookmark2;
    private Bookmark deletedBookmark;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE bookmarks;").executeUpdate();
        entityManager.getEntityManager()
            .createNativeQuery("TRUNCATE TABLE book_chapter_page_contents;")
            .executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_chapters;").executeUpdate();
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

        // 章の作成
        BookChapterId chapterId1 = new BookChapterId(testBook.getId(), 1L);
        BookChapter chapter1 = new BookChapter();
        chapter1.setId(chapterId1);
        chapter1.setTitle("第1章");
        chapter1.setBook(testBook);
        chapter1.setIsDeleted(false);
        entityManager.persist(chapter1);

        BookChapterId chapterId2 = new BookChapterId(testBook.getId(), 2L);
        BookChapter chapter2 = new BookChapter();
        chapter2.setId(chapterId2);
        chapter2.setTitle("第2章");
        chapter2.setBook(testBook);
        chapter2.setIsDeleted(false);
        entityManager.persist(chapter2);

        // ページコンテンツの作成
        pageContent1 = new BookChapterPageContent();
        pageContent1.setBookId(testBook.getId());
        pageContent1.setChapterNumber(1L);
        pageContent1.setPageNumber(1L);
        pageContent1.setContent("第1章第1ページの内容");
        pageContent1.setIsDeleted(false);
        entityManager.persist(pageContent1);

        pageContent2 = new BookChapterPageContent();
        pageContent2.setBookId(testBook.getId());
        pageContent2.setChapterNumber(1L);
        pageContent2.setPageNumber(2L);
        pageContent2.setContent("第1章第2ページの内容");
        pageContent2.setIsDeleted(false);
        entityManager.persist(pageContent2);

        // ブックマーク1
        bookmark1 = new Bookmark();
        bookmark1.setUser(testUser);
        bookmark1.setPageContent(pageContent1);
        bookmark1.setNote("重要な箇所");
        bookmark1.setIsDeleted(false);
        entityManager.persist(bookmark1);

        // ブックマーク2
        bookmark2 = new Bookmark();
        bookmark2.setUser(testUser);
        bookmark2.setPageContent(pageContent2);
        bookmark2.setNote("後で読み返す");
        bookmark2.setIsDeleted(false);
        entityManager.persist(bookmark2);

        // 削除済みブックマーク
        deletedBookmark = new Bookmark();
        deletedBookmark.setUser(testUser2);
        deletedBookmark.setPageContent(pageContent1);
        deletedBookmark.setNote("削除されたブックマーク");
        deletedBookmark.setIsDeleted(true);
        entityManager.persist(deletedBookmark);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(bookmarkRepository).isNotNull();
    }

    @Test
    @DisplayName("save - 新規ブックマーク作成")
    void testSave_NewBookmark() {
        // Given
        BookChapterPageContent newPageContent = new BookChapterPageContent();
        newPageContent.setBookId(testBook.getId());
        newPageContent.setChapterNumber(2L);
        newPageContent.setPageNumber(1L);
        newPageContent.setContent("第2章第1ページの内容");
        newPageContent.setIsDeleted(false);
        entityManager.persist(newPageContent);
        entityManager.flush();

        Bookmark newBookmark = new Bookmark();
        newBookmark.setUser(testUser);
        newBookmark.setPageContent(newPageContent);
        newBookmark.setNote("新しいブックマーク");
        newBookmark.setIsDeleted(false);

        // When
        Bookmark savedBookmark = bookmarkRepository.save(newBookmark);
        entityManager.flush();

        // Then
        assertThat(savedBookmark.getId()).isNotNull();
        assertThat(savedBookmark.getNote()).isEqualTo("新しいブックマーク");
        assertThat(savedBookmark.getCreatedAt()).isNotNull();
        assertThat(savedBookmark.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save - ブックマーク更新")
    void testSave_UpdateBookmark() {
        // Given
        bookmark1.setNote("更新されたメモ");

        // When
        bookmarkRepository.save(bookmark1);
        entityManager.flush();

        // Then
        Optional<Bookmark> result = bookmarkRepository.findByIdAndIsDeletedFalse(bookmark1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getNote()).isEqualTo("更新されたメモ");
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在するアクティブブックマークを取得")
    void testFindByIdAndIsDeletedFalse_ActiveBookmark() {
        // When
        Optional<Bookmark> result = bookmarkRepository.findByIdAndIsDeletedFalse(bookmark1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(bookmark1.getId());
        assertThat(result.get().getNote()).isEqualTo("重要な箇所");
        assertThat(result.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 論理削除済みブックマークは取得できない")
    void testFindByIdAndIsDeletedFalse_DeletedBookmark() {
        // When
        Optional<Bookmark> result = bookmarkRepository.findByIdAndIsDeletedFalse(deletedBookmark.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在しないIDではEmptyを返す")
    void testFindByIdAndIsDeletedFalse_NonExistentBookmark() {
        // When
        Optional<Bookmark> result = bookmarkRepository.findByIdAndIsDeletedFalse(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalse - ユーザーのブックマーク一覧取得")
    void testFindByUserIdAndIsDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

        // Then: bookmark1, bookmark2のみ（deletedBookmarkはtestUser2なので除外）
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(Bookmark::getId)
            .containsExactlyInAnyOrder(bookmark1.getId(), bookmark2.getId());
    }

    @Test
    @DisplayName("findByUserIdAndIsDeletedFalseAndPageContentBookId - ユーザー×書籍のブックマーク一覧")
    void testFindByUserIdAndIsDeletedFalseAndPageContentBookId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Bookmark> result = bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            testUser.getId(),
            testBook.getId(),
            pageable
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber - 特定ページのブックマーク取得")
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber() {
        // When
        Optional<Bookmark> result = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                testUser.getId(),
                testBook.getId(),
                1L,
                1L
            );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(bookmark1.getId());
    }

    @Test
    @DisplayName("findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber - 存在しない場合")
    void testFindByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber_NotFound() {
        // When
        Optional<Bookmark> result = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                testUser.getId(),
                testBook.getId(),
                99L,
                99L
            );

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - IDリストからJOIN FETCHで取得")
    void testFindAllByIdInWithRelations() {
        // Given
        List<Long> ids = List.of(bookmark1.getId(), bookmark2.getId());

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Bookmark::getId)
            .containsExactlyInAnyOrder(bookmark1.getId(), bookmark2.getId());

        // 関連エンティティがFETCHされていることを確認
        Bookmark fetchedBookmark = result.stream()
            .filter(b -> b.getId().equals(bookmark1.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(fetchedBookmark.getUser()).isNotNull();
        assertThat(fetchedBookmark.getPageContent()).isNotNull();
        assertThat(fetchedBookmark.getPageContent().getBook()).isNotNull();
        assertThat(fetchedBookmark.getPageContent().getBook().getGenres()).isNotEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 空のIDリスト")
    void testFindAllByIdInWithRelations_EmptyList() {
        // Given
        List<Long> ids = List.of();

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 書籍のブックマークを一括論理削除")
    void testSoftDeleteAllByBookId() {
        // Given: bookmark1, bookmark2がアクティブ
        assertThat(bookmarkRepository.findByIdAndIsDeletedFalse(bookmark1.getId())).isPresent();
        assertThat(bookmarkRepository.findByIdAndIsDeletedFalse(bookmark2.getId())).isPresent();

        // When
        bookmarkRepository.softDeleteAllByBookId(testBook.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 両方とも論理削除される
        assertThat(bookmarkRepository.findByIdAndIsDeletedFalse(bookmark1.getId())).isEmpty();
        assertThat(bookmarkRepository.findByIdAndIsDeletedFalse(bookmark2.getId())).isEmpty();

        // DBから直接確認（is_deleted = true になっている）
        Bookmark deletedBookmark1 = entityManager.find(Bookmark.class, bookmark1.getId());
        assertThat(deletedBookmark1.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - JOIN FETCHでLazyLoad回避を実証")
    void testFindAllByIdInWithRelations_AvoidLazyLoadException() {
        // Given
        List<Long> ids = List.of(bookmark1.getId(), bookmark2.getId());

        // When
        List<Bookmark> result = bookmarkRepository.findAllByIdInWithRelations(ids);
        entityManager.clear(); // セッションをクリア

        // Then: JOIN FETCHにより、セッション外でも関連エンティティにアクセス可能
        assertThat(result).hasSize(2);

        Bookmark fetchedBookmark = result.get(0);
        assertThat(fetchedBookmark.getUser()).isNotNull();
        assertThat(fetchedBookmark.getUser().getDisplayName()).isNotNull(); // LazyLoadExceptionなし
        assertThat(fetchedBookmark.getPageContent()).isNotNull();
        assertThat(fetchedBookmark.getPageContent().getContent()).isNotNull(); // LazyLoadExceptionなし
        assertThat(fetchedBookmark.getPageContent().getBook()).isNotNull();
        assertThat(fetchedBookmark.getPageContent().getBook().getGenres()).isNotEmpty(); // LazyLoadExceptionなし
    }
}
