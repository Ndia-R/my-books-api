# My Books Backend - プロジェクト概要

## プロジェクトの目的
Spring Boot 3.3.5とJava 17で構築された書籍管理REST APIシステム。ユーザー認証、書籍管理、レビュー、お気に入り、ブックマーク、章ページ機能を提供する包括的な書籍管理システム。

## 主要技術スタック
- **フレームワーク**: Spring Boot 3.3.5
- **Java**: 17
- **データベース**: MySQL 8.0 (JPA/Hibernate)
- **認証**: JWT トークンベース認証（Access Token + Refresh Token）
- **ドキュメント**: OpenAPI 3 (Swagger UI)
- **マッピング**: MapStruct 1.5.5
- **セキュリティ**: Spring Security 6
- **依存性注入**: Lombok
- **JWT**: Auth0 Java JWT 4.4.0
- **ビルドツール**: Gradle
- **開発環境**: Docker & Docker Compose

## アーキテクチャ
レイヤーアーキテクチャを採用:
- Controller → Service → Repository → Entity
- DTO ← Mapper で変換

## 出力JAR名
- `my-books.jar`（build.gradleで設定済み）

## 主要機能
1. ユーザー認証（JWT + Refresh Token）
2. 書籍管理（詳細情報、章・ページコンテンツ）
3. レビューシステム
4. お気に入り機能
5. ブックマーク機能
6. ジャンル管理
7. 管理者機能