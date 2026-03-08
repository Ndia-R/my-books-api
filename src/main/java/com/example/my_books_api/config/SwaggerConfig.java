package com.example.my_books_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        // Bearer トークン認証スキーム
        SecurityScheme bearerScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

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

        // SecurityRequirementの追加（両スキームを登録）
        SecurityRequirement securityRequirement = new SecurityRequirement()
            .addList("bearerAuth")
            .addList("oauth2");

        return new OpenAPI()
            .info(
                new Info()
                    .title("My Books API")
                    .description(
                        "書籍管理API - このAPIドキュメントはMy Books管理システムのAPIエンドポイントを説明します。\n\n" +
                            "---\n" +
                            "## bearerAuth  (http, Bearer)\n" +
                            "### APIテスト用トークン設定\n" +
                            "1. **[トークン取得用ページ](https://localhost/token-learn-swagger/swagger-ui/index.html)** からアクセストークンを取得する（認証プロバイダーから取得）\n"
                            +
                            "2. 右下の「Authorize 🔓」ボタンをクリックする\n" +
                            "3. 「Value」欄にアクセストークンを貼り付けて「Authorize」をクリックする\n" +
                            "4. 以降のAPIリクエストに `Authorization: Bearer <token>` が自動的に付与される\n" +
                            "### APIテスト用トークンを破棄する\n" +
                            "1. 右下の「Authorize 🔓」ボタンをクリックして、「Value」欄のすぐ下にある「Logout」をクリック\n" +
                            "---\n" +
                            "## oauth2 (OAuth2, authorizationCode with PKCE)\n" +
                            "### ログイン方法（OAuth2 認証）\n" +
                            "1. 右下の「Authorize 🔓」ボタンをクリックして、一番下までスクロールし「Authorize」をクリック\n" +
                            "2. 自動的に認証プロバイダーのログイン画面へ遷移するので、そこからログインする\n" +
                            "### ログアウト方法\n" +
                            "1. 右下の「Authorize 🔓」ボタンをクリックして、一番下までスクロールし「Logout」をクリック\n" +
                            "2. さらに、**[認証プロバイダーのログアウト画面](" + logoutUrl + ")** からログアウトする\n" +
                            "---\n"
                    )
                    .version("1.0.0")
            )
            .components(
                new Components()
                    .addSecuritySchemes("bearerAuth", bearerScheme)
                    .addSecuritySchemes("oauth2", oauthScheme)
            )
            .addSecurityItem(securityRequirement);
    }
}