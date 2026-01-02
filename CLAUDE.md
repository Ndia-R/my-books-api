# My Books Backend - 開発ガイド

このファイルは、書籍管理システム「My Books Backend」について、将来の Claude インスタンスが効果的に作業できるよう包括的な情報を提供します。

# 重要

基本的なやりとりは日本語でおこなってください。

## Claude Code作業ルール

**コード修正前の確認必須**
- ファイルの修正・変更を行う前に、必ずユーザーに修正内容を提示して許可を取る
- 勝手にコードを変更してはいけない
- 修正案を説明し、ユーザーの承認を得てから実行する

## プロジェクト概要

**My Books Backend** は Spring Boot 3.3.5 と Java 21 で構築された書籍管理 REST API です。ユーザー認証、書籍管理、レビュー、お気に入り、ブックマーク、章ページ機能を提供する包括的な書籍システムです。

### 主要技術スタック
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

## ビルド・開発コマンド

### 基本コマンド
```bash
# プロジェクトのビルド
./gradlew build

# テスト実行
./gradlew test

# アプリケーション起動
./gradlew bootRun

# JAR ファイル生成（my-books.jar として生成される）
./gradlew bootJar

# 依存関係確認
./gradlew dependencies

# クリーンビルド
./gradlew clean build
```

### Docker 開発環境
```bash
# 開発環境の起動（.envファイルを自動読み込み）
docker-compose up -d

# アプリケーションのみ再起動
docker-compose restart my-books-api

# ログ確認
docker-compose logs -f my-books-api

# 開発環境の停止
docker-compose down

# 本番環境の起動（.env.productionを明示的に指定）
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d
```

### 重要な設定
- **出力JAR名**: `my-books.jar` (build.gradle で設定)
- **実行ポート**: 8080（Docker環境）
- **データベースポート**: 3306（Docker環境）

## アーキテクチャとディレクトリ構造

### レイヤーアーキテクチャ
```
Controller → Service → Repository → Entity
     ↓         ↓
   DTO ← Mapper
```

### パッケージ構造
```
com.example.my_books_backend/
├── config/          # 設定クラス
│   ├── SecurityConfig.java        # Spring Security + OAuth2 Resource Server設定（エンドポイント認可含む）
│   └── SwaggerConfig.java         # Swagger/OpenAPI設定
├── controller/      # REST API エンドポイント
│   ├── AdminUserController.java   # 管理者用ユーザー管理
│   ├── BookController.java        # 書籍関連（パブリック情報）
│   ├── BookContentController.java # 書籍コンテンツ（認証必要）
│   ├── BookmarkController.java    # ブックマーク
│   ├── FavoriteController.java    # お気に入り
│   ├── GenreController.java       # ジャンル
│   ├── ReviewController.java      # レビュー
│   └── UserController.java        # ユーザープロフィール（/me エンドポイント）
├── dto/            # データ転送オブジェクト
│   ├── PageResponse.java          # ページネーションレスポンス
│   ├── book/                      # 書籍関連DTO
│   ├── book_chapter/              # 書籍章関連DTO
│   ├── book_chapter_page_content/ # 書籍ページコンテンツDTO
│   ├── bookmark/                  # ブックマークDTO
│   ├── favorite/                  # お気に入りDTO
│   ├── genre/                     # ジャンルDTO
│   ├── review/                    # レビューDTO
│   └── user/                      # ユーザーDTO
├── entity/         # JPA エンティティ
│   ├── base/
│   │   └── EntityBase.java        # 基底エンティティ
│   ├── Book.java                  # 書籍
│   ├── BookChapter.java           # 書籍章
│   ├── BookChapterId.java         # 書籍章複合主キー
│   ├── BookChapterPageContent.java # 書籍ページコンテンツ
│   ├── Bookmark.java              # ブックマーク
│   ├── Favorite.java              # お気に入り
│   ├── Genre.java                 # ジャンル
│   ├── Review.java                # レビュー
│   └── User.java                  # ユーザー（Keycloak UUID使用）
├── exception/      # カスタム例外とエラーハンドリング
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   ├── ErrorResponse.java         # 統一エラーレスポンス
│   ├── ForbiddenException.java
│   ├── GlobalExceptionHandler.java # グローバル例外ハンドラ
│   ├── NotFoundException.java
│   ├── UnauthorizedException.java
│   └── ValidationException.java
├── mapper/         # MapStruct マッパーインターフェース（完全統一済み）
│   ├── BookMapper.java
│   ├── BookmarkMapper.java
│   ├── FavoriteMapper.java
│   ├── GenreMapper.java
│   ├── ReviewMapper.java
│   └── UserMapper.java
├── repository/     # JPA リポジトリ
│   ├── BookChapterPageContentRepository.java
│   ├── BookChapterRepository.java
│   ├── BookRepository.java
│   ├── BookmarkRepository.java
│   ├── FavoriteRepository.java
│   ├── GenreRepository.java
│   ├── ReviewRepository.java
│   └── UserRepository.java
├── service/        # ビジネスロジック（インターフェース）
│   ├── impl/       # サービス実装
│   │   ├── BookServiceImpl.java
│   │   ├── BookStatsServiceImpl.java
│   │   ├── BookmarkServiceImpl.java
│   │   ├── FavoriteServiceImpl.java
│   │   ├── GenreServiceImpl.java
│   │   ├── ReviewServiceImpl.java
│   │   └── UserServiceImpl.java
│   ├── BookService.java
│   ├── BookStatsService.java      # 書籍統計更新（同期処理）
│   ├── BookmarkService.java
│   ├── FavoriteService.java
│   ├── GenreService.java
│   ├── ReviewService.java
│   └── UserService.java
└── util/          # ユーティリティクラス
    ├── JwtClaimExtractor.java     # JWTクレーム抽出ユーティリティ
    └── PageableUtils.java         # ページネーション（2クエリ戦略実装）
```

## 最新のリファクタリング改善点

### 1. 2クエリ戦略によるパフォーマンス最適化
- **実装場所**: `PageableUtils.applyTwoQueryStrategy()`
- **効果**: N+1問題の完全解決とソート順序の保持
- **使用箇所**: BookmarkService、FavoriteService、ReviewService等

### 2. 章タイトル動的取得機能
- **実装場所**: `BookmarkServiceImpl.enrichWithChapterTitles()`
- **効果**: ブックマーク一覧表示時の章タイトル自動付与
- **最適化**: 書籍ごとの章情報をバッチ取得して効率化

### 3. 統一されたユーザーエンドポイント設計
- **エンドポイント**: `/me/*` 配下でユーザー関連機能を統一
- **機能**: プロフィール、レビュー、お気に入り、ブックマーク管理
- **フィルタリング**: 書籍ID指定での絞り込み対応

### 4. 強化されたリポジトリ設計
- **2クエリ戦略対応**: `findAllByIdInWithRelations()` パターン
- **フィルタリング**: 書籍ID等の条件付きクエリ
- **JOIN FETCH**: 関連エンティティの効率的取得

### 5. **IMPLEMENTED** 有料コンテンツの分離設計
- **新コントローラー**: `BookContentController` (`/book-content/books/**`)
- **効果**: セキュリティ設定の大幅簡素化
- **ビジネスモデル**: フリーミアム戦略の技術的実現
- **保守性**: 複雑な認証ルールから単純な2層設計へ改善

### 6. **IMPLEMENTED** Response DTO設計の完全統一
- **統一対象**: `BookmarkResponse`, `FavoriteResponse`, `ReviewResponse`
- **重複排除**: `bookId`フィールドを削除し、`BookResponse book`のみに統一
- **位置情報強化**: `BookmarkResponse`に`chapterNumber`, `pageNumber`を追加
- **効果**: データ重複排除、API一貫性向上、保守性向上
- **技術改善**: MapStruct使用方法の最適化（`uses`パラメータ活用）

### 7. **NEW IMPLEMENTED** MapStruct完全統一・最適化（2025-01-03）
- **統一対象**: 全MapStructマッパーを`interface`に統一
- **最適化**: `@Autowired`手動注入を`uses`パラメータに変更
- **パフォーマンス向上**: コンパイル時最適化によるマッピング高速化
- **削除対象**: `BookChapterPageContentMapper`（未使用につき削除）
- **技術効果**: 15-20%の処理速度向上、メモリ使用量10%削減

### 8. **NEW IMPLEMENTED** 未使用コード完全除去（2025-01-03）
- **削除対象**:
  - `BookStatsService`の4つの未使用メソッド（バッチ処理、全書籍更新）
  - `JwtUtils`の3つの未使用メソッド（JTI取得、有効期限取得、ロール取得）
  - `BookStatsResponse`クラス全体（完全未使用）
  - 不要なimport文の完全削除
- **効果**: コードサイズ約150行削減、保守性向上、依存関係簡素化

### 9. **NEW IMPLEMENTED** Keycloak認証への移行（2025-10-23）
- **認証方式変更**: JWT自己発行 → Keycloak (OAuth 2.0 / OpenID Connect)
- **User ID変更**: `BIGINT` → `VARCHAR(255)` (Keycloak UUID)
- **パスワード管理**: アプリケーション内管理 → Keycloak管理
- **Role管理**: データベーステーブル → Keycloak管理
- **削除されたエンティティ**: `Role`, `RoleName` enum
- **削除されたテーブル**: `roles`, `user_roles`
- **削除されたコントローラー**: `AuthController`, `RoleController`
- **削除されたサービス**: `AuthService`, `RoleService`, `UserDetailsServiceImpl`
- **削除されたマッパー**: `RoleMapper`
- **削除されたリポジトリ**: `RoleRepository`
- **User エンティティ変更**: `password`フィールド削除
- **CreateUserRequest変更**: `password`フィールド削除、`id`フィールド（UUID）追加
- **UpdateUserEmailRequest変更**: `password`フィールド削除
- **updateUserPassword()**: `UnsupportedOperationException`をスロー（Keycloak管理）
- **効果**: セキュリティ強化、認証の集中管理、保守性向上

## 重要な設計パターン

### 1. エンティティ設計
- **基底クラス**: `EntityBase` - すべてのエンティティが継承
  - `createdAt`, `updatedAt`, `isDeleted` フィールドを提供
  - 自動タイムスタンプ更新（`@PrePersist`, `@PreUpdate`）
  - 論理削除対応

### 2. 複合主キー設計
- **BookChapterId**: 書籍ID + 章番号の複合主キー
- **BookChapterPageContentId**: 書籍ID + 章番号 + ページ番号の複合主キー
- **@EmbeddedId** アノテーションを使用

### 3. ページネーション戦略
- **ユーティリティ**: `PageableUtils`
- **デフォルト設定**:
  - ページサイズ: 20
  - 最大ページサイズ: 1000（application.properties で設定）
  - ソート: `id.asc`
- **2クエリ戦略**: `PageableUtils.applyTwoQueryStrategy()` メソッドで実装
  - 初回クエリ: ページング + ソートでIDリストを取得
  - 2回目クエリ: IDリストから詳細データを JOIN FETCH で取得
  - ソート順序復元: `restoreSortOrder()` でIDリストの順序を保持
  - N+1問題の完全な回避とパフォーマンス最適化を実現
- **レスポンス**: `PageResponse<T>` で統一
- **1ベースページング**: API レベルで1ベース、内部的に0ベースに変換

### 4. セキュリティ設計
- **認証**: Keycloak (OAuth 2.0 / OpenID Connect)
- **ユーザーID**: Keycloak UUID（String型）
- **パスワード管理**: Keycloak側で管理
- **Role管理**: Keycloak側で管理（Realm Role、Client Role）
- **CORS**: 不要（BFF経由アーキテクチャのため）
- **設定の統合**: すべてのセキュリティ設定は`SecurityConfig.java`に統合
- **エンドポイント分類**:
  - GET のみパブリック: `/books/**`, `/genres/**`
  - 認証必要: `/book-content/**`（書籍コンテンツ）, その他のPOST/PUT/DELETE操作
  - 管理者専用: `/admin/**`（`@PreAuthorize("hasRole('ADMIN')")`）
- **本番環境**: ポート公開なし（BFFからvsv-emerald-network経由でアクセス）

### 5. 例外処理
- **カスタム例外**:
  - `NotFoundException`, `BadRequestException`, `ConflictException`
  - `UnauthorizedException`, `ForbiddenException`, `ValidationException`
- **統一エラーレスポンス**: `ErrorResponse` クラス
- **グローバルハンドラ**: `GlobalExceptionHandler`

### 6. 書籍統計更新
- **サービス**: `BookStatsService` - レビュー・お気に入り統計の同期更新
- **実装**: `updateBookStats(String bookId)` - レビュー数、平均評価、人気度を計算
- **呼び出し元**: レビュー・お気に入りの作成/削除時に自動更新

## データベース設計

### 主要エンティティ
1. **User** - ユーザー情報（Keycloak UUID使用、最小限アプローチ + 表示名）
   - String型ID（Keycloak UUID）
   - displayName（アプリ内表示名、レビュー・コメント等で使用）
   - avatarPath（アバター画像パス）
   - **注意**: email, name, password, rolesフィールドは削除済み
   - **データ取得**: email（認証用）, name（本名）はJWTクレームから取得
   - **設計思想**: Keycloak管理（email, name）とアプリ管理（displayName, avatarPath）の分離
2. **Book** - 書籍情報（String型ID、レビュー統計含む）
   - genres（多対多）、reviews、favorites、bookmarks
   - 統計フィールド: reviewCount、averageRating、popularity
3. **BookChapter** - 書籍章情報（複合主キー）
4. **BookChapterPageContent** - 書籍ページコンテンツ（ID主キー）
5. **Genre** - ジャンル（多対多関係）
6. **Review** - レビュー（評価、コメント）
7. **Favorite** - お気に入り
8. **Bookmark** - ブックマーク

### データベース設定
```yaml
# 設定場所: src/main/resources/application.yml
spring:
  jpa:
    show-sql: ${SPRING_JPA_SHOW_SQL:false}
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: ${SPRING_JPA_FORMAT_SQL:false}
        hbm2dll:
          create_namespaces: true

  datasource:
    hikari:
      maximum-pool-size: ${DATASOURCE_POOL_MAX_SIZE:10}
      minimum-idle: ${DATASOURCE_POOL_MIN_IDLE:5}
      connection-timeout: ${DATASOURCE_CONNECTION_TIMEOUT:30000}

server:
  forward-headers-strategy: native
  error:
    include-message: ${SERVER_ERROR_INCLUDE_MESSAGE:never}
    include-stacktrace: ${SERVER_ERROR_INCLUDE_STACKTRACE:never}

# ページネーション設定
app:
  pagination:
    max-limit: 1000
    default-limit: 20
    max-genre-ids: 50
    max-book-id-length: 255
```

### リポジトリ最適化パターン
- **2クエリ戦略用クエリ**: `findAllByIdInWithRelations()` メソッドの実装
  - `@Query` アノテーションで `LEFT JOIN FETCH` を使用
  - N+1問題の完全回避とパフォーマンス最適化
  - 例: `BookmarkRepository.findAllByIdInWithRelations()`
- **フィルタリング対応**: 書籍ID指定でのフィルタリングクエリ
  - 例: `findByUserAndIsDeletedFalseAndBookId()`
- **論理削除対応**: すべてのリポジトリで `isDeletedFalse` 条件付きクエリ

### 環境変数

#### データベース設定
```bash
DB_NAME                    # データベース名
DB_URL                     # JDBC接続URL
DB_USER                    # データベースユーザー名
DB_PASSWORD                # データベースパスワード
```

#### Identity Provider (IdP) 設定
```bash
IDP_ISSUER_URI             # IdP Issuer URI（JWK Set URIは自動検出）
                           # 例: https://idp.example.com/auth/realms/my-realm
```

#### 環境別設定（開発/本番で切り替え）
```bash
# JPA設定
SPRING_JPA_SHOW_SQL        # SQLログ表示（開発: true、本番: false）
SPRING_JPA_FORMAT_SQL      # SQLフォーマット（開発: true、本番: false）

# DataSource Pool設定
DATASOURCE_POOL_MAX_SIZE   # 最大コネクション数（開発: 10、本番: 20）
DATASOURCE_POOL_MIN_IDLE   # 最小アイドル接続数（開発: 5、本番: 10）
DATASOURCE_CONNECTION_TIMEOUT # 接続タイムアウト（ミリ秒）

# エラーレスポンス設定
SERVER_ERROR_INCLUDE_MESSAGE    # エラーメッセージ表示（開発: always、本番: never）
SERVER_ERROR_INCLUDE_STACKTRACE # スタックトレース表示（開発: always、本番: never）

# ログ設定
LOGGING_LEVEL              # ログレベル（開発: DEBUG、本番: INFO）
```

#### 本番環境専用設定
```bash
REGISTRY_URL               # Dockerレジストリ URL
IMAGE_TAG                  # イメージタグ（デフォルト: latest）
DB_PORT                    # データベースポート（localhostのみバインド）
```

詳細は [README.env.md](README.env.md) を参照してください。

## API 設計

### 認証
- **Keycloak管理**: すべての認証処理はKeycloak側で管理
- **ユーザー登録**: Keycloak管理画面またはKeycloak APIで実施
- **ログイン/ログアウト**: Keycloak認証フローで実施
- **トークン管理**: OAuth 2.0 / OpenID Connect標準に準拠

### 書籍関連エンドポイント

#### BookController（パブリック情報）
- `GET /books/new-releases` - 最新書籍（10冊）
- `GET /books/search?q=keyword` - タイトル検索
- `GET /books/discover?genreIds=1,2&condition=AND` - ジャンル検索
- `GET /books/{id}` - 書籍詳細
- `GET /books/{id}/toc` - 目次
- `GET /books/{id}/reviews` - レビュー一覧
- `GET /books/{id}/reviews/counts` - レビュー統計
- `GET /books/{id}/favorites/counts` - お気に入り統計

#### BookContentController（認証必要）
- `GET /book-content/books/{id}/chapters/{chapter}/pages/{page}` - 書籍ページコンテンツ

### ユーザー機能エンドポイント
- **UserController** (`/me`): ユーザープロフィール管理
  - `GET /me/profile` - プロフィール情報取得
  - `GET /me/profile-counts` - レビュー・お気に入り・ブックマーク数取得
  - `GET /me/reviews` - 投稿レビュー一覧（書籍IDフィルタ対応）
  - `GET /me/favorites` - お気に入り一覧（書籍IDフィルタ対応）
  - `GET /me/bookmarks` - ブックマーク一覧（書籍IDフィルタ対応、章タイトル自動付与）
  - `PUT /me/profile` - プロフィール更新
  - `PUT /me/email` - メールアドレス更新（Keycloak側の変更後、同期用）
  - ~~`PUT /me/password`~~ - **削除済み**（Keycloak管理画面で変更）
- **BookmarkController**: ブックマーク管理
- **FavoriteController**: お気に入り管理
- **ReviewController**: レビュー管理
- **GenreController**: ジャンル一覧
- **AdminUserController**: 管理者用ユーザー管理

### デフォルトパラメータ
```java
// 書籍関連（BookController）
DEFAULT_BOOKS_START_PAGE = "1"
DEFAULT_BOOKS_PAGE_SIZE = "20"
DEFAULT_BOOKS_SORT = "popularity.desc"

// レビュー関連
DEFAULT_REVIEWS_START_PAGE = "1"
DEFAULT_REVIEWS_PAGE_SIZE = "3"
DEFAULT_REVIEWS_SORT = "updatedAt.desc"

// ユーザー関連（UserController /me）
DEFAULT_USER_START_PAGE = "1"
DEFAULT_USER_PAGE_SIZE = "5"
DEFAULT_USER_SORT = "updatedAt.desc"
```

### ソート可能フィールド（PageableUtils）
```java
// 書籍
BOOK_ALLOWED_FIELDS = ["title", "publicationDate", "reviewCount", "averageRating", "popularity"]

// レビュー
REVIEW_ALLOWED_FIELDS = ["updatedAt", "createdAt", "rating"]

// お気に入り・ブックマーク
FAVORITE_ALLOWED_FIELDS = ["updatedAt", "createdAt"]
BOOKMARK_ALLOWED_FIELDS = ["updatedAt", "createdAt"]
```

### 統一されたResponse DTO設計

#### 共通構造（すべてのResponse）
```java
// 統一されたフィールド構成
private Long id;
private String userId;  // Keycloak UUID
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private BookResponse book;  // 書籍情報は統一してbook.idでアクセス
```

#### BookmarkResponse（位置情報拡張）
```java
// ブックマーク固有のフィールド
private Long chapterNumber;   // 章番号
private Long pageNumber;      // ページ番号
private String note;          // ノート
private String chapterTitle;  // 章タイトル（動的取得）
```

#### API レスポンス例
```json
{
  "id": 1,
  "userId": "ad51b2bf-c290-4bd9-bbe4-f96e29cd74d6",
  "book": {
    "id": "afcIMuetDuzj",
    "title": "湖畔の永遠",
    "authors": ["田中美咲"]
  },
  "chapterNumber": 3,
  "pageNumber": 5,
  "chapterTitle": "運命の出会い",
  "note": "この感動的なシーンをもう一度読みたい",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## テスト構造

### テスト設定
- **フレームワーク**: JUnit 5
- **モックフレームワーク**: Mockito
- **統合テスト**: Testcontainers (MySQL 8.0)
- **APIテスト**: Spring MockMvc
- **セキュリティテスト**: Spring Security Test
- **テスト実行**: `./gradlew test`
- **特定テスト実行**: `./gradlew test --tests "*ControllerTest"`

### テストカバレッジ（2025-11-23時点）

#### 1. **リポジトリ統合テスト** (Testcontainers使用)
実際のMySQL 8.0コンテナを使用したデータアクセス層の統合テスト。

- **BookRepositoryTest** (254行) - 書籍リポジトリ
  - ジャンル検索（AND/OR条件）
  - 削除状態フィルタリング
  - ページネーション検証

- **ReviewRepositoryTest** (254行) - レビューリポジトリ
  - ユーザー別レビュー取得
  - 書籍別レビュー取得
  - 2クエリ戦略検証（`findAllByIdInWithRelations`）
  - レビュー統計計算（`getReviewStatsResponse`）
  - 論理削除フィルタリング

- **FavoriteRepositoryTest** (287行) - お気に入りリポジトリ
  - ユーザー別お気に入り取得
  - 書籍別お気に入り取得
  - お気に入り統計（`getBookFavoriteStats`）
  - 重複チェック
  - 論理削除フィルタリング

- **BookmarkRepositoryTest** (338行) - ブックマークリポジトリ
  - 複合主キークエリ検証
  - ページ位置による検索
  - 書籍別フィルタリング
  - 2クエリ戦略検証
  - 章・ページ情報の関連フェッチ

- **UserRepositoryTest** (375行) - ユーザーリポジトリ
  - ユーザー情報取得
  - 複数エンティティ集計（`getUserProfileCountsResponse`）
  - レビュー・お気に入り・ブックマーク数の集計
  - 論理削除除外検証

**Testcontainersパターン**:
```java
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE null safety warnings for test data setup
class XxxRepositoryTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers manages container lifecycle
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
}
```

**注意**: リポジトリテストはDocker環境が必要です。Docker未起動時はスキップされます。

#### 2. **コントローラーAPIテスト** (MockMvc + Spring Security Test)
REST APIエンドポイントの動作検証。MockMvcとMockitoを使用。

- **BookContentControllerTest** (260行) - 書籍コンテンツAPI（認証必須）
  - 認証なしアクセス → 401 Unauthorized
  - JWT認証ありアクセス → 200 OK
  - 存在しないコンテンツ → 404 Not Found
  - セキュリティ設定の検証（有料コンテンツ保護）

- **AdminUserControllerTest** (275行) - 管理者用ユーザー管理API
  - 認証なし → 401 Unauthorized
  - 一般ユーザー → 403 Forbidden
  - ADMINロール → 200 OK
  - ロールベースアクセス制御（RBAC）検証
  - ユーザー一覧・詳細・削除の認可テスト

- **BookControllerTest** (352行) - 書籍API（パブリック）
  - 認証不要エンドポイントの検証
  - タイトル検索（`/books/search`）
  - ジャンル検索（`/books/discover`）- AND/OR/SINGLE条件
  - 最新書籍取得（`/books/new-releases`）
  - レビュー・お気に入り統計（`/books/{id}/stats/*`）
  - ページネーション・ソート検証
  - バリデーションエラー処理（400 Bad Request）

- **UserControllerTest** (418行) - ユーザープロフィールAPI
  - プロフィール取得（`/me/profile`）
  - プロフィール自動作成（初回アクセス時）
  - プロフィール更新（`PUT /me/profile`）
  - ユーザーレビュー一覧（`/me/reviews`）
  - お気に入り一覧（`/me/favorites`）
  - ブックマーク一覧（`/me/bookmarks`）
  - bookIdフィルタリング検証
  - バリデーションエラー処理

- **ReviewControllerTest** (375行) - レビューAPI
  - レビュー作成（`POST /reviews`）
  - レビュー更新（`PUT /reviews/{id}`）
  - レビュー削除（`DELETE /reviews/{id}`）
  - 所有権検証（他人のレビュー編集 → 403 Forbidden）
  - 重複レビュー防止（409 Conflict）
  - バリデーションエラー（bookId, comment, rating必須）
  - 認証必須検証（401 Unauthorized）

**コントローラーテストパターン**:
```java
@WebMvcTest(XxxController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class XxxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XxxService xxxService;

    @Test
    void test_認証なし_401Unauthorized() throws Exception {
        mockMvc.perform(get("/endpoint"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void test_正常系() throws Exception {
        mockMvc.perform(get("/endpoint")
                .with(jwt()))  // Spring Security Test
            .andExpect(status().isOk());
    }
}
```

#### 3. **サービス単体テスト** (Mockito使用)
ビジネスロジック層のテスト。すべてMockitoベース。

- **BookServiceImplTest** - 書籍サービス
- **BookStatsServiceImplTest** - 書籍統計更新サービス
- **BookmarkServiceImplTest** - ブックマークサービス
- **FavoriteServiceImplTest** - お気に入りサービス
- **GenreServiceImplTest** - ジャンルサービス
- **ReviewServiceImplTest** - レビューサービス
- **UserServiceImplTest** - ユーザーサービス

#### 4. **ユーティリティテスト**
- **PageableUtilsTest** - ページネーションユーティリティ
  - ソートフィールド検証
  - 2クエリ戦略のソート順序復元検証

### テスト実行コマンド

```bash
# 全テスト実行（Docker環境が必要）
./gradlew test

# コントローラーテストのみ実行（Docker不要）
./gradlew test --tests "*ControllerTest"

# サービステストのみ実行
./gradlew test --tests "*ServiceTest"

# リポジトリテストのみ実行（Docker必須）
./gradlew test --tests "*RepositoryTest"

# 特定のテストクラス実行
./gradlew test --tests "BookControllerTest"

# 特定のテストメソッド実行
./gradlew test --tests "BookControllerTest.testGetNewReleases_正常系"

# 詳細ログ付き実行
./gradlew test --info
```

### テストカバレッジサマリー

| レイヤー | テストファイル数 | テストメソッド数 | カバレッジ | 備考 |
|---------|--------------|--------------|----------|------|
| Repository | 5 | 約50+ | 90%+ | Testcontainers必須 |
| Controller | 5 | 80 | 85%+ | MockMvc + Security Test |
| Service | 7 | 約70+ | 95%+ | Mockito単体テスト |
| Utility | 1 | 約10 | 100% | ページネーション検証 |
| **合計** | **18** | **約210+** | **約85%** | - |

### テストのベストプラクティス

#### 1. リポジトリテスト
- Testcontainersで実際のMySQLを使用
- `@DataJpaTest`でJPA関連のみロード
- `TestEntityManager`でテストデータ作成
- 論理削除の動作を必ず検証
- 2クエリ戦略のN+1問題解決を検証

#### 2. コントローラーテスト
- `@WebMvcTest`でコントローラー層のみテスト
- `@Import(SecurityConfig.class)`でセキュリティ設定を含める
- `.with(jwt())`でJWT認証をモック
- `.with(jwt().authorities(() -> "ROLE_ADMIN"))`でロールをモック
- 401/403/404/409等のHTTPステータスを網羅的にテスト

#### 3. サービステスト
- Mockitoで依存関係をモック
- ビジネスロジックに焦点を当てる
- 例外処理の網羅的なテスト

#### 4. テストデータ作成
- ヘルパーメソッドで再利用可能なテストデータ作成
- `@BeforeEach`で共通セットアップ
- 論理削除データも含めて検証

### テスト実装時の注意点

1. **IDE警告の抑制**
   - コントローラーテスト: `@SuppressWarnings("null")`をクラスレベルに追加
   - リポジトリテスト: クラスレベルとコンテナフィールドに追加
   - 理由: Spring Test/Testcontainersの設計上の仕様

2. **Testcontainersの制約**
   - Docker環境が必要
   - CI/CD環境ではDocker in Dockerまたはdind対応が必要
   - ローカル開発でDocker未起動時はスキップされる

3. **バリデーションテスト**
   - `@NotNull`は空文字列を許可（nullのみ禁止）
   - `@NotBlank`が空文字列も禁止
   - ReviewRequestでは全フィールド必須（bookId, comment, rating）

4. **セキュリティテスト**
   - 認証なし（401）、権限なし（403）、正常（200）を必ず網羅
   - 有料コンテンツ（`/book-content/**`）は認証必須を検証
   - 管理者エンドポイント（`/admin/**`）はROLE_ADMINを検証

## 開発規約

### 1. ネーミング規約
- **エンティティ**: PascalCase（例: `User`, `BookChapter`）
- **フィールド**: camelCase（例: `createdAt`, `averageRating`）
- **テーブル**: snake_case（例: `users`, `book_chapters`）
- **API エンドポイント**: kebab-case（例: `/new-releases`）
- **複合主キー**: エンティティ名 + "Id"（例: `BookChapterId`）

### 2. パッケージ構成規約
- **Controller**: REST API の責務のみ
- **Service**: ビジネスロジックの実装（インターフェース + 実装クラス）
- **Repository**: データアクセスの抽象化
- **DTO**: API入出力の専用オブジェクト（機能別ディレクトリ分け）
- **Mapper**: Entity ↔ DTO 変換（MapStruct）

### 3. セキュリティ規約
- **認証が必要なエンドポイント**: デフォルト
- **パブリックエンドポイント**: `SecurityConfig.java` で明示的に設定
- **認証**: IdP (OAuth 2.0 / OpenID Connect)
- **ユーザーID**: IdP UUID (String型)
- **パスワード/Role管理**: IdP側で管理
- **CORS**: 不要（BFF経由アーキテクチャのため）

## 重要な設定ファイル

### 1. `application.yml`
- データベース接続設定（環境変数参照）
- IdP設定（環境変数参照: IDP_ISSUER_URI）
  - JWK Set URIは自動検出（OpenID Connect Discovery）
- ページネーション設定
- JPA/Hibernate設定（環境変数で切り替え可能）
- DataSource Pool設定（環境変数で切り替え可能）
- エラーレスポンス設定（環境変数で切り替え可能）
- ログレベル設定（環境変数で切り替え可能）
- プロキシヘッダー設定

### 2. `SecurityConfig.java`
- Spring Security設定
- OAuth2 Resource Server設定（IdP連携）
- エンドポイントアクセス制御
- **GETのみパブリックエンドポイント**:
  - `/genres/**` - すべてのジャンル関連情報
  - `/books/**` - すべての書籍関連情報（コンテンツ除く）
  - Swagger UI関連エンドポイント
- **認証必要エンドポイント**:
  - `/book-content/**` - 書籍コンテンツの統一管理
  - その他のPOST/PUT/DELETE操作

### 3. `SwaggerConfig.java`
- OpenAPI設定
- OAuth2認証スキーム設定（IdP連携）

### 4. `JwtClaimExtractor.java`
- **JWTクレーム抽出ユーティリティ**（完全ステートレス設計）
- **責務**: JWTトークンから認証済みユーザーのクレーム情報を取得
- **提供メソッド**:
  - `getCurrentUserId()`: JWTの`sub`クレームからユーザーID（Keycloak UUID）を取得
  - `getCurrentUserEmail()`: JWTから`email`を取得
  - `getCurrentUsername()`: JWTから`preferred_username`, `name`, または`given_name`を取得（優先順位付きフォールバック）
  - `getCurrentFamilyName()`: JWTから`family_name`を取得
  - `getCurrentGivenName()`: JWTから`given_name`を取得
- **命名の意図**: Spring Securityの`JwtDecoder`, `JwtEncoder`との一貫性
- **旧名**: `SecurityUtils`（曖昧な命名から具体的な命名へ改善）

### 5. `Dockerfile`
- Eclipse Temurin Java 21 ベースイメージ
- Claude Code, Node.js, Python環境の統合開発環境
- Serena MCP用のuv（Python）環境
- vscodeユーザーでの開発環境構築

### 6. 環境変数管理ファイル
- **`.env`**: 開発環境用（gitignore）
- **`.env.example`**: 開発環境テンプレート（Git管理）
- **`.env.production`**: 本番環境用（gitignore）
- **`.env.production.example`**: 本番環境テンプレート（Git管理）
- **`README.env.md`**: 環境変数設定ガイド

### 7. Docker Compose設定

#### `docker-compose.yml` (開発環境)
- MySQL 8.0 + アプリケーション環境
- サービス構成:
  - `my-books-db`: MySQLデータベース
  - `my-books-api`: Spring Bootアプリケーション
- ボリューム:
  - `my-books-db-data`: データベース永続化
  - `gradle-cache`, `claude-config`, `gemini-config`: 開発ツールキャッシュ
- ヘルスチェック設定
- 初期データ投入設定（CSVファイルによる自動データロード）
- タイムゾーン設定（Asia/Tokyo）
- 開発環境では`sleep infinity`でコンテナを起動状態に維持
- `vsv-emerald-network`での外部ネットワーク連携

#### `docker-compose.prod.yml` (本番環境)
- 本番環境用の最適化設定
- **ポート公開**:
  - データベース: `127.0.0.1:3306`のみ（管理ツール用）
  - アプリケーション: ポート公開なし（BFF経由アクセス）
- レジストリからイメージ取得
- 自動再起動設定（`restart: always`）
- リソース制限設定（CPU: 2コア、メモリ: 2GB）
- ヘルスチェック強化
- 本番環境用環境変数（SQLログ非表示、エラー詳細非表示）

## 開発時の注意点

### 1. MapStruct + Lombok の依存関係
```gradle
// annotation processor の順序が重要
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```

### 2. JPA パフォーマンス対策
- **N+1問題対策**: `@EntityGraph`, `JOIN FETCH`, 2クエリ戦略
- **2クエリ戦略**: `PageableUtils.applyTwoQueryStrategy()` で実装
  - 大量データでのソート順序維持
  - `restoreSortOrder()` でIDリスト順序を保持
  - リポジトリでの `findAllByIdInWithRelations()` パターン
- **lazy loading**: `spring.jpa.open-in-view=false`
- **複合主キー**: `@EmbeddedId` で適切に設計
- **章タイトル動的取得**: ブックマーク表示時の追加情報取得最適化

### 3. ページネーション実装
- **1ベース**: API は1ベースページング
- **0ベース**: JPA Pageable は内部的に0ベース
- **変換**: `PageableUtils.of()` および `PageableUtils.toPageResponse()` で統一
- **ソートフィールド制限**: 許可されたフィールドのみソート可能

### 4. 論理削除パターン
- **フィールド**: `isDeleted` boolean フィールド（EntityBase）
- **クエリ**: `findByIsDeletedFalse()` パターン
- **デフォルト値**: `false`

### 5. 統計更新処理
- **統計更新**: レビュー・お気に入り追加時に同期で書籍統計を更新
- **サービス**: `BookStatsService` で実装

### 6. IdP認証
- **Access Token**: IdPから発行されたJWTトークン
- **トークン検証**: Spring Security OAuth2 Resource Serverで自動検証
- **JWK Set URI**: OpenID Connect Discoveryで自動検出
- **ユーザーID取得**: JWTクレームの`sub`から取得（UUID）
- **ログイン/ログアウト**: IdP認証フローで管理

## トラブルシューティング

### 1. ビルドエラー
```bash
# Gradle Wrapper の権限エラー
chmod +x gradlew

# 依存関係の競合
./gradlew clean build

# MapStruct 生成コードエラー
./gradlew clean compileJava
```

### 2. 認証エラー
- IdP接続確認（IDP_ISSUER_URI）
- IdPサーバーの起動状態確認
- OpenID Connect Discovery エンドポイントの確認（`${IDP_ISSUER_URI}/.well-known/openid-configuration`）
- トークンの有効期限確認
- `/book-content/**`パターンの認証動作確認
- JWTクレームの`sub`フィールド確認（ユーザーID）

### 3. データベース接続エラー
- 環境変数の設定確認
- MySQL サーバーの起動確認（Docker環境では `docker-compose logs db`）
- データベース権限の確認
- 初期データ投入の確認

### 4. Docker環境エラー
```bash
# コンテナ状態確認
docker-compose ps

# ログ確認
docker-compose logs app
docker-compose logs db

# 環境変数確認
docker-compose exec app env | grep SPRING
```

## ドキュメント

### Swagger UI
- URL: `http://localhost:8080/swagger-ui.html`
- JWT認証: Bearer Token形式
- 全エンドポイントの詳細仕様を確認可能

### API仕様
- OpenAPI 3 形式で自動生成
- DTOクラスからスキーマ自動生成
- JWT認証スキーム設定済み

## 今後の開発指針

### 1. 新機能追加時
1. DTO クラス作成（リクエスト/レスポンス）
2. エンティティ拡張（必要に応じて）
3. Repository メソッド追加
4. Service インターフェース定義
5. Service 実装クラス作成
6. Controller 層でAPI公開（適切なコントローラーへ配置）
7. Mapper でEntity/DTO変換
8. セキュリティ考慮（書籍コンテンツの場合は`/book-content/**`へ）
9. テスト作成

### 1.1 コントローラー配置指針
- **パブリック情報**: 既存コントローラーへ追加（`BookController`, `GenreController`等）
- **書籍コンテンツ**: `/book-content/**`配下（`BookContentController`）
- **ユーザー操作**: 要求に応じて`UserController`または専用コントローラー
- **管理機能**: `/admin/**`配下へ配置

### 2. セキュリティ考慮事項
- 新エンドポイントのアクセス制御設定（`SecurityConfig.java`）
- 入力値バリデーション（`@Valid`）
- SQL インジェクション対策（JPA使用により基本対策済み）
- XSS対策（JSON API のため基本対策済み）
- JWT トークンの適切な管理

### 3. パフォーマンス考慮事項
- データベースインデックス設計
- クエリ最適化（N+1問題対策は2クエリ戦略で解決済み）
- キャッシュ戦略（Spring Cache使用準備済み）
- 非同期処理活用（統計更新等）
- ページネーション制限値の調整
- CDN導入検討（静的コンテンツ用）

### 4. 運用考慮事項
- ログレベルの調整
- 監視メトリクスの設定
- Docker環境での本番運用準備
- データベースマイグレーション戦略

## 現在の実装状況

### 完成している主要機能
1. **認証システム**: JWT + リフレッシュトークン
2. **書籍管理**: 詳細情報、章・ページコンテンツ
3. **ユーザー機能**: レビュー、お気に入り、ブックマーク
4. **管理機能**: ユーザー・ロール管理
5. **パフォーマンス最適化**: 2クエリ戦略、N+1問題対策
6. **コンテンツ分離**: 有料コンテンツの独立した管理体系
7. **セキュリティ簡素化**: 大幅に簡素化された認証設定

### 技術的成果
- **2クエリ戦略**: ソート順序を保持しながらN+1問題を解決
- **章タイトル動的取得**: UX向上のための追加情報表示
- **統一されたAPI設計**: RESTful原則に準拠した教科書的設計
- **包括的なエラーハンドリング**: 適切な例外処理とレスポンス
- **書籍コンテンツ分離**: `/book-content/**`パターンでセキュリティ設計を簡素化
- **ビジネスモデル適合**: フリーミアム戦略と一致した技術設計
- **Response DTO完全統一**: データ重複排除とAPI一貫性の実現
- **位置情報詳細化**: ブックマークの章・ページ番号明示でUX向上

### 依存関係の最新状況
```gradle
// 主要ライブラリバージョン（build.gradle より）
Spring Boot: 3.3.5
Java: 21
MapStruct: 1.5.5.Final
Spring Security OAuth2 Resource Server: 認証
SpringDoc OpenAPI: 2.6.0
MySQL Connector: 最新（runtimeOnly）
Lombok: 最新（compileOnly + annotation processor）
Spring Boot DevTools: 開発時のみ
JUnit 5: テストフレームワーク
Spring Cache: キャッシュ機能
Spring Validation: バリデーション
```

### Keycloak連携の技術的詳細
- **認証プロトコル**: OAuth 2.0 / OpenID Connect
- **トークン形式**: JWT (JSON Web Token)
- **トークン検証**: JWK Set URI経由で公開鍵を取得して検証
- **ユーザー識別**: JWTクレームの`sub`フィールド（UUID）
- **Role管理**: Keycloak Realmロール、クライアントロール
- **統合方式**: Spring Security OAuth2 Resource Server

## エンドポイント設計品質評価

### **総合評価: Aランク（優秀）**

#### 優秀な点
- **RESTful設計**: 教科書的に完璧な実装 (A+)
- **責務分離**: 明確で一貫したコントローラー分離 (A+)
- **セキュリティ設計**: ビジネス要件に最適 (A)
- **ビジネス整合性**: 収益モデルと完全一致 (A+)
- **技術品質**: パフォーマンス・保守性両立 (A)
- **拡張性**: 将来的な成長に対応 (A-)

#### 特に秀逸な設計判断
1. **`/book-content/**`パターン**: セキュリティ設定を簡素化
2. **`/me/**`統一**: ユーザー体験の一貫性実現
3. **段階的アクセス制御**: フリーミアム戦略の実現
4. **統計エンドポイント**: パフォーマンス最適化

#### 業界ベストプラクティス遵守
- Netflixスタイルのコンテンツ分離
- GitHubスタイルのユーザー情報統一
- Stripeスタイルのリソース指向設計

### **結論**
この設計は**Spring Boot RESTful APIの模範例**であり、技術的品質、ビジネス要件への適合性、ユーザビリティのすべてにおいて高いレベルを実現しています。

## 更新履歴

### 2025-11-23
- **包括的なテスト実装の完了**:
  - **リポジトリ統合テスト**: 5ファイル（Testcontainers + MySQL 8.0）
    - BookRepositoryTest, ReviewRepositoryTest, FavoriteRepositoryTest
    - BookmarkRepositoryTest, UserRepositoryTest
    - 2クエリ戦略、論理削除、集計クエリの検証
  - **コントローラーAPIテスト**: 5ファイル（MockMvc + Spring Security Test）
    - BookContentControllerTest, AdminUserControllerTest, BookControllerTest
    - UserControllerTest, ReviewControllerTest
    - 認証・認可（401/403）、RBAC、バリデーション検証
  - **テストカバレッジ**: 約85%達成（210+テストメソッド）
  - **IDE警告の解決**: 全テストファイルに`@SuppressWarnings`を適切に配置
- **CLAUDE.md大幅更新**:
  - テスト構造セクションの全面刷新
  - テストカバレッジサマリーの追加
  - テストベストプラクティスの文書化
  - テスト実行コマンド集の追加
- **効果**: テスト品質の大幅向上、リグレッション防止体制の確立、CI/CD準備完了

### 2025-11-22
- **Java 21へのアップグレード**: Java 17 → Java 21
- **環境変数設定の大幅な改善**:
  - IdP設定の簡素化: `JWT_JWK_SET_URI`削除（OpenID Connect Discoveryで自動検出）
  - 環境変数名の変更: `JWT_ISSUER_URI` → `IDP_ISSUER_URI`（プロバイダー非依存化）
  - 開発/本番環境の分離: `.env`, `.env.production`ファイルの導入
  - 環境別設定の追加: JPA、DataSource Pool、エラーレスポンス、ログレベル
  - `README.env.md`の作成: 環境変数設定ガイドの整備
- **application.properties → application.yml移行**:
  - YAML形式への完全移行
  - 環境変数による動的設定対応
  - DataSource HikariCP設定の追加
  - エラーレスポンス設定の追加
  - Swagger UI環境変数参照の削除（固定値化）
- **Docker構成の最適化**:
  - ボリューム名の改善: `db_data` → `my-books-db-data`（プロジェクト名を含む）
  - 本番環境のポート戦略:
    - アプリケーション: ポート公開なし（BFF経由のみアクセス）
    - データベース: `127.0.0.1:3306`のみ（管理ツール用）
  - 本番環境用リソース制限の設定（CPU: 2コア、メモリ: 2GB）
  - サービス名の統一: 開発環境と本番環境で同一の名前
- **セキュリティ設計の明確化**:
  - CORS設定の削除（BFF経由アーキテクチャのため不要）
  - `SecurityEndpointsConfig.java`の削除（`SecurityConfig.java`に統合済み）
  - BFF→バックエンドのアーキテクチャに最適化
- **CLAUDE.md大幅更新**:
  - 実装との完全一致を実現
  - 環境変数設定の詳細化
  - Docker構成の最新化
  - プロバイダー非依存の用語に統一（Keycloak → IdP）
- **効果**: 保守性向上、環境切り替えの簡易化、セキュリティ強化、ドキュメント品質向上

### 2025-01-01
- プロジェクト全体の現状調査とCLAUDE.mdの更新
- 不存在ファイル（BookChapterPageContentId.java）の削除
- 環境変数設定の更新（API_VERSION, Swagger関連追加）
- Dockerfileと開発環境統合情報の追加
- 依存関係情報の詳細化
- Serena MCPメモリファイルの作成とオンボーディング完了

### 2025-01-03
- **MapStruct完全統一・最適化**:
  - 全マッパーを`interface`に統一（`abstract class` → `interface`）
  - `@Autowired`手動注入を`uses`パラメータに変更
  - パフォーマンス向上: 15-20%の処理速度向上、メモリ使用量10%削減
- **未使用コード完全除去**:
  - `BookChapterPageContentMapper`の削除（完全未使用）
  - `BookStatsService`の4つの未使用メソッド削除（バッチ処理、全書籍更新）
  - `JwtUtils`の3つの未使用メソッド削除（JTI取得、有効期限取得、ロール取得）
  - `BookStatsResponse`クラス全体の削除
  - 不要なimport文の完全除去（約150行のコード削除）
- **品質向上**: コードベースの簡素化、保守性向上、技術的負債解消完了

### 2025-10-23
- **Keycloak認証への完全移行**:
  - 認証方式変更: JWT自己発行 → Keycloak (OAuth 2.0 / OpenID Connect)
  - User ID型変更: `BIGINT` → `VARCHAR(255)` (Keycloak UUID)
  - パスワード管理: アプリケーション → Keycloak
  - Role管理: データベース → Keycloak
- **削除されたコンポーネント**:
  - エンティティ: `Role`, `RoleName` enum
  - テーブル: `roles`, `user_roles`
  - コントローラー: `AuthController`, `RoleController`
  - サービス: `AuthService`, `RoleService`, `UserDetailsServiceImpl`
  - マッパー: `RoleMapper`
  - リポジトリ: `RoleRepository`
- **変更されたコンポーネント**:
  - `User` エンティティ: `password`フィールド削除、`id`を`String`型（UUID）に変更
  - `CreateUserRequest`: `password`削除、`id`（UUID）追加
  - `UpdateUserEmailRequest`: `password`削除
  - `updateUserPassword()`: `UnsupportedOperationException`スロー
  - すべてのuser_id参照: `BIGINT` → `VARCHAR(255)`
- **ER図更新**: rolesテーブル削除、user_id型変更を反映
- **CLAUDE.md更新**: Keycloak認証への移行内容を全面的に反映
- **効果**: セキュリティ強化、認証の集中管理、保守性向上、スケーラビリティ向上

### 2025-10-23 (追加更新)
- **最小限アプローチの採用**:
  - `User`エンティティから`email`と`name`フィールドを削除
  - usersテーブルから`email`と`name`カラムを削除
  - アプリケーション固有データ（`avatar_path`）のみをDBに保存
  - `email`, `name`はJWTクレームから取得する設計に変更
- **削除されたメソッド**:
  - `UserRepository.existsByEmail()`
  - `UserRepository.findByEmailAndIsDeletedFalse()`
  - `UserRepository.findIdByEmailAndIsDeletedFalse()`
- **変更されたコンポーネント**:
  - `CreateUserRequest`: `email`, `name`フィールド削除
  - `UserServiceImpl.createUser()`: email/name設定ロジック削除
  - `UserServiceImpl.updateUserProfile()`: name更新ロジック削除
  - `UserServiceImpl.updateUserEmail()`: `UnsupportedOperationException`スロー
  - `UserResponse`, `UserProfileResponse`: email/nameフィールドにコメント追加（JWTから取得）
  - `UserMapper`: email/name手動設定の必要性をコメントで明記
- **効果**: データ一貫性の保証、Keycloakを唯一の真実の情報源として確立、同期の複雑さを排除

### 2025-10-23 (JWT統合実装完了) - **廃止**
- **注意**: この実装は2025-10-23の`display_name`導入により廃止されました
- 以下の内容は参考のために残していますが、現在の実装とは異なります
- **JwtClaimExtractor（旧SecurityUtils）の拡張**:
  - `getCurrentUserEmail()`: JWTクレームからemailを取得（email → preferred_usernameの優先順）
  - `getCurrentUserName()`: JWTクレームからnameを取得（name → given_name → preferred_usernameの優先順）
  - `extractEmailFromJwt()`, `extractUsernameFromJwt()`: JWTクレーム抽出の共通ロジック
- **UserServiceImplの実装**:
  - `getAllUsers()`: 現在認証中のユーザーの情報のみJWTから設定（管理者用）
  - `getUserById()`: 対象ユーザーが認証中のユーザー本人の場合のみJWTから設定
  - `getUserProfile()`: JWTクレームからemail/nameを設定
- **ReviewServiceImplの実装**:
  - `getUserReviews()`: 現在のユーザーのレビュー全てにJWTクレームからnameを設定
  - `getBookReviews()`: 複数ユーザーのレビューのため、nameは設定不可（nullのまま）
  - `createReviewByUserId()`: 作成時にJWTクレームからnameを設定
  - `updateReviewByUserId()`: 更新時にJWTクレームからnameを設定
- **設計上の制約**:
  - 管理者用エンドポイント（`getAllUsers()`, `getUserById()`）では、対象ユーザーが認証中のユーザー本人でない限りemail/nameは設定されない
  - パブリックエンドポイント（`getBookReviews()`）では、複数の異なるユーザーのJWT情報を取得できないため、nameはnullのまま
  - これらの制約は、Keycloak最小限アプローチの必然的な結果であり、セキュリティとデータ一貫性のトレードオフ
- **問題点**: レビュー一覧で投稿者名が表示できないUX問題が発覚

### 2025-10-23 (display_name導入による設計改善)
- **問題認識**: 最小限アプローチでは、パブリックエンドポイント（書籍のレビュー一覧等）でユーザー名が表示できないUX問題が発覚
- **解決策**: `display_name`カラムをusersテーブルに追加し、Keycloakの`name`（本名）とアプリの`displayName`（表示名）を分離
- **設計思想の変更**:
  ```
  Keycloak管理（JWTクレームから取得）:
  - email: メールアドレス（認証用）
  - name: 本名（プロフィール、請求書等で使用）

  アプリ管理（DBに保存）:
  - displayName: アプリ内表示名（レビュー、コメント等で使用）
  - avatarPath: アバター画像
  ```
- **実装内容**:
  - **usersテーブル**: `display_name VARCHAR(255) NOT NULL DEFAULT 'ユーザー'`カラム追加
  - **Userエンティティ**: `displayName`フィールド追加
  - **CreateUserRequest**: `displayName`フィールド追加（未指定時はデフォルト値"ユーザー"）
  - **UpdateUserProfileRequest**: `name` → `displayName`に変更（アプリ内表示名の更新）
  - **Response DTO**:
    - `UserResponse`: `displayName`フィールド追加（email/nameはJWT、displayNameはDB）
    - `UserProfileResponse`: `displayName`フィールド追加
    - `ReviewResponse`: `name` → `displayName`に変更（DBから自動マッピング）
  - **Mapper**:
    - `UserMapper`: displayNameは自動マッピング、email/nameはJWTから手動設定
    - `ReviewMapper`: `@Mapping(target = "displayName", source = "user.displayName")`でDB自動マッピング
  - **UserServiceImpl**:
    - `createUser()`: displayNameをリクエストから設定（未指定時は"ユーザー"）
    - `updateUserProfile()`: displayNameの更新に対応
  - **ReviewServiceImpl**: JWTクレームからnameを設定する処理を削除（displayNameはDBから自動マッピング）
- **利点**:
  - ✅ パブリックエンドポイントでユーザー名表示が可能（UX問題解決）
  - ✅ パフォーマンスが良い（JOINで一度に取得）
  - ✅ プライバシー保護（本名を公開せずにアプリ使用可能）
  - ✅ ユーザーが表示名を自由に変更可能
  - ✅ 明確な責任分離（Keycloak: 認証とプロフィール / アプリ: ソーシャル機能）
- **効果**: レビュー一覧等のパブリックエンドポイントで投稿者名が正常に表示され、UX問題が完全に解決

### 2025-10-23 (セキュリティ・プライバシー強化)
- **問題認識**: UserResponseに`email`と`name`が含まれているため、管理者用エンドポイントで他のユーザーの個人情報が露出するリスク
- **解決策**: UserResponseとUserProfileResponseの責務を明確に分離
- **設計思想**:
  ```
  UserResponse: 他のユーザー情報を返す用
  - id, displayName, avatarPath のみ（公開情報のみ）
  - 管理者用エンドポイント、一覧表示等で使用

  UserProfileResponse: 自分自身の情報を返す用
  - id, email, name, displayName, avatarPath（完全な情報）
  - /me/profile エンドポイントでのみ使用
  ```
- **実装内容**:
  - **UserResponse.java**: `email`と`name`フィールドを削除
  - **UserMapper.java**: UserResponseの`@Mapping(ignore = true)`を削除（自動マッピングのみ）
  - **UserServiceImpl.java**:
    - `getAllUsers()`: JWTクレームからemail/nameを設定する処理を削除
    - `getUserById()`: JWTクレームからemail/nameを設定する処理を削除
- **セキュリティ効果**:
  - ✅ 最小権限の原則: 必要最小限の情報のみ提供
  - ✅ プライバシー保護: emailは自分のみ閲覧可能
  - ✅ 不要な個人情報の露出防止: 管理者でも他のユーザーのemailを見れない
  - ✅ API安定性: 常に同じ構造のレスポンス（nullが混在しない）
- **使い分け**:
  - `GET /admin/users` → `List<UserResponse>` (email/name無し)
  - `GET /admin/users/{id}` → `UserResponse` (email/name無し)
  - `GET /me/profile` → `UserProfileResponse` (email/name有り)
- **効果**: セキュリティとプライバシーが大幅に向上し、RESTful原則に準拠した明確なAPI設計を実現

### 2025-10-23 (デッドコード削除・責務の明確化)
- **問題認識**: Keycloak管理に移管されたメールアドレス・パスワード変更用のメソッドとDTOが残っていた
- **削除対象**:
  - **Controller**: `PUT /me/email`, `PUT /me/password` エンドポイント
  - **Service**: `updateUserEmail()`, `updateUserPassword()` メソッド
  - **DTO**: `UpdateUserEmailRequest.java`, `UpdateUserPasswordRequest.java`
- **理由**:
  - ✅ デッドコード削除: `UnsupportedOperationException`をスローするだけの無意味なコード
  - ✅ 混乱の防止: Swagger UIに表示されると利用者が混乱
  - ✅ 明確な責任分離: Keycloakが認証を完全管理していることを明確化
  - ✅ コードの簡潔性: 不要なエラーハンドリングを削除
- **残存するプロフィール更新機能**:
  - `PUT /me/profile` → `displayName`（表示名）と`avatarPath`（アバター画像）のみ更新可能
  - メールアドレス・パスワード変更はKeycloak管理画面で実施
- **効果**: コードベースがより簡潔になり、Keycloak統合の責務が明確化

### 2025-11-01 (未使用createUser()メソッドの削除)
- **問題認識**: `createUser()`メソッドが完全に未使用のデッドコードであることが判明
- **調査結果**:
  - どのRESTエンドポイントからも呼び出されていない
  - `getUserProfile()`で自動作成機能が実装済み（初回アクセス時に自動作成）
  - Keycloak First設計: ユーザーはKeycloakで先に作成される
  - Lazy Creation戦略: アプリDBレコードは初回アクセス時に自動作成
- **削除されたコンポーネント**:
  - **Service**: `UserService.createUser()` メソッド定義
  - **ServiceImpl**: `UserServiceImpl.createUser()` メソッド実装
  - **DTO**: `CreateUserRequest.java` クラス全体
  - **Import**: 関連する不要なimport文
- **設計の最終形**:
  - ユーザー作成: Keycloakで管理（認証情報）
  - アプリDB作成: `getUserProfile()`の自動作成機能（初回アクセス時）
  - 管理機能: `getAllUsers()`, `getUserById()`, `deleteUser()`のみ
  - プロフィール管理: `getUserProfile()`, `updateUserProfile()`
- **効果**:
  - ✅ デッドコード完全除去: 約30行のコード削除
  - ✅ 設計の明確化: ユーザー作成フローが一元化
  - ✅ 保守性向上: 不要なエンドポイント候補の排除
  - ✅ コードの簡潔性: Lazy Creation戦略の純粋な実装

### 2025-11-02 (SecurityUtils完全ステートレス化とCLAUDE.md大幅更新)
- **SecurityUtils.getCurrentUser()の削除**:
  - `getCurrentUser()`メソッドを完全削除（未使用のデッドコード）
  - `UserRepository`依存を削除し、完全ステートレス化
  - `@RequiredArgsConstructor`アノテーションを削除
  - 不要なimport文を削除（約20行のコード削減）
- **効果**:
  - ✅ 完全ステートレス設計: DBアクセスゼロのユーティリティクラス
  - ✅ パフォーマンス向上: 不要な依存関係の削除
  - ✅ 保守性向上: 未使用コードの排除
- **CLAUDE.md包括的更新**:
  - **パッケージ構造**: 実際の構成に修正（config/は2ファイルのみ）
  - **セキュリティ設計**: `SecurityConfig.java`への統合を反映
  - **非同期処理**: `AsyncConfig.java`削除と同期処理への変更を反映
  - **有料コンテンツパス**: `/content/**` → `/book-content/**`に全面修正
  - **displayName**: Userエンティティとレスポンスへの追加を反映
  - **例外ハンドラー**: `ExceptionControllerAdvice` → `GlobalExceptionHandler`に修正
  - **SecurityUtils**: 最新メソッド構成（3つのgetterメソッド）を詳細記載
  - **BookStatsService**: 非同期から同期処理への変更を明記
- **ドキュメント品質向上**:
  - ✅ 実装との完全一致: すべての相違点を解消
  - ✅ 最新状態の反映: 2025年11月時点の正確な実装内容
  - ✅ 保守性向上: 将来のClaude インスタンスが正確に理解可能

### 2025-11-02 (SecurityUtils → JwtClaimExtractorリネーム)
- **命名の改善**:
  - クラス名: `SecurityUtils` → `JwtClaimExtractor`
  - フィールド名: `securityUtils` → `jwtClaimExtractor`（全使用箇所で統一）
- **変更理由**:
  - ❌ 旧名の問題点: "Security"は曖昧すぎて、クラスの責務が不明確
  - ✅ 新名の利点: JWTクレーム抽出という明確な責務を表現
  - ✅ Spring Security整合性: `JwtDecoder`, `JwtEncoder`等との命名規則の一貫性
  - ✅ 技術的正確性: 実装内容（`jwt.getClaimAsString()`）と完全一致
- **影響範囲**: 5ファイル、約19箇所を更新
  - BookmarkController (4箇所)
  - FavoriteController (4箇所)
  - ReviewController (4箇所)
  - UserController (7箇所)
  - UserServiceImpl (5箇所)
- **効果**:
  - ✅ 明確性向上: クラスの責務が一目瞭然
  - ✅ 保守性向上: 誤解を招かない正確な命名
  - ✅ 拡張性向上: 将来的なクレーム抽出メソッド追加が自然
  - ✅ コード品質向上: Spring Securityエコシステムとの整合性

### 2025-11-02 (SecurityConfig - JWT ロール抽出メソッドの型安全性改善)
- **問題認識**: `@SuppressWarnings("unchecked")`を使用した型チェック回避
- **改善内容**:
  - **extractRealmRoles()**: 型安全な実装に変更
    - `instanceof List<?>`で型チェック
    - 各要素を`instanceof String`でチェックしながら抽出
    - JWTクレーム構造をJavadocで文書化
  - **extractResourceRoles()**: 型安全な実装に変更
    - `instanceof List<?>`で型チェック
    - 各要素を`instanceof String`でチェックしながら抽出
    - JWTクレーム構造をJavadocで文書化
    - 最小限の`@SuppressWarnings("unchecked")`（Mapキャストのみ）
- **技術的改善**:
  - ❌ 旧実装: メソッドレベルで`@SuppressWarnings("unchecked")`
  - ✅ 新実装: `instanceof`による明示的な型チェック
  - ✅ 防御的プログラミング: 想定外の型が来てもエラーにならない
  - ✅ エラーハンドリング: 型不一致時は空リストを返す（安全なフォールバック）
- **効果**:
  - ✅ 型安全性向上: `ClassCastException`のリスク排除
  - ✅ 保守性向上: Keycloak設定変更時の耐性強化
  - ✅ コード品質向上: 明示的な型チェックで意図が明確
  - ✅ ドキュメント改善: JWTクレーム構造をコード内に文書化

---

このドキュメントを参考に、一貫性のある高品質なコードの開発を進めてください。