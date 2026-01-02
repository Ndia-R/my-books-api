package com.example.my_books_backend.util;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.my_books_backend.exception.UnauthorizedException;

/**
 * JWTクレーム抽出ユーティリティクラス
 * JWTトークンから認証済みユーザーのクレーム情報を取得する
 */
@Component
public class JwtClaimExtractor {

    /**
     * 現在認証されているユーザーのIDを取得
     * JWTのsubクレームから直接UUID（Keycloak User ID）を取得
     *
     * @return ユーザーID（Keycloak UUID）
     * @throws UnauthorizedException 認証されていない場合
     */
    public @NonNull String getCurrentUserId() {
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
     * 現在認証されているユーザーのusernameを取得
     * JWTクレームから取得
     *
     * @return ユーザーのusername
     * @throws UnauthorizedException 認証されていない場合
     */
    public @NonNull String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        return extractUsernameFromJwt(jwt);
    }

    /**
     * 現在認証されているユーザーのemailを取得
     * JWTクレームから取得
     *
     * @return ユーザーのemail
     * @throws UnauthorizedException 認証されていない場合
     */
    public @NonNull String getCurrentUserEmail() {
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
     * 現在認証されているユーザーのfamilyNameを取得
     * JWTクレームから取得
     *
     * @return ユーザーのfamilyName
     * @throws UnauthorizedException 認証されていない場合
     */
    public @NonNull String getCurrentFamilyName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        return extractFamilyNameFromJwt(jwt);
    }

    /**
     * 現在認証されているユーザーのgivenNameを取得
     * JWTクレームから取得
     *
     * @return ユーザーのgivenName
     * @throws UnauthorizedException 認証されていない場合
     */
    public @NonNull String getCurrentGivenName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        Jwt jwt = (Jwt) principal;

        return extractGivenNameFromJwt(jwt);
    }

    /**
     * JWTトークンからusernameを抽出する
     * 優先順位: preferred_username > name > given_name
     *
     * @param jwt JWTトークン
     * @return username文字列
     * @throws UnauthorizedException usernameが取得できない場合
     */
    private @NonNull String extractUsernameFromJwt(Jwt jwt) {
        // 優先順位1: preferred_usernameクレーム
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) {
            return preferredUsername;
        }

        // 優先順位2: nameクレーム
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isEmpty()) {
            return name;
        }

        // 優先順位3: given_nameクレーム
        String givenName = jwt.getClaimAsString("given_name");
        if (givenName != null && !givenName.isEmpty()) {
            return givenName;
        }

        // いずれも取得できない場合はエラー
        throw new UnauthorizedException(
            "ユーザー名が取得できません。JWT内にpreferred_username/name/given_nameが見つかりません。Claims: " + jwt.getClaims()
        );
    }

    /**
     * JWTトークンからemailを抽出する
     *
     * @param jwt JWTトークン
     * @return email文字列
     * @throws UnauthorizedException emailが取得できない場合
     */
    private @NonNull String extractEmailFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isEmpty()) {
            return email;
        }

        // 取得できない場合はエラー
        throw new UnauthorizedException(
            "メールアドレスが取得できません。JWT内にemailが見つかりません。Claims: " + jwt.getClaims()
        );
    }

    /**
     * JWTトークンからfamilyNameを抽出する
     *
     * @param jwt JWTトークン
     * @return familyName文字列
     * @throws UnauthorizedException familyNameが取得できない場合
     */
    private @NonNull String extractFamilyNameFromJwt(Jwt jwt) {
        String familyName = jwt.getClaimAsString("family_name");
        if (familyName != null && !familyName.isEmpty()) {
            return familyName;
        }

        // 取得できない場合はエラー
        throw new UnauthorizedException(
            "姓が取得できません。JWT内にfamily_nameが見つかりません。Claims: " + jwt.getClaims()
        );
    }

    /**
     * JWTトークンからgivenNameを抽出する
     *
     * @param jwt JWTトークン
     * @return givenName文字列
     * @throws UnauthorizedException givenNameが取得できない場合
     */
    private @NonNull String extractGivenNameFromJwt(Jwt jwt) {
        String givenName = jwt.getClaimAsString("given_name");
        if (givenName != null && !givenName.isEmpty()) {
            return givenName;
        }

        // 取得できない場合はエラー
        throw new UnauthorizedException(
            "名が取得できません。JWT内にgiven_nameが見つかりません。Claims: " + jwt.getClaims()
        );
    }
}
