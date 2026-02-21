# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 重要

基本的なやりとりは日本語で行ってください。

## Claude Code作業ルール

**コード修正前の確認必須**
- ファイルの修正・変更を行う前に、必ずユーザーに修正内容を提示して許可を取る
- 勝手にコードを変更してはいけない
- 修正案を説明し、ユーザーの承認を得てから実行する

## プロジェクト概要

Spring Boot 3.3.5 + Java 21による書籍管理REST APIバックエンド。Keycloak認証統合、BFF経由アーキテクチャ、フリーミアム戦略に対応した設計。

**技術スタック**: Spring Boot 3.3.5、Java 21、MySQL 8.0、Keycloak (OAuth2/OIDC)、MapStruct 1.5.5、Testcontainers

## よく使うコマンド

```bash
# ビルドとテスト
./gradlew build                    # プロジェクトのビルド
./gradlew test                     # 全テスト実行
./gradlew clean build              # クリーンビルド

# 単体テストのみ（Docker不要）
./gradlew test --tests "*ControllerTest"
./gradlew test --tests "*ServiceTest"

# 統合テスト（Docker必須）
./gradlew test --tests "*RepositoryTest"

# アプリケーション実行
./gradlew bootRun                  # 開発環境で起動
./gradlew bootJar                  # JAR生成 (my-books.jar)

# Docker環境
docker-compose up -d                                                          # 開発環境起動
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d   # 本番環境起動
docker-compose logs -f my-books-api                                          # ログ確認
```

## アーキテクチャの重要な設計判断

### 1. レイヤーアーキテクチャ

```
Controller → Service → Repository → Entity
          ↓
        DTO ← Mapper (MapStruct)
```

- **Controller**: REST APIの責務のみ
- **Service**: ビジネスロジック（インターフェース + 実装）
- **Repository**: JPA Repository（カスタムクエリあり）
- **Mapper**: MapStruct interfaceで実装（abstract classは使用しない）

### 2. Keycloak認証統合と権限管理の設計

**重要**: ユーザー管理はKeycloakとアプリDBで責務を分離している

- **ユーザーID**: Keycloak UUID (String型) - `VARCHAR(255)`
- **認証情報管理**: Keycloak側（email, password, roles）
- **アプリ固有データ**: DB側（displayName, avatarPath）
- **JWTクレーム取得**: `JwtClaimExtractor.getUserId()` で `sub` クレームから取得
- **プロフィール情報**: `/me/profile` で email/name はJWTから、displayNameはDBから取得

**権限管理の3層防御アーキテクチャ**:
1. **SecurityConfig**: エンドポイントパターンでの粗いチェック（第1層）
2. **サービス層**: `@PreAuthorize` による厳密な権限チェック（最後の砦）
3. **コントローラー層**: 権限チェックなし（サービス層に委譲）

**エンドポイント保護**:
- **パブリック（GETのみ）**: `/books/**`, `/genres/**`
- **認証必要**: `/book-content/**` (有料コンテンツ)、その他のPOST/PUT/DELETE
- **管理者専用**: `/admin/**` (`user:manage` 権限)

**権限設計の詳細**: `docs/ROLE-DESIGN.md` を参照
- 「権限 (Role)」と「役割 (Composite Roles)」の2層構造
- バックエンド: 権限で細かい制御（例: `book:manage`, `review:delete:all`）
- フロントエンド: 役割のみ返却（例: `USER`, `ADMIN`）

### 3. 有料コンテンツの分離設計

`/book-content/**` パターンで有料コンテンツを完全分離。これによりセキュリティ設定が大幅に簡素化され、フリーミアム戦略を技術的に実現。

- **パブリック**: `GET /books/{id}` - 書籍情報の閲覧
- **有料**: `GET /book-content/books/{id}/chapters/{chapter}/pages/{page}` - 実際のコンテンツ

### 4. 2クエリ戦略によるN+1問題の完全解決

`PageableUtils.applyTwoQueryStrategy()` でソート順序を保持しながらN+1問題を解決:

1. 初回クエリ: ページング+ソートでIDリストを取得
2. 2回目クエリ: IDリストから `JOIN FETCH` で詳細データ取得
3. ソート順序復元: `restoreSortOrder()` でIDリスト順序を保持

リポジトリ側では `findAllByIdInWithRelations()` パターンを実装。

### 5. MapStruct + Lombok の依存関係順序

**build.gradle で順序が重要**:

```gradle
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
```

この順序でないとコンパイルエラーが発生する。

**Mapperの実装パターン**:
- 全マッパーは `interface` で実装（abstract classは使用しない）
- 依存マッパーは `uses` パラメータで注入（`@Autowired` は使用しない）
- 例: `@Mapper(componentModel = "spring", uses = {BookMapper.class})`

### 6. テスト構造

**リポジトリテスト** (Testcontainers + MySQL 8.0):
- `@DataJpaTest` + `@Testcontainers`
- Docker環境必須
- 2クエリ戦略、論理削除、集計クエリを検証

**コントローラーテスト** (MockMvc + Spring Security Test):
- `@WebMvcTest` + `@Import(SecurityConfig.class)`
- `.with(jwt())` でJWT認証をモック
- 401/403/404の網羅的なテスト

**サービステスト** (Mockito):
- ビジネスロジックの単体テスト

### 7. ページネーション実装

- **API**: 1ベースページング
- **内部**: JPA Pageableは0ベース
- **変換**: `PageableUtils.of()` で統一
- **ソート制限**: 許可されたフィールドのみ（`BOOK_ALLOWED_FIELDS` 等）

### 8. 論理削除パターン

- **基底クラス**: `EntityBase` に `isDeleted` フィールド
- **クエリパターン**: `findByIsDeletedFalse()` を使用
- **自動タイムスタンプ**: `@PrePersist`, `@PreUpdate` で管理

## 重要な設定ファイル

### application.yml

- IdP設定: `IDP_ISSUER_URI` のみ（JWK Set URIは自動検出）
- 環境変数による動的設定対応（開発/本番の切り替え）
- ページネーション設定: `app.pagination.*`

### SecurityConfig.java

- Spring Security + OAuth2 Resource Server統合
- エンドポイント認可設定: `hasAuthority()` でロールチェック
- JWTロール抽出: `realm_access.roles` からプレフィックスなしで取得
- 複数権限対応: `hasAnyAuthority()` で OR 条件（例: レビュー削除）

### JwtClaimExtractor.java

完全ステートレスなJWTクレーム抽出ユーティリティ:

**ユーザー情報取得**:
- `getUserId()`: `sub` クレームからKeycloak UUID取得
- `getEmail()`: `email` クレーム取得
- `getUsername()`: `preferred_username` → `name` → `given_name` の優先順フォールバック
- `getFamilyName()`: `family_name` クレーム取得
- `getGivenName()`: `given_name` クレーム取得

**権限・ロール取得**:
- `getRoles()`: `ROLE_` プレフィックスのロールのみ抽出（フロントエンド用）
- `hasAuthority(String role)`: 指定したロールを保持しているかチェック

### Docker環境

**docker-compose.yml** (開発):
- MySQL 8.0 + アプリケーション環境
- `sleep infinity` でコンテナ起動状態維持
- `vsv-emerald-network` で外部ネットワーク連携

**docker-compose.prod.yml** (本番):
- ポート公開なし（BFF経由アクセス）
- データベースは `127.0.0.1:3306` のみ（管理ツール用）
- リソース制限: CPU 2コア、メモリ 2GB

## VPS2台構成アーキテクチャ

```
VPS1 (vsv-crystal): Keycloak認証 + Docker Registry
VPS2 (vsv-emerald): Nginx → BFF → my-books-api → MySQL
                    (vsv-emerald-network経由で内部通信)
```

本番環境ではポート公開せず、BFF経由でのみアクセス可能。

## 環境変数

**.env.example** から `.env` をコピーして設定:

- `IDP_ISSUER_URI`: Identity Provider Issuer URI
- `DB_URL`, `DB_USER`, `DB_PASSWORD`: データベース接続
- `SPRING_JPA_SHOW_SQL`: 開発=true、本番=false
- `SERVER_ERROR_INCLUDE_MESSAGE`: 開発=always、本番=never
- `LOG_LEVEL`: 開発=DEBUG、本番=INFO

## 出力成果物

- **JARファイル名**: `my-books.jar` (build.gradle で設定)
- **ポート**: 8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
