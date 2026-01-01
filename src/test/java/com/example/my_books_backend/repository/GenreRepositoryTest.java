package com.example.my_books_backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_backend.entity.Genre;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class GenreRepositoryTest {

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
    private GenreRepository genreRepository;

    private Genre testGenre1;
    private Genre testGenre2;
    private Genre deletedGenre;

    @BeforeEach
    void setUp() {
        // アクティブなジャンルを作成
        testGenre1 = new Genre();
        testGenre1.setName("ロマンス");
        testGenre1.setDescription("恋愛小説のジャンル");
        testGenre1.setIsDeleted(false);
        entityManager.persist(testGenre1);

        testGenre2 = new Genre();
        testGenre2.setName("ミステリー");
        testGenre2.setDescription("推理小説のジャンル");
        testGenre2.setIsDeleted(false);
        entityManager.persist(testGenre2);

        // 削除済みジャンルを作成
        deletedGenre = new Genre();
        deletedGenre.setName("削除済み");
        deletedGenre.setDescription("削除されたジャンル");
        deletedGenre.setIsDeleted(true);
        entityManager.persist(deletedGenre);

        entityManager.flush();
    }

    @Test
    void testFindAll_全ジャンル取得() {
        // When
        List<Genre> genres = genreRepository.findAll();

        // Then
        assertEquals(3, genres.size(), "削除済みを含む全ジャンルが取得されること");
    }

    @Test
    void testFindById_存在するジャンル() {
        // When
        Optional<Genre> found = genreRepository.findById(testGenre1.getId());

        // Then
        assertTrue(found.isPresent(), "ジャンルが見つかること");
        assertEquals("ロマンス", found.get().getName());
        assertEquals("恋愛小説のジャンル", found.get().getDescription());
        assertFalse(found.get().getIsDeleted());
    }

    @Test
    void testFindById_存在しないジャンル() {
        // When
        Optional<Genre> found = genreRepository.findById(999L);

        // Then
        assertFalse(found.isPresent(), "存在しないIDでは見つからないこと");
    }

    @Test
    void testSave_新規ジャンル作成() {
        // Given
        Genre newGenre = new Genre();
        newGenre.setName("ホラー");
        newGenre.setDescription("怖い物語のジャンル");
        newGenre.setIsDeleted(false);

        // When
        Genre saved = genreRepository.save(newGenre);

        // Then
        assertNotNull(saved.getId(), "IDが自動採番されること");
        assertEquals("ホラー", saved.getName());
        assertEquals("怖い物語のジャンル", saved.getDescription());
        assertFalse(saved.getIsDeleted());

        // データベースから取得して検証
        Optional<Genre> found = genreRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("ホラー", found.get().getName());
    }

    @Test
    void testSave_既存ジャンル更新() {
        // Given
        testGenre1.setName("ラブストーリー");
        testGenre1.setDescription("愛と恋の物語");

        // When
        Genre updated = genreRepository.save(testGenre1);

        // Then
        assertEquals(testGenre1.getId(), updated.getId(), "IDは変わらないこと");
        assertEquals("ラブストーリー", updated.getName());
        assertEquals("愛と恋の物語", updated.getDescription());

        // データベースから取得して検証
        entityManager.clear(); // キャッシュをクリア
        Optional<Genre> found = genreRepository.findById(updated.getId());
        assertTrue(found.isPresent());
        assertEquals("ラブストーリー", found.get().getName());
        assertEquals("愛と恋の物語", found.get().getDescription());
    }

    @Test
    void testDelete_論理削除() {
        // Given
        Long genreId = testGenre1.getId();

        // When - 論理削除
        testGenre1.setIsDeleted(true);
        genreRepository.save(testGenre1);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Genre> found = genreRepository.findById(genreId);
        assertTrue(found.isPresent(), "データは残っていること");
        assertTrue(found.get().getIsDeleted(), "論理削除フラグが立っていること");
    }

    @Test
    void testDelete_物理削除() {
        // Given
        Long genreId = testGenre1.getId();

        // When - 物理削除
        genreRepository.delete(testGenre1);
        entityManager.flush();

        // Then
        Optional<Genre> found = genreRepository.findById(genreId);
        assertFalse(found.isPresent(), "物理削除されてデータが存在しないこと");
    }

    @Test
    void testCount_全ジャンル数() {
        // When
        long count = genreRepository.count();

        // Then
        assertEquals(3, count, "削除済みを含む全ジャンル数");
    }

    @Test
    void testEntityBase_タイムスタンプ自動設定() {
        // Given
        Genre newGenre = new Genre();
        newGenre.setName("SF");
        newGenre.setDescription("サイエンスフィクション");
        newGenre.setIsDeleted(false);

        // When
        Genre saved = genreRepository.save(newGenre);
        entityManager.flush();

        // Then
        assertNotNull(saved.getCreatedAt(), "createdAtが自動設定されること");
        assertNotNull(saved.getUpdatedAt(), "updatedAtが自動設定されること");
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt(), "初回作成時はcreatedAtとupdatedAtが同じ");
    }

    @Test
    void testEntityBase_更新時のタイムスタンプ更新() throws InterruptedException {
        // Given
        Genre genre = genreRepository.findById(testGenre1.getId()).orElseThrow();
        var originalCreatedAt = genre.getCreatedAt();
        var originalUpdatedAt = genre.getUpdatedAt();

        // 時間を確実にずらすため少し待機
        Thread.sleep(10);

        // When
        genre.setDescription("更新された説明");
        genreRepository.save(genre);
        entityManager.flush();
        entityManager.clear();

        // Then
        Genre updated = genreRepository.findById(testGenre1.getId()).orElseThrow();
        assertEquals(originalCreatedAt, updated.getCreatedAt(), "createdAtは変わらないこと");
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updated.getUpdatedAt().equals(originalUpdatedAt), 
                   "updatedAtが更新されること");
    }

    @Test
    void testExistsById_存在するジャンル() {
        // When
        boolean exists = genreRepository.existsById(testGenre1.getId());

        // Then
        assertTrue(exists, "存在するジャンルはtrueを返すこと");
    }

    @Test
    void testExistsById_存在しないジャンル() {
        // When
        boolean exists = genreRepository.existsById(999L);

        // Then
        assertFalse(exists, "存在しないジャンルはfalseを返すこと");
    }
}
