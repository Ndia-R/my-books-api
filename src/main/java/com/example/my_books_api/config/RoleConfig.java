package com.example.my_books_api.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ロール（Role）と権限（Permission）のマッピング設定
 * 
 * application.yml の roles.mappings から自動的に設定される
 */
@Configuration
@ConfigurationProperties(prefix = "roles")
public class RoleConfig {

    /**
     * ロール → 権限リストのマップ
     * 
     * 例:
     * {
     *   "USER": ["book-content:read:all", "favorite:manage:own", ...],
     *   "CONTENT_EDITOR": ["book:manage:all", "genre:manage:all", ...]
     * }
     */
    private Map<String, List<String>> mappings = new HashMap<>();

    public Map<String, List<String>> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, List<String>> mappings) {
        this.mappings = mappings;
    }

    /**
     * 指定したロールに含まれる権限のリストを取得
     * 
     * @param role ロール（例: "ADMIN"）
     * @return 権限のリスト（存在しない場合は空リスト）
     */
    public List<String> getRoles(String role) {
        return mappings.getOrDefault(role, Collections.emptyList());
    }
}