# コードベース構造

## パッケージ構造
```
com.example.my_books_api/
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
│   └── User.java                  # ユーザー（Keycloak UUID使用、最小限アプローチ + 表示名）
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

## 重要なファイル
- `MyBooksApiApplication.java`: メインアプリケーションクラス
- `application.yml`: アプリケーション設定
- `build.gradle`: ビルド設定とライブラリ依存関係
- `docker-compose.yml`: Docker開発環境設定
- `docker-compose.prod.yml`: Docker本番環境設定
- `Dockerfile`: コンテナ設定（開発・本番マルチステージビルド）
- `.env.example`: 開発環境用環境変数テンプレート
- `.env.production.example`: 本番環境用環境変数テンプレート

## 削除済みコンポーネント（Keycloak移行により）
- ~~`AuthController.java`~~ - 削除済み（Keycloak管理）
- ~~`RoleController.java`~~ - 削除済み（Keycloak管理）
- ~~`AuthTokenFilter.java`~~ - 削除済み（OAuth2 Resource Serverに置き換え）
- ~~`SecurityEndpointsConfig.java`~~ - 削除済み（SecurityConfig.javaに統合）
- ~~`AsyncConfig.java`~~ - 削除済み（同期処理に変更）
- ~~`Role` エンティティ~~ - 削除済み（Keycloak管理）
- ~~`RoleName` enum~~ - 削除済み（Keycloak管理）
