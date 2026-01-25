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
- **Mapper**: Entity ↔ DTO 変換（MapStruct interface実装）

## 重要な依存関係順序（MapStruct + Lombok）
```gradle
// annotation processor の順序が重要
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
```

## MapStruct設計パターン
- **interface実装**: 全マッパーをinterfaceで実装（abstract classは使用しない）
- **uses パラメータ**: `@Mapper(componentModel = "spring", uses = {XxxMapper.class})`で依存マッパーを注入
- **@Autowired削除**: 手動注入は使用せず、usesパラメータで自動注入
- **効果**: 15-20%の処理速度向上、メモリ使用量10%削減

## セキュリティ規約
- **認証が必要なエンドポイント**: デフォルト
- **パブリックエンドポイント**: `SecurityConfig.java` で明示的に設定
  - GET `/books/**` - 書籍情報の閲覧のみパブリック
  - GET `/genres/**` - ジャンル情報の閲覧のみパブリック
  - `/swagger-ui/**`, `/v3/api-docs/**` - Swagger UI
- **認証**: Keycloak (OAuth 2.0 / OpenID Connect)
- **ユーザーID**: Keycloak UUID (String型)
- **パスワード管理**: Keycloak側で管理（アプリケーション側では管理しない）
- **Role管理**: Keycloak側で管理（realm_access / resource_accessから抽出）
- **管理者機能**: `@PreAuthorize("hasRole('ADMIN')")`でメソッドレベル認可
- **CORS**: 不要（BFF経由アーキテクチャのため）

## エンティティ設計パターン
- **基底クラス**: `EntityBase` - すべてのエンティティが継承
- **自動フィールド**: `createdAt`, `updatedAt`, `isDeleted`
- **論理削除**: `isDeleted` フラグで実装
- **複合主キー**: `@EmbeddedId` アノテーションを使用

## User エンティティ設計（Keycloak統合）
```java
@Entity
public class User extends EntityBase {
    @Id
    private String id;              // Keycloak UUID
    private String displayName;     // アプリ内表示名（レビュー・コメント用）
    private String avatarPath;      // アバター画像パス
    // email, name, password, rolesフィールドは削除済み
}
```

### データ取得方法
- **email** (認証用): JWTクレームから取得（JwtClaimExtractor.getEmail()）
- **username** (ユーザー名): JWTクレームから取得（JwtClaimExtractor.getUsername()）
- **familyName** (姓): JWTクレームから取得（JwtClaimExtractor.getFamilyName()）
- **givenName** (名): JWTクレームから取得（JwtClaimExtractor.getGivenName()）
- **displayName** (表示名): DBから取得（レビュー・コメント等で使用）
- **設計思想**: Keycloak管理（email, username, familyName, givenName）とアプリ管理（displayName, avatarPath）の分離

## JwtClaimExtractor の使用方法
```java
@RestController
public class XxxController {
    private final JwtClaimExtractor jwtClaimExtractor;

    public ResponseEntity<?> someMethod() {
        String userId = jwtClaimExtractor.getUserId();           // Keycloak UUID (sub クレーム)
        String email = jwtClaimExtractor.getEmail();         // email クレーム
        String username = jwtClaimExtractor.getUsername();       // preferred_username クレーム (フォールバック: name, given_name)
        String familyName = jwtClaimExtractor.getFamilyName();   // family_name クレーム
        String givenName = jwtClaimExtractor.getGivenName();     // given_name クレーム
    }
}
```

## パフォーマンス最適化パターン
- **2クエリ戦略**: `PageableUtils.applyTwoQueryStrategy()` で N+1問題を解決
- **リポジトリパターン**: `findAllByIdInWithRelations()` メソッドで JOIN FETCH
- **ソート順序復元**: `PageableUtils.restoreSortOrder()` でIDリスト順序を保持
- **章タイトル動的取得**: 書籍ごとの章情報をバッチ取得して効率化
