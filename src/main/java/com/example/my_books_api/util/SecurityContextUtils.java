package com.example.my_books_api.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Securityコンテキスト操作ユーティリティ
 * SecurityContextから認証情報や権限を取得する
 */
@Component
public class SecurityContextUtils {

    /**
     * 指定した権限（Authority）を持っているかをSpring Securityのコンテキストから確認
     * （jwtAuthenticationConverterで展開された単一権限を確認する場合に使用）
     *
     * @param authority 確認する権限名（例: "review:delete:all"）
     * @return 権限を持っている場合true
     */
    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
