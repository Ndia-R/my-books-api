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

import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.entity.User;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Genre genre1;
    private Genre genre2;
    private Genre genre3;
    private User testUser;
    private Book book1;
    private Book book2;
    private Book deletedBook;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE book_genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE books;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE users;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

        // ジャンルの作成
        genre1 = new Genre();
        genre1.setName("ファンタジー");
        genre1.setDescription("ファンタジー作品のジャンル");
        entityManager.persist(genre1);

        genre2 = new Genre();
        genre2.setName("SF");
        genre2.setDescription("サイエンスフィクション作品のジャンル");
        entityManager.persist(genre2);

        genre3 = new Genre();
        genre3.setName("ミステリー");
        genre3.setDescription("ミステリー作品のジャンル");
        entityManager.persist(genre3);

        // ユーザーの作成
        testUser = new User();
        testUser.setId("user-uuid-001");
        testUser.setDisplayName("テスト著者");
        testUser.setAvatarPath("/avatars/author.jpg");
        testUser.setIsDeleted(false);
        entityManager.persist(testUser);

        // 書籍1: ファンタジー + SF
        book1 = new Book();
        book1.setId("book-uuid-001");
        book1.setTitle("異世界転生物語");
        book1.setDescription("主人公が異世界に転生する物語");
        book1.setGenres(List.of(genre1, genre2));
        book1.setAuthors("テスト著者");
        book1.setPublisher("テスト出版社");
        book1.setPublicationDate(Date.valueOf("2024-01-01"));
        book1.setPrice(1200L);
        book1.setPageCount(300L);
        book1.setIsbn("978-4-1234-5678-9");
        book1.setImagePath("/images/book1.jpg");
        book1.setReviewCount(10L);
        book1.setAverageRating(4.5);
        book1.setPopularity(100.0);
        book1.setIsDeleted(false);
        entityManager.persist(book1);

        // 書籍2: ファンタジーのみ
        book2 = new Book();
        book2.setId("book-uuid-002");
        book2.setTitle("魔法学園の日々");
        book2.setDescription("魔法学園での学生生活");
        book2.setGenres(List.of(genre1));
        book2.setAuthors("テスト著者");
        book2.setPublisher("テスト出版社");
        book2.setPublicationDate(Date.valueOf("2024-02-01"));
        book2.setPrice(1000L);
        book2.setPageCount(250L);
        book2.setIsbn("978-4-1234-5679-6");
        book2.setImagePath("/images/book2.jpg");
        book2.setReviewCount(5L);
        book2.setAverageRating(4.0);
        book2.setPopularity(80.0);
        book2.setIsDeleted(false);
        entityManager.persist(book2);

        // 削除済み書籍
        deletedBook = new Book();
        deletedBook.setId("book-uuid-deleted");
        deletedBook.setTitle("削除済み書籍");
        deletedBook.setDescription("論理削除された書籍");
        deletedBook.setGenres(List.of(genre3));
        deletedBook.setAuthors("削除済み著者");
        deletedBook.setPublisher("削除済み出版社");
        deletedBook.setPublicationDate(Date.valueOf("2023-01-01"));
        deletedBook.setPrice(800L);
        deletedBook.setPageCount(200L);
        deletedBook.setIsbn("978-4-1234-0000-0");
        deletedBook.setImagePath("/images/deleted.jpg");
        deletedBook.setReviewCount(0L);
        deletedBook.setAverageRating(0.0);
        deletedBook.setPopularity(0.0);
        deletedBook.setIsDeleted(true);
        entityManager.persist(deletedBook);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(bookRepository).isNotNull();
    }

    @Test
    @DisplayName("save - 新規書籍作成")
    void testSave_NewBook() {
        // Given
        Book newBook = new Book();
        newBook.setId("book-uuid-new");
        newBook.setTitle("新しい書籍");
        newBook.setDescription("新規作成される書籍");
        newBook.setGenres(List.of(genre1));
        newBook.setAuthors("新規著者");
        newBook.setPublisher("新規出版社");
        newBook.setPublicationDate(Date.valueOf("2024-03-01"));
        newBook.setPrice(1500L);
        newBook.setPageCount(350L);
        newBook.setIsbn("978-4-1234-9999-9");
        newBook.setImagePath("/images/new.jpg");
        newBook.setReviewCount(0L);
        newBook.setAverageRating(0.0);
        newBook.setPopularity(0.0);
        newBook.setIsDeleted(false);

        // When
        Book savedBook = bookRepository.save(newBook);
        entityManager.flush();

        // Then
        assertThat(savedBook.getId()).isEqualTo("book-uuid-new");
        assertThat(savedBook.getCreatedAt()).isNotNull();
        assertThat(savedBook.getUpdatedAt()).isNotNull();

        // DB確認
        Optional<Book> result = bookRepository.findByIdAndIsDeletedFalse("book-uuid-new");
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("新しい書籍");
    }

    @Test
    @DisplayName("save - 既存書籍更新")
    void testSave_UpdateBook() {
        // Given
        book1.setTitle("更新されたタイトル");
        book1.setPrice(1500L);

        // When
        bookRepository.save(book1);
        entityManager.flush();

        // Then
        Optional<Book> result = bookRepository.findByIdAndIsDeletedFalse(book1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("更新されたタイトル");
        assertThat(result.get().getPrice()).isEqualTo(1500L);
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在するアクティブ書籍を取得")
    void testFindByIdAndIsDeletedFalse_ActiveBook() {
        // When
        Optional<Book> result = bookRepository.findByIdAndIsDeletedFalse(book1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("book-uuid-001");
        assertThat(result.get().getTitle()).isEqualTo("異世界転生物語");
        assertThat(result.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 論理削除済み書籍は取得できない")
    void testFindByIdAndIsDeletedFalse_DeletedBook() {
        // When
        Optional<Book> result = bookRepository.findByIdAndIsDeletedFalse(deletedBook.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse - 存在しないIDではEmptyを返す")
    void testFindByIdAndIsDeletedFalse_NonExistentBook() {
        // When
        Optional<Book> result = bookRepository.findByIdAndIsDeletedFalse("non-existent-uuid");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIsDeletedFalse - アクティブ書籍のみ取得（ページネーション）")
    void testFindByIsDeletedFalse_Pagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2); // book1, book2のみ
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(Book::getId)
            .containsExactlyInAnyOrder("book-uuid-001", "book-uuid-002");
    }

    @Test
    @DisplayName("findByTitleContainingAndIsDeletedFalse - タイトル検索")
    void testFindByTitleContainingAndIsDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When: "異世界"を含むタイトルで検索
        Page<Book> result = bookRepository.findByTitleContainingAndIsDeletedFalse("異世界", pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("異世界転生物語");
    }

    @Test
    @DisplayName("findDistinctByGenres_IdInAndIsDeletedFalse - 単一ジャンルOR検索")
    void testFindDistinctByGenres_IdInAndIsDeletedFalse_SingleGenre() {
        // Given
        List<Long> genreIds = List.of(genre1.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When: ファンタジージャンルで検索
        Page<Book> result = bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(genreIds, pageable);

        // Then: book1, book2が該当
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Book::getId)
            .containsExactlyInAnyOrder("book-uuid-001", "book-uuid-002");
    }

    @Test
    @DisplayName("findDistinctByGenres_IdInAndIsDeletedFalse - 複数ジャンルOR検索")
    void testFindDistinctByGenres_IdInAndIsDeletedFalse_MultipleGenres() {
        // Given
        List<Long> genreIds = List.of(genre1.getId(), genre2.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When: ファンタジーまたはSFジャンルで検索
        Page<Book> result = bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(genreIds, pageable);

        // Then: book1, book2が該当（book1は両方のジャンルを持つ）
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Book::getId)
            .containsExactlyInAnyOrder("book-uuid-001", "book-uuid-002");
    }

    @Test
    @DisplayName("findBooksHavingAllGenres - AND条件：全ジャンル一致")
    void testFindBooksHavingAllGenres_AllGenresMatch() {
        // Given: ファンタジーとSFの両方を含む書籍を検索
        List<Long> genreIds = List.of(genre1.getId(), genre2.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksHavingAllGenres(genreIds, (long) genreIds.size(), pageable);

        // Then: book1のみが該当（ファンタジーとSF両方を持つ）
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("book-uuid-001");
    }

    @Test
    @DisplayName("findBooksHavingAllGenres - AND条件：部分一致（検索結果なし）")
    void testFindBooksHavingAllGenres_PartialMatch() {
        // Given: ファンタジー、SF、ミステリーの全てを含む書籍を検索
        List<Long> genreIds = List.of(genre1.getId(), genre2.getId(), genre3.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksHavingAllGenres(genreIds, (long) genreIds.size(), pageable);

        // Then: 3つ全てを持つ書籍は存在しない
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findBooksHavingAllGenres - 空のジャンルリスト")
    void testFindBooksHavingAllGenres_EmptyGenreList() {
        // Given
        List<Long> genreIds = List.of();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksHavingAllGenres(genreIds, 0L, pageable);

        // Then: 空のリストなので結果なし
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - IDリストからJOIN FETCHで取得")
    void testFindAllByIdInWithRelations_WithGenres() {
        // Given
        List<String> ids = List.of("book-uuid-001", "book-uuid-002");

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Book::getId)
            .containsExactlyInAnyOrder("book-uuid-001", "book-uuid-002");

        // ジャンルがFETCHされていることを確認
        Book fetchedBook1 = result.stream()
            .filter(b -> b.getId().equals("book-uuid-001"))
            .findFirst()
            .orElseThrow();
        assertThat(fetchedBook1.getGenres()).hasSize(2);
        assertThat(fetchedBook1.getGenres())
            .extracting(Genre::getName)
            .containsExactlyInAnyOrder("ファンタジー", "SF");
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - 空のIDリスト")
    void testFindAllByIdInWithRelations_EmptyList() {
        // Given
        List<String> ids = List.of();

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByIdAndIsDeletedFalse - 存在する書籍（true）")
    void testExistsByIdAndIsDeletedFalse_ExistingBook() {
        // When
        Boolean exists = bookRepository.existsByIdAndIsDeletedFalse(book1.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByIdAndIsDeletedFalse - 論理削除済み書籍（false）")
    void testExistsByIdAndIsDeletedFalse_DeletedBook() {
        // When
        Boolean exists = bookRepository.existsByIdAndIsDeletedFalse(deletedBook.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndIsDeletedFalse - 存在しない書籍（false）")
    void testExistsByIdAndIsDeletedFalse_NonExistentBook() {
        // When
        Boolean exists = bookRepository.existsByIdAndIsDeletedFalse("non-existent-uuid");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByIsDeletedFalse - titleでソート（昇順）")
    void testFindByIsDeletedFalse_SortByTitleAsc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then: 五十音順ソート「異世界転生物語」→「魔法学園の日々」
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("異世界転生物語");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("魔法学園の日々");
    }

    @Test
    @DisplayName("findByTitleContainingAndIsDeletedFalse - titleでソート")
    void testFindByTitleContainingAndIsDeletedFalse_WithSort() {
        // Given: 「世界」を含むタイトルで検索
        Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());

        // When
        Page<Book> result = bookRepository.findByTitleContainingAndIsDeletedFalse("世界", pageable);

        // Then: 「世界」を含むのは「異世界転生物語」のみ
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("異世界転生物語");
    }

    @Test
    @DisplayName("findByIsDeletedFalse - ページサイズ1での取得")
    void testFindByIsDeletedFalse_PageSize1() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("findByIsDeletedFalse - 最後のページ取得")
    void testFindByIsDeletedFalse_LastPage() {
        // Given
        Pageable pageable = PageRequest.of(1, 1); // 2ページ目

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("findByIsDeletedFalse - 存在しないページ番号")
    void testFindByIsDeletedFalse_NonExistentPage() {
        // Given
        Pageable pageable = PageRequest.of(10, 10); // 存在しないページ

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2); // 総件数は正しい
        assertThat(result.getTotalPages()).isEqualTo(1); // 総ページ数は1
    }

    @Test
    @DisplayName("findAllByIdInWithRelations - JOIN FETCHでLazyLoad回避を実証")
    void testFindAllByIdInWithRelations_AvoidLazyLoadException() {
        // Given
        List<String> ids = List.of(book1.getId(), book2.getId());

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);
        entityManager.clear(); // セッションをクリア（重要：これによりLazyLoadが発生する可能性がある）

        // Then: JOIN FETCHにより、セッション外でも関連エンティティにアクセス可能
        assertThat(result).hasSize(2);

        // LazyLoadExceptionが発生しないことを実証
        Book fetchedBook = result.get(0);
        assertThat(fetchedBook.getGenres()).isNotNull();
        assertThat(fetchedBook.getGenres()).isNotEmpty();
        assertThat(fetchedBook.getGenres().get(0).getName()).isNotNull(); // セッション外でもアクセス可能
    }
}
