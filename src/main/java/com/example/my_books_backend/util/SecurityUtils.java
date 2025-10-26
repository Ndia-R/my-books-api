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
     * @return 認証済みユーザーエンティティ
     * @throws UnauthorizedException 認証されていない場合、またはユーザーが見つからない場合
     */
    public User getCurrentUser() {
        final String userId = getCurrentUserId();

        // データベースからユーザーを取得
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("ユーザーが見つかりません: " + userId));

        return user;
    }

    /**
     * 現在認証されているユーザーのIDを取得
     * JWTのsubクレームから直接UUID（Keycloak User ID）を取得
     *
     * @return ユーザーID（Keycloak UUID）
     * @throws UnauthorizedException 認証されていない場合
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        // JWTのsubクレームからユーザーID（UUID）を取得
        String userId = jwt.getSubject();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException(
                "ユーザーIDが取得できません。JWT内にsubが見つかりません。Claims: " + jwt.getClaims()
            );
        }

        return userId;
    }

    /**
     * 現在認証されているユーザーのemailを取得
     * JWTクレームから取得
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
     * 現在認証されているユーザーのnameを取得
     * JWTクレームから取得
     *
     * @return ユーザーのname
     * @throws UnauthorizedException 認証されていない場合
     */
    public String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        return extractNameFromJwt(jwt);
    }

    /**
     * JWTトークンからemailを抽出する
     * 優先順位: email > preferred_username
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

        // いずれも取得できない場合はエラー
        throw new UnauthorizedException(
            "メールアドレスが取得できません。JWT内にemail/preferred_usernameが見つかりません。Claims: " + jwt.getClaims()
        );
    }

    /**
     * JWTトークンからnameを抽出する
     * 優先順位: name > given_name > preferred_username
     *
     * @param jwt JWTトークン
     * @return name文字列
     * @throws UnauthorizedException nameが取得できない場合
     */
    private String extractNameFromJwt(Jwt jwt) {
        // 優先順位1: nameクレーム
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isEmpty()) {
            return name;
        }

        // 優先順位2: given_nameクレーム
        String givenName = jwt.getClaimAsString("given_name");
        if (givenName != null && !givenName.isEmpty()) {
            return givenName;
        }

        // 優先順位3: preferred_usernameクレーム
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) {
            return preferredUsername;
        }

        // いずれも取得できない場合はエラー
        throw new UnauthorizedException(
            "ユーザー名が取得できません。JWT内にname/given_name/preferred_usernameが見つかりません。Claims: " + jwt.getClaims()
        );
    }
}
