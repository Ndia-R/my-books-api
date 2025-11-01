package com.example.my_books_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security設定
 * OAuth2 Resource Serverとして動作
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * セキュリティフィルターチェーンの設定
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
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * JWTクレームからロール情報を抽出し、
     * ROLE_プレフィックス付きのGrantedAuthorityに変換
     *
     * 対応するクレーム:
     * - realm_access.roles: レルムレベルのロール
     * - resource_access.{client}.roles: クライアントレベルのロール
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            // デフォルトのscope権限を取得
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

            // レルムロールを取得
            List<String> realmRoles = extractRealmRoles(jwt);

            // リソース（クライアント）ロールを取得
            List<String> resourceRoles = extractResourceRoles(jwt);

            // すべてのロールをROLE_プレフィックス付きで追加
            Collection<GrantedAuthority> allAuthorities = Stream.concat(
                authorities.stream(),
                Stream.concat(
                    realmRoles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())),
                    resourceRoles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                )
            ).collect(Collectors.toList());

            return allAuthorities;
        };
    }

    /**
     * realm_access.rolesを抽出
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    /**
     * resource_access.{client}.rolesを抽出
     */
    @SuppressWarnings("unchecked")
    private List<String> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            return resourceAccess.values()
                .stream()
                .filter(resource -> resource instanceof Map)
                .flatMap(resource -> {
                    Map<String, Object> resourceMap = (Map<String, Object>) resource;
                    if (resourceMap.containsKey("roles")) {
                        return ((List<String>) resourceMap.get("roles")).stream();
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
