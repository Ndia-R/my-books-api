package com.example.my_books_backend.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.my_books_backend.exception.UnauthorizedException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class JwtClaimExtractorTest {

    private JwtClaimExtractor jwtClaimExtractor;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        jwtClaimExtractor = new JwtClaimExtractor();
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserId_正常系() {
        // Given
        String expectedUserId = "550e8400-e29b-41d4-a716-446655440000";
        Jwt jwt = createJwt(Map.of("sub", expectedUserId));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String userId = jwtClaimExtractor.getCurrentUserId();

        // Then
        assertEquals(expectedUserId, userId);
    }

    @Test
    void testGetCurrentUserId_認証されていない場合() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserId();
        });
        assertEquals("認証されていません", exception.getMessage());
    }

    @Test
    void testGetCurrentUserId_認証フラグがfalseの場合() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserId();
        });
        assertEquals("認証されていません", exception.getMessage());
    }

    @Test
    void testGetCurrentUserId_PrincipalがJwtでない場合() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("invalid-principal");

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserId();
        });
        assertEquals("無効な認証情報です", exception.getMessage());
    }

    @Test
    void testGetCurrentUserId_subクレームがnullの場合() {
        // Given
        Jwt jwt = createJwt(Map.of("email", "test@example.com"));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserId();
        });
        assertTrue(exception.getMessage().contains("ユーザーIDが取得できません"));
    }

    @Test
    void testGetCurrentUserEmail_正常系_emailクレーム() {
        // Given
        String expectedEmail = "test@example.com";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "email", expectedEmail
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String email = jwtClaimExtractor.getCurrentUserEmail();

        // Then
        assertEquals(expectedEmail, email);
    }

    @Test
    void testGetCurrentUserEmail_正常系_preferredUsernameクレーム() {
        // Given
        String expectedEmail = "preferred@example.com";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "preferred_username", expectedEmail
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String email = jwtClaimExtractor.getCurrentUserEmail();

        // Then
        assertEquals(expectedEmail, email);
    }

    @Test
    void testGetCurrentUserEmail_emailが優先される() {
        // Given
        String expectedEmail = "email@example.com";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "email", expectedEmail,
            "preferred_username", "preferred@example.com"
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String email = jwtClaimExtractor.getCurrentUserEmail();

        // Then
        assertEquals(expectedEmail, email);
    }

    @Test
    void testGetCurrentUserEmail_クレームが存在しない場合() {
        // Given
        Jwt jwt = createJwt(Map.of("sub", "user-id"));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserEmail();
        });
        assertTrue(exception.getMessage().contains("メールアドレスが取得できません"));
    }

    @Test
    void testGetCurrentUserName_正常系_nameクレーム() {
        // Given
        String expectedName = "Test User";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "name", expectedName
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String name = jwtClaimExtractor.getCurrentUserName();

        // Then
        assertEquals(expectedName, name);
    }

    @Test
    void testGetCurrentUserName_正常系_givenNameクレーム() {
        // Given
        String expectedName = "Given Name";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "given_name", expectedName
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String name = jwtClaimExtractor.getCurrentUserName();

        // Then
        assertEquals(expectedName, name);
    }

    @Test
    void testGetCurrentUserName_正常系_preferredUsernameクレーム() {
        // Given
        String expectedName = "preferred_user";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "preferred_username", expectedName
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String name = jwtClaimExtractor.getCurrentUserName();

        // Then
        assertEquals(expectedName, name);
    }

    @Test
    void testGetCurrentUserName_優先順位の検証() {
        // Given
        String expectedName = "Full Name";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "name", expectedName,
            "given_name", "Given",
            "preferred_username", "username"
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String name = jwtClaimExtractor.getCurrentUserName();

        // Then
        assertEquals(expectedName, name, "nameが最優先される");
    }

    @Test
    void testGetCurrentUserName_クレームが存在しない場合() {
        // Given
        Jwt jwt = createJwt(Map.of("sub", "user-id"));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtClaimExtractor.getCurrentUserName();
        });
        assertTrue(exception.getMessage().contains("ユーザー名が取得できません"));
    }

    @Test
    void testGetCurrentUserName_空文字列のクレームはスキップされる() {
        // Given
        String expectedName = "Valid Name";
        Jwt jwt = createJwt(Map.of(
            "sub", "user-id",
            "name", "",
            "given_name", expectedName
        ));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // When
        String name = jwtClaimExtractor.getCurrentUserName();

        // Then
        assertEquals(expectedName, name, "空文字列はスキップされる");
    }

    // ヘルパーメソッド：テスト用のJWTトークンを作成
    private Jwt createJwt(Map<String, Object> claims) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        return new Jwt(
            "token-value",
            issuedAt,
            expiresAt,
            headers,
            claims
        );
    }
}
