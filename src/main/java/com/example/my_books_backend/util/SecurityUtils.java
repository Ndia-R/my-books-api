package com.example.my_books_backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.UnauthorizedException;
import com.example.my_books_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * セキュリティ関連のユーティリティクラス
 * JWTトークンから認証済みユーザー情報を取得する
 *
 * リクエストスコープでキャッシュを保持し、同一リクエスト内でのDB重複アクセスを防ぐ
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * 現在認証されているユーザーを取得
     *
     * 注意: このメソッドは重い処理（DBからUserエンティティ全体を取得）です。
     * ユーザーIDのみが必要な場合は getCurrentUserId() を使用してください。
     *
     * @return 認証済みユーザーエンティティ
     * @throws UnauthorizedException 認証されていない場合、またはユーザーが見つからない場合
     */
    public User getCurrentUser() {
        final String email = getCurrentUserEmail();

        // データベースからユーザーを検索（Rolesも含めて全取得）
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new UnauthorizedException("ユーザーが見つかりません: " + email));

        return user;
    }

    /**
     * 現在認証されているユーザーのemailを取得
     *
     * @return ユーザーのemail
     * @throws UnauthorizedException 認証されていない場合
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        return extractEmailFromJwt(jwt);
    }

    /**
     * 現在認証されているユーザーのIDを取得
     *
     * JWTのemailからユーザーIDのみを軽量クエリで取得します。
     * getCurrentUser()と違い、Userエンティティ全体を取得しないため高速です。
     *
     * @return ユーザーID
     * @throws UnauthorizedException 認証されていない場合、またはユーザーが見つからない場合
     */
    public Long getCurrentUserId() {
        final String email = getCurrentUserEmail();

        // IDのみを取得する軽量クエリ（SELECT u.id のみ）
        return userRepository.findIdByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new UnauthorizedException("ユーザーが見つかりません: " + email));
    }

    /**
     * JWTトークンからemailを抽出する
     * 優先順位: email > preferred_username > sub
     *
     * @param jwt JWTトークン
     * @return email文字列
     * @throws UnauthorizedException emailが取得できない場合
     */
    private String extractEmailFromJwt(Jwt jwt) {
        // 優先順位1: emailクレーム
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isEmpty()) {
            return email;
        }

        // 優先順位2: preferred_usernameクレーム
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) {
            return preferredUsername;
        }

        // 優先順位3: subjectクレーム
        String subject = jwt.getSubject();
        if (subject != null && !subject.isEmpty()) {
            return subject;
        }

        // いずれも取得できない場合はエラー
        throw new UnauthorizedException(
            "ユーザー情報が取得できません。JWT内にemail/preferred_username/subが見つかりません。Claims: " + jwt.getClaims()
        );
    }
}
