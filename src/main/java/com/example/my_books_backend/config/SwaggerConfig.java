package com.example.my_books_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${app.api.version}")
    private String apiVersion;

    @Value("${app.swagger.server.url}")
    private String serverUrl;

    @Value("${app.swagger.server.description}")
    private String serverDescription;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        // OpenID Connect 標準エンドポイントURL（issuer-uri から動的に構築）
        String authorizationUrl = issuerUri + "/protocol/openid-connect/auth";
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        String logoutUrl = issuerUri + "/protocol/openid-connect/logout";

        // OAuth2 Authorization Code フロー（PKCE対応）
        SecurityScheme oauthScheme = new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .flows(
                new io.swagger.v3.oas.models.security.OAuthFlows()
                    .authorizationCode(
                        new io.swagger.v3.oas.models.security.OAuthFlow()
                            .authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(
                                new io.swagger.v3.oas.models.security.Scopes()
                                    .addString("openid", "OpenID Connect")
                                    .addString("profile", "User Profile")
                                    .addString("email", "Email Address")
                            )
                    )
            );

        // SecurityRequirementの追加
        SecurityRequirement oauthRequirement = new SecurityRequirement().addList("oauth2");

        return new OpenAPI()
            .servers(
                List.of(
                    new Server()
                        .url(serverUrl)
                        .description(serverDescription)
                )
            )
            .info(
                new Info()
                    .title("My Books API " + apiVersion)
                    .version(apiVersion)
                    .description(
                        "書籍管理API - このAPIドキュメントはMy Books管理システムのAPIエンドポイントを説明します。\n\n" +
                            "## 認証方法\n" +
                            "「Authorize」ボタンをクリックして、OAuth2 認証を行ってください。\n\n" +
                            "## ログアウト方法\n" +
                            "Swagger UI で「Logout」をクリックした後、完全にログアウトするには以下のリンクをクリック:\n\n" +
                            "**[認証プロバイダーからログアウト](" + logoutUrl + ")**"
                    )
            )
            .components(
                new Components()
                    .addSecuritySchemes("oauth2", oauthScheme)
            )
            .addSecurityItem(oauthRequirement);
    }
}