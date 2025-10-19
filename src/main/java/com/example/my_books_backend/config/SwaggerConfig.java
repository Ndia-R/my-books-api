package com.example.my_books_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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

    @Bean
    public OpenAPI customOpenAPI() {
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
                    .description("書籍管理API - このAPIドキュメントはMy Books管理システムのAPIエンドポイントを説明します。")
            );
    }
}