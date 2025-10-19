# コードベース構造

## パッケージ構造
```
com.example.my_books_backend/
├── config/          # 設定クラス
│   ├── AsyncConfig.java           # 非同期処理設定
│   ├── AuthTokenFilter.java       # JWT認証フィルター
│   ├── SecurityConfig.java        # Spring Security設定
│   ├── SecurityEndpointsConfig.java # エンドポイントアクセス制御設定
│   └── SwaggerConfig.java         # Swagger/OpenAPI設定
├── controller/      # REST API エンドポイント
│   ├── AdminUserController.java   # 管理者用ユーザー管理
│   ├── AuthController.java        # 認証（ログイン/サインアップ）
│   ├── BookController.java        # 書籍関連（パブリック情報）
│   ├── BookContentController.java # 書籍コンテンツ（認証必要）
│   ├── BookmarkController.java    # ブックマーク
│   ├── FavoriteController.java    # お気に入り
│   ├── GenreController.java       # ジャンル
│   ├── ReviewController.java      # レビュー
│   ├── RoleController.java        # ロール
│   └── UserController.java        # ユーザープロフィール（/me エンドポイント）
├── dto/            # データ転送オブジェクト
│   ├── PageResponse.java          # ページネーションレスポンス
│   └── [機能別ディレクトリ]        # auth/, book/, user/, etc.
├── entity/         # JPA エンティティ
│   ├── base/
│   │   └── EntityBase.java        # 基底エンティティ
│   ├── enums/
│   │   └── RoleName.java          # ロール名enum
│   └── [各種エンティティクラス]
├── exception/      # カスタム例外とエラーハンドリング
├── mapper/         # MapStruct マッパーインターフェース
├── repository/     # JPA リポジトリ
├── service/        # ビジネスロジック
│   └── impl/       # サービス実装
└── util/          # ユーティリティクラス
    ├── JwtUtils.java              # JWT生成・検証
    └── PageableUtils.java         # ページネーション
```

## 重要なファイル
- `MyBooksBackendApplication.java`: メインアプリケーションクラス
- `application.properties`: アプリケーション設定
- `build.gradle`: ビルド設定とライブラリ依存関係
- `docker-compose.yml`: Docker環境設定
- `Dockerfile`: コンテナ設定