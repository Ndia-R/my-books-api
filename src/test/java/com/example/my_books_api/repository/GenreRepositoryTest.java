package com.example.my_books_api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.my_books_api.entity.Genre;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class GenreRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Genre genre1;
    private Genre genre2;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE genres;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

        // アクティブジャンル1
        genre1 = new Genre();
        genre1.setName("ファンタジー");
        genre1.setDescription("ファンタジー作品のジャンル");
        entityManager.persist(genre1);

        // アクティブジャンル2
        genre2 = new Genre();
        genre2.setName("SF");
        genre2.setDescription("サイエンスフィクション作品のジャンル");
        entityManager.persist(genre2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void contextLoads() {
        assertThat(genreRepository).isNotNull();
    }

    @Test
    @DisplayName("findAll - 全ジャンル取得")
    void testFindAll() {
        // When
        List<Genre> result = genreRepository.findAll();

        // Then
        assertThat(result).hasSize(2); // genre1, genre2のみ
        assertThat(result)
            .extracting(Genre::getName)
            .containsExactlyInAnyOrder("ファンタジー", "SF");
    }

    @Test
    @DisplayName("findById - 存在するアクティブジャンルを取得")
    void testFindById_ActiveGenre() {
        // When
        Optional<Genre> result = genreRepository.findById(genre1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("ファンタジー");
        assertThat(result.get().getDescription()).isEqualTo("ファンタジー作品のジャンル");
    }

    @Test
    @DisplayName("findAllById - 複数ID指定で取得")
    void testFindAllById() {
        // Given
        List<Long> ids = List.of(genre1.getId(), genre2.getId());

        // When
        List<Genre> result = genreRepository.findAllById(ids);

        // Then: アクティブジャンルのみ取得
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Genre::getId)
            .containsExactlyInAnyOrder(genre1.getId(), genre2.getId());
    }

    @Test
    @DisplayName("findByName - 存在するジャンルを名前で検索")
    void testFindByName() {
        // When: アクティブジャンルを名前で検索
        Optional<Genre> result1 = genreRepository.findByName("ファンタジー");
        // 存在しないジャンルを名前で検索
        Optional<Genre> result2 = genreRepository.findByName("削除済みジャンル");

        // Then: アクティブジャンルは1つ取得される
        assertThat(result1).isPresent();
        assertThat(result1.get().getName()).isEqualTo("ファンタジー");

        // Then: 存在しないジャンルは取得されない
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("部分UNIQUE制約 - アクティブジャンル名の重複を防止")
    void testPartialUniqueConstraint_ActiveGenreDuplicatePrevented() {
        // Given: "ファンタジー"という名前のアクティブジャンルが既に存在

        // When: 同じ名前のアクティブジャンルを作成しようとする
        Genre duplicateGenre = new Genre();
        duplicateGenre.setName("ファンタジー");
        duplicateGenre.setDescription("重複したジャンル");

        // Then: DataIntegrityViolationExceptionが発生
        assertThatThrownBy(() -> {
            genreRepository.save(duplicateGenre);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("save - 新規ジャンル作成")
    void testSave_NewGenre() {
        // Given
        Genre newGenre = new Genre();
        newGenre.setName("ミステリー");
        newGenre.setDescription("ミステリー作品のジャンル");

        // When
        Genre savedGenre = genreRepository.save(newGenre);
        entityManager.flush();

        // Then
        assertThat(savedGenre.getId()).isNotNull();
        assertThat(savedGenre.getCreatedAt()).isNotNull();
        assertThat(savedGenre.getUpdatedAt()).isNotNull();

        // DB確認
        Optional<Genre> result = genreRepository.findById(savedGenre.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("ミステリー");
    }

    @Test
    @DisplayName("save - ジャンル更新")
    void testSave_UpdateGenre() {
        // Given
        genre1.setName("ハイファンタジー");
        genre1.setDescription("更新された説明");

        // When
        genreRepository.save(genre1);
        entityManager.flush();

        // Then
        Optional<Genre> result = genreRepository.findById(genre1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("ハイファンタジー");
        assertThat(result.get().getDescription()).isEqualTo("更新された説明");
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteGenre - ジャンルを物理削除")
    void testDeleteGenre_PhysicallyDeletesGenre() {
        // Given
        Genre genreToDelete = new Genre();
        genreToDelete.setName("削除対象");
        genreToDelete.setDescription("削除されるジャンル");
        entityManager.persist(genreToDelete);
        entityManager.flush();
        Long genreId = genreToDelete.getId();

        // When
        genreRepository.deleteById(genreId);
        entityManager.flush();

        // Then: DBから完全に削除されていることを確認
        Optional<Genre> result = genreRepository.findById(genreId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteById - 存在しないIDで削除（例外をスローしない）")
    void testDeleteById_NonExistentId_DoesNotThrowException() {
        // Given
        Long nonExistentId = 999L; // 存在しないID

        // When
        // JpaRepositoryのdeleteByIdは、存在しないIDの場合でも例外をスローせず、サイレントに何もしない
        genreRepository.deleteById(nonExistentId);
        entityManager.flush();

        // Then: 例外がスローされなかったことを暗黙的に確認
        // 念のため、既存のジャンルが削除されていないことを確認
        Optional<Genre> result1 = genreRepository.findById(genre1.getId());
        assertThat(result1).isPresent();
    }

    @Test
    @DisplayName("findById - 存在しないID")
    void testFindById_NonExistentId_ReturnsEmptyOptional() {
        // Given
        Long nonExistentId = 999L; // 存在しないID

        // When
        Optional<Genre> result = genreRepository.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllById - 空のIDリスト")
    void testFindAllById_EmptyList() {
        // When
        List<Genre> result = genreRepository.findAllById(List.of());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllById - 存在しないID")
    void testFindAllById_NonExistentIds() {
        // When
        List<Genre> result = genreRepository.findAllById(List.of(999L, 1000L));

        // Then
        assertThat(result).isEmpty();
    }
}
