package com.example.my_books_backend.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
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

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * セキュリティフィルターチェーンの設定
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF無効化（JWT認証のため不要）
            .csrf(csrf -> csrf.disable())

            // CORS設定を適用
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

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
     * JWTからKeycloakのロールを抽出してSpring SecurityのGrantedAuthorityに変換
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * realm_access.rolesとresource_access.{client}.rolesを
     * ROLE_プレフィックス付きのGrantedAuthorityに変換
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            // デフォルトのscope権限を取得
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

            // realm_access.rolesを取得
            List<String> realmRoles = extractRealmRoles(jwt);

            // resource_access.{client}.rolesを取得
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

    /**
     * CORS設定
     * BFFおよびフロントエンドからのアクセスを許可
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 許可するオリジン（環境変数から取得）
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // 許可するHTTPメソッド
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 許可するヘッダー
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 認証情報の送信を許可
        configuration.setAllowCredentials(true);

        // プリフライトリクエストのキャッシュ時間（秒）
        configuration.setMaxAge(3600L);

        // レスポンスで公開するヘッダー
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
