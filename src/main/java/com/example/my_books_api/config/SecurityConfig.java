package com.example.my_books_api.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security設定
 * OAuth2 Resource Serverとして動作
 *
 * <p>
 * Keycloakから発行されたJWTトークンの検証と、エンドポイントごとの権限チェックを行います。
 * </p>
 *
 * <p>
 * 詳細は docs/ROLE-DESIGN.md を参照してください。
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final RoleConfig roleConfig;

    /**
     * セキュリティフィルターチェーンの設定
     *
     * <p>
     * エンドポイントごとに必要な権限（Permission）を定義します。
     * </p>
     * <p>
     * 動的な所有者チェックが必要な場合は、コントローラーメソッドで@PreAuthorizeを使用します。
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF無効化（JWT認証のため不要）
            .csrf(csrf -> csrf.disable())

            // セッションレス（JWTベース認証のため）
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // エンドポイント認可設定
            .authorizeHttpRequests(
                auth -> auth
                    // 完全パブリック: Swagger UI関連
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                    )
                    .permitAll()

                    // 完全パブリック: Actuator（必要に応じて制限可能）
                    .requestMatchers("/actuator/**")
                    .permitAll()

                    // 書籍管理
                    .requestMatchers(HttpMethod.GET, "/books/*/reviews")
                    .hasAuthority("review:read:all")
                    .requestMatchers(HttpMethod.GET, "/books/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/books")
                    .hasAuthority("book:manage:all")
                    .requestMatchers(HttpMethod.PUT, "/books/**")
                    .hasAuthority("book:manage:all")
                    .requestMatchers(HttpMethod.DELETE, "/books/**")
                    .hasAuthority("book:manage:all")

                    // プレミアムコンテンツ（試し読みと有料コンテンツ）
                    .requestMatchers(HttpMethod.GET, "/book-content/preview/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/book-content/**")
                    .hasAuthority("book-content:read:all")

                    // レビュー管理
                    .requestMatchers(HttpMethod.DELETE, "/reviews/**")
                    .hasAnyAuthority("review:manage:own", "review:delete:all")
                    .requestMatchers("/reviews/**")
                    .hasAuthority("review:manage:own")

                    // お気に入り管理
                    .requestMatchers("/favorites/**")
                    .hasAuthority("favorite:manage:own")

                    // ブックマーク管理
                    .requestMatchers("/bookmarks/**")
                    .hasAuthority("bookmark:manage:own")

                    // ジャンル管理
                    .requestMatchers(HttpMethod.GET, "/genres/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/genres")
                    .hasAuthority("genre:manage:all")
                    .requestMatchers(HttpMethod.PUT, "/genres/**")
                    .hasAuthority("genre:manage:all")
                    .requestMatchers(HttpMethod.DELETE, "/genres/**")
                    .hasAuthority("genre:manage:all")

                    // ユーザープロフィール
                    .requestMatchers(HttpMethod.GET, "/me/**")
                    .hasAuthority("user:read:own")
                    .requestMatchers(HttpMethod.PUT, "/me/profile")
                    .hasAuthority("user:update:own")
                    .requestMatchers(HttpMethod.PUT, "/me/subscription")
                    .hasAuthority("user:update:own")

                    // 試し読み設定（管理者のみ）
                    .requestMatchers("/preview-settings/**")
                    .hasAuthority("book:manage:all")

                    // 管理者機能: ユーザー管理
                    .requestMatchers("/admin/users/**")
                    .hasAuthority("user:manage:all")

                    // その他すべて認証必要
                    .anyRequest()
                    .authenticated()
            )

            // OAuth2 Resource Server設定（JWT認証）
            .oauth2ResourceServer(
                oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * JWTからロールを抽出してSpring SecurityのGrantedAuthorityに変換
     *
     * <p>
     * Keycloakの `realm_access.roles` クレームからロールを抽出します。
     * </p>
     * <p>
     * Spring SecurityのJwtGrantedAuthoritiesConverterはネストしたクレームパスをサポートしていないため、
     * カスタムコンバーターを実装しています。
     * </p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        // Keycloakの realm_access.roles から権限を抽出するカスタムコンバーター
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // realm_access.roles クレームを取得
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            // ロールを権限に展開し、マージ（重複除去）
            // JWTの roles には "ROLE_USER" のようにプレフィックス付きで格納されている
            Collection<GrantedAuthority> authorities = roles.stream()
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring("ROLE_".length()))
                .flatMap(role -> roleConfig.getRoles(role).stream())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

            return authorities;
        });

        return jwtAuthenticationConverter;
    }
}
