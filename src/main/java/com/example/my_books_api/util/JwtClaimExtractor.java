package com.example.my_books_api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.my_books_api.exception.UnauthorizedException;

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
     * @throws UnauthorizedException
     */
    public @NonNull String getUserId() {
        Jwt jwt = getAuthenticatedJwt();
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
     * 
     * @return ユーザーのusername
     */
    public @NonNull String getUsername() {
        Jwt jwt = getAuthenticatedJwt();
        return extractClaimFromJwt(jwt, "preferred_username", "name", "given_name");
    }

    /**
     * 現在認証されているユーザーのemailを取得
     * 
     * @return ユーザーのemail
     */
    public @NonNull String getEmail() {
        Jwt jwt = getAuthenticatedJwt();
        return extractClaimFromJwt(jwt, "email");
    }

    /**
     * 現在認証されているユーザーのfamilyNameを取得
     * 
     * @return ユーザーのfamilyName
     */
    public @NonNull String getFamilyName() {
        Jwt jwt = getAuthenticatedJwt();
        return extractClaimFromJwt(jwt, "family_name");
    }

    /**
     * 現在認証されているユーザーのgivenNameを取得
     * 
     * @return ユーザーのgivenName
     */
    public @NonNull String getGivenName() {
        Jwt jwt = getAuthenticatedJwt();
        return extractClaimFromJwt(jwt, "given_name");
    }

    /**
     * 現在認証されているユーザーのロールを取得する
     * （「ROLE_」プレフィックスのついたロールを抽出し、プレフィックスを削除した値をリストで返す）
     *
     * @return ユーザーのロールのリスト
     */
    public @NonNull List<String> getRoles() {
        Jwt jwt = getAuthenticatedJwt();

        // realm_access.roles クレームを取得
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");

        if (roles == null) {
            return new ArrayList<>();
        }

        List<String> returnRoles = new ArrayList<>();
        for (String role : roles) {
            if (role.startsWith("ROLE_")) {
                returnRoles.add(role.substring(5)); // "ROLE_" プレフィックスを除去
            }
        }
        return returnRoles;
    }

    /**
     * 現在認証されているユーザーのグループを取得する
     *
     * @return ユーザーのグループのリスト
     */
    public @NonNull List<String> getGroups() {
        Jwt jwt = getAuthenticatedJwt();

        List<String> groups = jwt.getClaim("groups");

        if (groups == null) {
            return new ArrayList<>();
        }

        return groups;
    }

    /**
     * 認証されたJWTを取得する
     * 
     * @return 認証されたJWT
     * @throws UnauthorizedException 認証されていない場合
     */
    private @NonNull Jwt getAuthenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("認証されていません");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt)) {
            throw new UnauthorizedException("無効な認証情報です");
        }

        return (Jwt) principal;
    }

    /**
     * JWTトークンから指定されたクレームを抽出する
     * 
     * @param jwt JWTトークン
     * @param claimKeys クレームキーの可変長引数
     * @return クレーム値
     * @throws UnauthorizedException クレームが取得できない場合
     */
    private @NonNull String extractClaimFromJwt(Jwt jwt, String... claimKeys) {
        for (String claimKey : claimKeys) {
            String claimValue = jwt.getClaimAsString(claimKey);
            if (claimValue != null && !claimValue.isEmpty()) {
                return claimValue;
            }
        }

        throw new UnauthorizedException(
            "クレームが取得できません。JWT内に" + String.join("/", claimKeys) + "が見つかりません。Claims:" + jwt.getClaims()
        );
    }

}
