package com.example.my_books_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * ロール権限マッピングテスト（Docker不要）
 *
 * application.yml の roles.mappings が ROLE-DESIGN.md の権限マトリックス（Section 5）と
 * 一致することを検証する。
 *
 * 権限変更手順:
 *   1. docs/ROLE-DESIGN.md を更新
 *   2. このテストの期待値を更新（テストが失敗する状態にする）
 *   3. application.yml を更新（テストが通る）
 */
class RoleConfigTest {

    private static Map<String, List<String>> roleMappings;

    /**
     * 権限の命名規則: {リソース}:{アクション}:{スコープ}
     * ROLE-DESIGN.md Section 2 に準拠
     */
    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
        "^[a-z][a-z-]*:[a-z]+:[a-z]+$"
    );

    private static final Set<String> VALID_ACTIONS = Set.of(
        "read",
        "create",
        "update",
        "delete",
        "manage",
        "exec"
    );

    private static final Set<String> VALID_SCOPES = Set.of(
        "any",
        "own",
        "department",
        "team",
        "public"
    );

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void loadRoleMappings() {
        Yaml yaml = new Yaml();
        try (InputStream is = RoleConfigTest.class.getClassLoader()
            .getResourceAsStream("application.yml")) {
            Map<String, Object> config = yaml.load(is);
            Map<String, Object> roles = (Map<String, Object>) config.get("roles");
            Map<String, List<String>> mappings = (Map<String, List<String>>) roles.get("mappings");
            roleMappings = mappings;
        } catch (Exception e) {
            throw new RuntimeException("application.yml の読み込みに失敗しました", e);
        }
    }

    // =========================================================================
    // ロール権限マトリックス検証（ROLE-DESIGN.md Section 5）
    // =========================================================================

    @Test
    @DisplayName("USER ロールの権限がマトリックスと一致する")
    void userRole_shouldMatchPermissionMatrix() {
        assertThat(roleMappings.get("USER")).containsExactlyInAnyOrder(
            "book-content:read:all",
            "favorite:manage:own",
            "bookmark:manage:own",
            "review:read:all",
            "review:manage:own",
            "user:read:own",
            "user:update:own"
        );
    }

    @Test
    @DisplayName("CONTENT_EDITOR ロールの権限がマトリックスと一致する")
    void contentEditorRole_shouldMatchPermissionMatrix() {
        assertThat(roleMappings.get("CONTENT_EDITOR")).containsExactlyInAnyOrder(
            "book:manage:all",
            "genre:manage:all"
        );
    }

    @Test
    @DisplayName("MODERATOR ロールの権限がマトリックスと一致する")
    void moderatorRole_shouldMatchPermissionMatrix() {
        assertThat(roleMappings.get("MODERATOR")).containsExactlyInAnyOrder(
            "review:read:all",
            "review:delete:all"
        );
    }

    @Test
    @DisplayName("ADMIN ロールの権限がマトリックスと一致する")
    void adminRole_shouldMatchPermissionMatrix() {
        assertThat(roleMappings.get("ADMIN")).containsExactlyInAnyOrder(
            "book:manage:all",
            "book-content:read:all",
            "favorite:manage:own",
            "bookmark:manage:own",
            "review:read:all",
            "review:manage:own",
            "review:delete:all",
            "genre:manage:all",
            "user:manage:all",
            "user:read:own",
            "user:update:own"
        );
    }

    @Test
    @DisplayName("定義されたロールは4つのみ（USER, CONTENT_EDITOR, MODERATOR, ADMIN）")
    void roleMappings_shouldContainExactlyFourRoles() {
        assertThat(roleMappings.keySet()).containsExactlyInAnyOrder(
            "USER",
            "CONTENT_EDITOR",
            "MODERATOR",
            "ADMIN"
        );
    }

    // =========================================================================
    // 命名規則検証（ROLE-DESIGN.md Section 2）
    // =========================================================================

    @Test
    @DisplayName("全権限が {リソース}:{アクション}:{スコープ} の命名規則に準拠している")
    void allPermissions_shouldFollowNamingConvention() {
        Set<String> allPermissions = roleMappings.values()
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toSet());

        for (String permission : allPermissions) {
            assertThat(permission)
                .as("権限 '%s' が命名規則 {リソース}:{アクション}:{スコープ} に準拠していること", permission)
                .matches(PERMISSION_PATTERN);

            String[] parts = permission.split(":");
            assertThat(VALID_ACTIONS)
                .as("権限 '%s' のアクション '%s' が有効なアクションであること", permission, parts[1])
                .contains(parts[1]);
            assertThat(VALID_SCOPES)
                .as("権限 '%s' のスコープ '%s' が有効なスコープであること", permission, parts[2])
                .contains(parts[2]);
        }
    }
}
