package com.example.my_books_backend.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 権限セット（Permission Set）と単一権限（Permission）のマッピング設定
 * 
 * application.yml の permission-sets.mappings から自動的に設定される
 */
@Configuration
@ConfigurationProperties(prefix = "permission-sets")
public class PermissionSetConfig {

    /**
     * 権限セット名 → 単一権限リストのマップ
     * 
     * 例:
     * {
     *   "general-user": ["book-preview:read:any", "favorite:manage:own", ...],
     *   "premium-user": ["book-preview:read:any", "book-content:read:any", ...]
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
     * 指定した権限セットに含まれる単一権限のリストを取得
     * 
     * @param permissionSet 権限セット名（例: "premium-user"）
     * @return 単一権限のリスト（存在しない場合は空リスト）
     */
    public List<String> getPermissions(String permissionSet) {
        return mappings.getOrDefault(permissionSet, Collections.emptyList());
    }
}