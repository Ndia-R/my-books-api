# My Books Backend - プロジェクト概要

## プロジェクトの目的
Spring Boot 3.3.5とJava 21で構築された書籍管理REST APIシステム。ユーザー認証、書籍管理、レビュー、お気に入り、ブックマーク、章ページ機能を提供する包括的な書籍管理システム。

## 主要技術スタック
- **フレームワーク**: Spring Boot 3.3.5
- **Java**: 21
- **データベース**: MySQL 8.0 (JPA/Hibernate)
- **認証**: Keycloak (OAuth 2.0 / OpenID Connect)
- **ドキュメント**: OpenAPI 3 (Swagger UI)
- **マッピング**: MapStruct 1.5.5
- **セキュリティ**: Spring Security 6 with OAuth2 Resource Server
- **依存性注入**: Lombok
- **ビルドツール**: Gradle
- **開発環境**: Docker & Docker Compose
- **テスト**: JUnit 5, Mockito, Testcontainers, Spring Security Test

## アーキテクチャ
レイヤーアーキテクチャを採用:
- Controller → Service → Repository → Entity
- DTO ← Mapper (MapStruct) で変換

## VPS2台構成での役割
- **VPS1 (vsv-crystal)**: 認証プロバイダー (Keycloak) + Docker Registry
- **VPS2 (vsv-emerald)**: アプリケーション本体 (my-books-api)
- **ネットワーク**: vsv-emerald-network で内部通信
- **アクセス**: BFF経由のみ（本番環境ではポート公開なし）

## 出力JAR名
- `my-books.jar`（build.gradleで設定済み）

## 主要機能
1. ユーザー認証（Keycloak統合、OAuth 2.0 / OpenID Connect）
2. 書籍管理（詳細情報、章・ページコンテンツ）
3. レビューシステム
4. お気に入り機能
5. ブックマーク機能
6. ジャンル管理
7. 管理者機能（@PreAuthorize("hasRole('ADMIN')")）

## 認証設計
- **認証方式**: Keycloak (OAuth 2.0 / OpenID Connect)
- **ユーザーID**: Keycloak UUID (String型)
- **パスワード管理**: Keycloak側で管理
- **Role管理**: Keycloak側で管理（Realm Role、Client Role）
- **トークン**: JWT (Access Token)、BFF側でRefresh Token管理
- **完全ステートレス**: JWTベース、セッション不要
