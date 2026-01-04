package com.example.my_books_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.example.my_books_backend.entity.User;

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
    private User deletedUser;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("TRUNCATE TABLE users;").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();

        testUser1 = new User();
        testUser1.setId("user-uuid-001");
        testUser1.setDisplayName("テストユーザー1");
        testUser1.setAvatarPath("/avatars/test1.jpg");
        testUser1.setIsDeleted(false);
        entityManager.persist(testUser1);

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
        User testUser2 = new User();
        testUser2.setId("user-uuid-002");
        testUser2.setDisplayName("テストユーザー2");
        testUser2.setAvatarPath("/avatars/test2.jpg");
        testUser2.setIsDeleted(false);
        entityManager.persist(testUser2);
        entityManager.flush();
        entityManager.clear();

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
}
