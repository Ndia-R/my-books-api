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

import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Genre;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class BookRepositoryTest {

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
    private BookRepository bookRepository;

    private Book testBook1;
    private Book testBook2;
    private Genre testGenre1;
    private Genre testGenre2;

    @BeforeEach
    void setUp() {
        // ジャンルを作成
        testGenre1 = new Genre();
        testGenre1.setName("Fantasy");
        testGenre1.setDescription("Fantasy books");
        entityManager.persist(testGenre1);

        testGenre2 = new Genre();
        testGenre2.setName("Adventure");
        testGenre2.setDescription("Adventure books");
        entityManager.persist(testGenre2);

        // 書籍を作成
        testBook1 = createBook("book-1", "Magic Adventures", Arrays.asList(testGenre1, testGenre2), false);
        entityManager.persist(testBook1);

        testBook2 = createBook("book-2", "Fantasy World", Arrays.asList(testGenre1), false);
        entityManager.persist(testBook2);

        Book deletedBook = createBook("book-deleted", "Deleted Book", Arrays.asList(), true);
        entityManager.persist(deletedBook);

        entityManager.flush();
    }

    private Book createBook(String id, String title, List<Genre> genres, boolean isDeleted) {
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
        book.setIsDeleted(isDeleted);
        book.setGenres(genres);
        return book;
    }

    @Test
    void testFindByIsDeletedFalse_正常系() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByIsDeletedFalse(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertFalse(result.getContent().stream()
            .anyMatch(Book::getIsDeleted));
    }

    @Test
    void testFindByTitleContainingAndIsDeletedFalse_正常系() {
        // Given
        String keyword = "Magic";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Magic Adventures", result.getContent().get(0).getTitle());
    }

    @Test
    void testFindByTitleContainingAndIsDeletedFalse_マッチなし() {
        // Given
        String keyword = "Nonexistent";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindDistinctByGenres_IdInAndIsDeletedFalse_正常系_OR条件() {
        // Given
        List<Long> genreIds = Arrays.asList(testGenre1.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(genreIds, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements()); // book-1 and book-2 both have genre1
    }

    @Test
    void testFindDistinctByGenres_IdInAndIsDeletedFalse_複数ジャンル() {
        // Given
        List<Long> genreIds = Arrays.asList(testGenre1.getId(), testGenre2.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(genreIds, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements()); // book-1 and book-2
    }

    @Test
    void testFindBooksHavingAllGenres_正常系_AND条件() {
        // Given
        List<Long> genreIds = Arrays.asList(testGenre1.getId(), testGenre2.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksHavingAllGenres(genreIds, 2L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements()); // Only book-1 has both genres
        assertEquals("book-1", result.getContent().get(0).getId());
    }

    @Test
    void testFindBooksHavingAllGenres_単一ジャンル() {
        // Given
        List<Long> genreIds = Arrays.asList(testGenre1.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksHavingAllGenres(genreIds, 1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements()); // book-1 and book-2 both have genre1
    }

    @Test
    void testFindAllByIdInWithRelations_正常系() {
        // Given
        List<String> ids = Arrays.asList("book-1", "book-2");

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // ジャンルが正しくフェッチされていることを確認
        Book book1 = result.stream()
            .filter(b -> "book-1".equals(b.getId()))
            .findFirst()
            .orElseThrow();

        assertNotNull(book1.getGenres());
        assertEquals(2, book1.getGenres().size());
    }

    @Test
    void testFindAllByIdInWithRelations_空のIDリスト() {
        // Given
        List<String> ids = Arrays.asList();

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindAllByIdInWithRelations_存在しないID() {
        // Given
        List<String> ids = Arrays.asList("non-existent-id");

        // When
        List<Book> result = bookRepository.findAllByIdInWithRelations(ids);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
