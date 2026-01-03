package com.example.my_books_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security設定
 * OAuth2 Resource Serverとして動作
 *
 * <p>Keycloakから発行されたJWTトークンの検証と、エンドポイントごとの権限チェックを行います。</p>
 *
 * <h3>権限体系</h3>
 * <ul>
 *   <li>基本Role (Permission): 最小単位の権限（例: book:write, review:delete:any）</li>
 *   <li>Composite Role: 権限のセット（例: user, premium-user, admin）</li>
 * </ul>
 *
 * <p>詳細は docs/ROLE-DESIGN.md を参照してください。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * セキュリティフィルターチェーンの設定
     *
     * <p>エンドポイントごとに必要な権限（Permission）を定義します。</p>
     * <p>動的な所有者チェックが必要な場合は、コントローラーメソッドで@PreAuthorizeを使用します。</p>
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

                    // GETのみパブリック: 書籍情報の閲覧
                    .requestMatchers(HttpMethod.GET, "/books/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/genres/**")
                    .permitAll()

                    // 管理者機能: ユーザー管理
                    .requestMatchers("/admin/users/**").hasRole("user:manage:all")

                    // 書籍管理
                    .requestMatchers(HttpMethod.POST, "/books").hasRole("book:write")
                    .requestMatchers(HttpMethod.PUT, "/books/**").hasRole("book:write")
                    .requestMatchers(HttpMethod.DELETE, "/books/**").hasRole("book:delete")

                    // ジャンル管理
                    .requestMatchers(HttpMethod.POST, "/genres").hasRole("genre:manage")
                    .requestMatchers(HttpMethod.PUT, "/genres/**").hasRole("genre:manage")
                    .requestMatchers(HttpMethod.DELETE, "/genres/**").hasRole("genre:manage")

                    // プレミアムコンテンツ（有料会員のみ）
                    .requestMatchers(HttpMethod.GET, "/book-content/**").hasRole("book:read:premium")

                    // レビュー管理
                    .requestMatchers(HttpMethod.POST, "/reviews").hasRole("review:write:own")
                    .requestMatchers(HttpMethod.PUT, "/reviews/**").hasRole("review:write:own")
                    // DELETE /reviews/** は動的権限チェック(@PreAuthorize)で制御

                    // お気に入り管理
                    .requestMatchers("/favorites/**").hasRole("favorite:manage")

                    // ブックマーク管理
                    .requestMatchers("/bookmarks/**").hasRole("bookmark:manage")

                    // ユーザープロフィール
                    .requestMatchers("/me/**").hasRole("user:read:own")

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
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Keycloakのアクセストークン内の `realm_access.roles` クレームを権限として使用する
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        // `ROLE_` プレフィックスを自動付与する
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
