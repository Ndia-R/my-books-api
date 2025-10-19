# 開発規約とコーディングスタイル

## ネーミング規約
- **エンティティ**: PascalCase（例: `User`, `BookChapter`）
- **フィールド**: camelCase（例: `createdAt`, `averageRating`）
- **テーブル**: snake_case（例: `users`, `book_chapters`）
- **API エンドポイント**: kebab-case（例: `/new-releases`）
- **複合主キー**: エンティティ名 + "Id"（例: `BookChapterId`）

## パッケージ構成規約
- **Controller**: REST API の責務のみ
- **Service**: ビジネスロジックの実装（インターフェース + 実装クラス）
- **Repository**: データアクセスの抽象化
- **DTO**: API入出力の専用オブジェクト（機能別ディレクトリ分け）
- **Mapper**: Entity ↔ DTO 変換（MapStruct）

## 重要な依存関係順序（MapStruct + Lombok）
```gradle
// annotation processor の順序が重要
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```

## セキュリティ規約
- **認証が必要なエンドポイント**: デフォルト
- **パブリックエンドポイント**: `SecurityEndpointsConfig` で明示的に設定
- **パスワード**: 必ず BCrypt で暗号化
- **JWT**: HttpOnly Cookie でリフレッシュトークン管理
- **CORS**: localhost パターンのみ許可

## エンティティ設計パターン
- **基底クラス**: `EntityBase` - すべてのエンティティが継承
- **自動フィールド**: `createdAt`, `updatedAt`, `isDeleted`
- **論理削除**: `isDeleted` フラグで実装
- **複合主キー**: `@EmbeddedId` アノテーションを使用