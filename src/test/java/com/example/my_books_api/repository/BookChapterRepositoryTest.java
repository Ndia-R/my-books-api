package com.example.my_books_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookChapter;
import com.example.my_books_api.entity.BookChapterId;
import com.example.my_books_api.entity.Genre;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookChapterRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private BookChapterRepository bookChapterRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Book testBook;
    private BookChapter chapter1;
    private BookChapter chapter2;
    private BookChapter deletedChapter;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_chapters;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE books;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

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

        // 章1
        BookChapterId chapterId1 = new BookChapterId("book-uuid-001", 1L);
        chapter1 = new BookChapter();
        chapter1.setId(chapterId1);
        chapter1.setTitle("第1章：始まり");
        chapter1.setBook(testBook);
        chapter1.setIsDeleted(false);
        entityManager.persist(chapter1);

        // 章2
        BookChapterId chapterId2 = new BookChapterId("book-uuid-001", 2L);
        chapter2 = new BookChapter();
        chapter2.setId(chapterId2);
        chapter2.setTitle("第2章：展開");
        chapter2.setBook(testBook);
        chapter2.setIsDeleted(false);
        entityManager.persist(chapter2);

        // 削除済み章
        BookChapterId deletedChapterId = new BookChapterId("book-uuid-001", 3L);
        deletedChapter = new BookChapter();
        deletedChapter.setId(deletedChapterId);
        deletedChapter.setTitle("第3章：削除済み");
        deletedChapter.setBook(testBook);
        deletedChapter.setIsDeleted(true);
        entityManager.persist(deletedChapter);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(bookChapterRepository).isNotNull();
    }

    @Test
    @DisplayName("save - 新規章作成")
    void testSave_NewBookChapter() {
        // Given
        BookChapterId newChapterId = new BookChapterId("book-uuid-001", 4L);
        BookChapter newChapter = new BookChapter();
        newChapter.setId(newChapterId);
        newChapter.setTitle("第4章：新章");
        newChapter.setBook(testBook);
        newChapter.setIsDeleted(false);

        // When
        BookChapter savedChapter = bookChapterRepository.save(newChapter);
        entityManager.flush();

        // Then
        assertThat(savedChapter.getId().getBookId()).isEqualTo("book-uuid-001");
        assertThat(savedChapter.getId().getChapterNumber()).isEqualTo(4L);
        assertThat(savedChapter.getTitle()).isEqualTo("第4章：新章");
        assertThat(savedChapter.getCreatedAt()).isNotNull();
        assertThat(savedChapter.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById_BookIdAndIsDeletedFalse - 書籍IDから複数章を取得")
    void testFindById_BookIdAndIsDeletedFalse_MultipleChapters() {
        // When
        List<BookChapter> result = bookChapterRepository.findById_BookIdAndIsDeletedFalse("book-uuid-001");

        // Then: chapter1, chapter2のみ（deletedChapterは除外）
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(chapter -> chapter.getId().getChapterNumber())
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(result)
            .extracting(BookChapter::getTitle)
            .containsExactlyInAnyOrder("第1章：始まり", "第2章：展開");
    }

    @Test
    @DisplayName("findById_BookIdAndIsDeletedFalse - 章なし")
    void testFindById_BookIdAndIsDeletedFalse_NoChapters() {
        // When: 存在しない書籍IDで検索
        List<BookChapter> result = bookChapterRepository.findById_BookIdAndIsDeletedFalse("non-existent-book-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById_BookIdAndIsDeletedFalse - 論理削除済みのみ")
    void testFindById_BookIdAndIsDeletedFalse_OnlyDeletedChapters() {
        // Given: 全ての章を論理削除
        chapter1.setIsDeleted(true);
        chapter2.setIsDeleted(true);
        bookChapterRepository.save(chapter1);
        bookChapterRepository.save(chapter2);
        entityManager.flush();

        // When
        List<BookChapter> result = bookChapterRepository.findById_BookIdAndIsDeletedFalse("book-uuid-001");

        // Then: 全て削除済みなので結果なし
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdInAndIsDeletedFalse - 複数BookChapterIdから取得")
    void testFindByIdInAndIsDeletedFalse_MultipleIds() {
        // Given
        BookChapterId id1 = new BookChapterId("book-uuid-001", 1L);
        BookChapterId id2 = new BookChapterId("book-uuid-001", 2L);
        List<BookChapterId> ids = List.of(id1, id2);

        // When
        List<BookChapter> result = bookChapterRepository.findByIdInAndIsDeletedFalse(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(chapter -> chapter.getId().getChapterNumber())
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("findByIdInAndIsDeletedFalse - 空のIDリスト")
    void testFindByIdInAndIsDeletedFalse_EmptyList() {
        // Given
        List<BookChapterId> ids = List.of();

        // When
        List<BookChapter> result = bookChapterRepository.findByIdInAndIsDeletedFalse(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdInAndIsDeletedFalse - 存在しないID")
    void testFindByIdInAndIsDeletedFalse_NonExistentIds() {
        // Given
        BookChapterId nonExistentId1 = new BookChapterId("non-existent-book", 1L);
        BookChapterId nonExistentId2 = new BookChapterId("book-uuid-001", 999L);
        List<BookChapterId> ids = List.of(nonExistentId1, nonExistentId2);

        // When
        List<BookChapter> result = bookChapterRepository.findByIdInAndIsDeletedFalse(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 書籍削除時の一括ソフト削除成功")
    void testSoftDeleteAllByBookId_Success() {
        // When: 書籍IDで章を一括ソフト削除
        bookChapterRepository.softDeleteAllByBookId("book-uuid-001");
        entityManager.flush();
        entityManager.clear();

        // Then: 全ての章がソフト削除される
        List<BookChapter> activeChapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse("book-uuid-001");
        assertThat(activeChapters).isEmpty();

        // DB確認: データは物理的に残っている
        List<BookChapter> allChapters = entityManager.getEntityManager()
            .createQuery("SELECT c FROM BookChapter c WHERE c.book.id = :bookId", BookChapter.class)
            .setParameter("bookId", "book-uuid-001")
            .getResultList();
        assertThat(allChapters).hasSize(3); // 全てのデータが残っている
        assertThat(allChapters).allMatch(BookChapter::getIsDeleted); // 全てisDeleted=true
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 章なし")
    void testSoftDeleteAllByBookId_NoChapters() {
        // When: 存在しない書籍IDで一括削除
        bookChapterRepository.softDeleteAllByBookId("non-existent-book-id");
        entityManager.flush();

        // Then: エラーなく終了
        List<BookChapter> chapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse("book-uuid-001");
        assertThat(chapters).hasSize(2); // 既存の章は影響を受けない
    }

    @Test
    @DisplayName("softDeleteAllByBookId - 既に削除済み")
    void testSoftDeleteAllByBookId_AlreadyDeleted() {
        // Given: 全ての章を先にソフト削除
        bookChapterRepository.softDeleteAllByBookId("book-uuid-001");
        entityManager.flush();
        entityManager.clear();

        // When: 再度ソフト削除
        bookChapterRepository.softDeleteAllByBookId("book-uuid-001");
        entityManager.flush();

        // Then: エラーなく終了
        List<BookChapter> activeChapters = bookChapterRepository.findById_BookIdAndIsDeletedFalse("book-uuid-001");
        assertThat(activeChapters).isEmpty();
    }
}
