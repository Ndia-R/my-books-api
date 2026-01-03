# My Books Backend - Role設計提案書

## 📋 目次

- [現在の実装状況](#現在の実装状況)
- [推奨Role設計](#推奨role設計)
- [Role詳細](#role詳細)
- [実装例](#実装例)
- [Keycloak設定](#keycloak設定)
- [実装時の注意点](#実装時の注意点)
- [段階的実装計画](#段階的実装計画)

---

## 現在の実装状況

### 実装済みの機能

- **SecurityConfig.java**: Realm RoleとClient Roleの両方をサポート
- **AdminUserController**: `ROLE_ADMIN`のみを使用
- **エンドポイント認可**:
  - GETエンドポイント（`/books/**`, `/genres/**`）: パブリック
  - その他のエンドポイント: 認証必須（ロールチェックなし）
  - 管理者エンドポイント（`/admin/**`）: `ROLE_ADMIN`必須

### 現在のセキュリティ設定

```java
// SecurityConfig.java（抜粋）
.authorizeHttpRequests(auth -> auth
    // GETのみパブリック: 書籍情報の閲覧
    .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/genres/**").permitAll()

    // その他すべて認証必要
    .anyRequest().authenticated()
)
```

```java
// AdminUserController.java
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    // ユーザー管理機能
}
```

---

## 推奨Role設計

### 3階層のRole構造

このアプリの特性（書籍管理、レビュー、有料コンテンツ、管理機能）を考慮した5つのRoleを提案します。

```
USER (一般ユーザー)
├─ 書籍の閲覧（パブリックと同じ）
├─ レビュー投稿・編集・削除（自分のもののみ）
├─ お気に入り管理
├─ ブックマーク管理
└─ プロフィール管理

PREMIUM_USER (有料会員)
├─ USERの全権限
├─ /book-content/** へのアクセス（有料コンテンツ）
└─ 高度な検索機能（将来的な拡張）

AUTHOR (著者・出版社)
├─ USERの全権限
├─ 自分の書籍の統計閲覧
├─ 自分の書籍のレビュー管理（削除権限）
└─ 自分の書籍情報の編集

MODERATOR (モデレーター)
├─ USERの全権限
├─ 不適切なレビューの削除
├─ ユーザー報告の管理
└─ コンテンツの一時非公開化

ADMIN (システム管理者)
├─ 全ての権限
├─ ユーザー管理（/admin/users/**）
├─ 書籍の追加・編集・削除
├─ ジャンル管理
└─ システム設定
```

---

## Role詳細

### 1. USER（一般ユーザー）

**対象:** 無料登録ユーザー
**想定人数:** 多数

#### 権限

| 機能 | 操作 | エンドポイント例 |
|------|------|-----------------|
| 書籍閲覧 | GET | `/books/**`, `/genres/**` |
| レビュー管理 | POST/PUT/DELETE | `/reviews/**`（自分のもののみ） |
| お気に入り | POST/DELETE | `/favorites/**` |
| ブックマーク | POST/PUT/DELETE | `/bookmarks/**` |
| プロフィール | GET/PUT | `/me/profile` |

#### ビジネス目的

- 基本的なユーザー体験の提供
- サービスへのエンゲージメント
- PREMIUM_USERへのアップグレード促進

---

### 2. PREMIUM_USER（有料会員）

**対象:** サブスクリプション契約ユーザー
**想定人数:** 中程度

#### 権限

| 機能 | 操作 | エンドポイント例 |
|------|------|-----------------|
| USERの全権限 | - | - |
| 有料コンテンツ閲覧 | GET | `/book-content/**` |
| 高度な検索 | GET | `/books/advanced-search` |
| 無制限ブックマーク | POST | `/bookmarks/**` |
| オフライン保存 | GET | `/books/{id}/download` |

#### ビジネス目的

- 収益化の核となるロール
- フリーミアムモデルの実現
- 継続課金ユーザーの獲得

---

### 3. AUTHOR（著者・出版社）

**対象:** コンテンツ提供者（著者、出版社）
**想定人数:** 少数

#### 権限

| 機能 | 操作 | エンドポイント例 |
|------|------|-----------------|
| USERの全権限 | - | - |
| 自分の書籍管理 | POST/PUT | `/books/**`（所有権チェック） |
| 書籍統計閲覧 | GET | `/books/{id}/analytics` |
| レビュー管理 | DELETE | `/reviews/**`（自分の書籍のみ） |
| 章・ページ管理 | POST/PUT/DELETE | `/books/{id}/chapters/**` |

#### ビジネス目的

- コンテンツプロバイダーの自律的な管理
- プラットフォームへのコンテンツ供給促進
- 著者と読者の直接的なエンゲージメント

---

### 4. MODERATOR（モデレーター）

**対象:** コンテンツ管理担当者
**想定人数:** 少数（2-5名程度）

#### 権限

| 機能 | 操作 | エンドポイント例 |
|------|------|-----------------|
| USERの全権限 | - | - |
| レビュー削除 | DELETE | `/reviews/**`（全て） |
| ユーザー報告管理 | GET/PUT | `/moderation/reports/**` |
| コンテンツ非公開化 | PUT | `/books/{id}/visibility` |
| 不適切コンテンツ管理 | GET/DELETE | `/moderation/**` |

#### ビジネス目的

- コミュニティの健全性維持
- 不適切コンテンツの迅速な対応
- ADMINの負担軽減

---

### 5. ADMIN（システム管理者）

**対象:** システム全体の管理者
**想定人数:** 極少数（1-2名）

#### 権限

| 機能 | 操作 | エンドポイント例 |
|------|------|-----------------|
| 全ての権限 | ALL | `/admin/**` |
| ユーザー管理 | GET/DELETE | `/admin/users/**` |
| 書籍管理 | POST/PUT/DELETE | `/books/**`（全て） |
| ジャンル管理 | POST/PUT/DELETE | `/genres/**` |
| システム設定 | GET/PUT | `/admin/settings/**` |
| Role管理 | - | Keycloak管理画面 |

#### ビジネス目的

- システム全体の管理・監視
- 緊急時の対応
- データの整合性維持

---

## 実装例

### SecurityConfig.javaの強化案

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 完全パブリック: Swagger UI関連
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // 完全パブリック: Actuator
                .requestMatchers("/actuator/**").permitAll()

                // パブリック: 書籍情報の閲覧
                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/genres/**").permitAll()

                // 有料コンテンツ: PREMIUM_USER以上
                .requestMatchers("/book-content/**")
                    .hasAnyRole("PREMIUM_USER", "AUTHOR", "MODERATOR", "ADMIN")

                // 管理者専用
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // モデレーター機能
                .requestMatchers("/moderation/**")
                    .hasAnyRole("MODERATOR", "ADMIN")

                // 書籍管理: AUTHOR以上（細かい制御はメソッドレベルで）
                .requestMatchers(HttpMethod.POST, "/books/**")
                    .hasAnyRole("AUTHOR", "MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/books/**")
                    .hasAnyRole("AUTHOR", "MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/books/**")
                    .hasAnyRole("MODERATOR", "ADMIN")

                // ジャンル管理: ADMIN専用
                .requestMatchers(HttpMethod.POST, "/genres/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/genres/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/genres/**").hasRole("ADMIN")

                // その他: 認証必須（USERロールがあればOK）
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    // 既存のjwtAuthenticationConverter()は維持
}
```

### コントローラーレベルの@PreAuthorize例

#### BookContentController（有料コンテンツ）

```java
@RestController
@RequestMapping("/book-content")
@RequiredArgsConstructor
@Tag(name = "BookContent", description = "書籍コンテンツAPI（認証必須）")
@PreAuthorize("hasAnyRole('PREMIUM_USER', 'AUTHOR', 'MODERATOR', 'ADMIN')")
public class BookContentController {

    @GetMapping("/books/{id}/chapters/{chapter}/pages/{page}")
    @Operation(description = "書籍ページコンテンツ取得")
    public ResponseEntity<BookChapterPageContentResponse> getPageContent(
        @PathVariable String id,
        @PathVariable Long chapter,
        @PathVariable Long page
    ) {
        // 実装
    }
}
```

#### BookController（書籍管理）

```java
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Book", description = "書籍API")
public class BookController {

    // GET: パブリック（@PreAuthorizeなし）
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable String id) {
        // 実装
    }

    // POST: AUTHOR以上
    @PostMapping
    @PreAuthorize("hasAnyRole('AUTHOR', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> createBook(@RequestBody BookRequest request) {
        // 実装
    }

    // PUT: AUTHOR以上（所有者チェックはServiceレイヤーで）
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> updateBook(
        @PathVariable String id,
        @RequestBody BookRequest request
    ) {
        // 実装
    }

    // DELETE: MODERATOR以上
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        // 実装
    }
}
```

#### ModerationController（モデレーション機能）

```java
@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "モデレーションAPI")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class ModerationController {

    @GetMapping("/reports")
    @Operation(description = "ユーザー報告一覧取得")
    public ResponseEntity<PageResponse<ReportResponse>> getReports(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // 実装
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(description = "不適切なレビュー削除")
    public ResponseEntity<Void> deleteInappropriateReview(@PathVariable Long id) {
        // 実装
    }
}
```

### Serviceレイヤーでの所有者チェック例

```java
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final JwtClaimExtractor jwtClaimExtractor;

    @Override
    public BookResponse updateBook(String bookId, BookRequest request) {
        // 書籍を取得
        Book book = bookRepository.findByIdAndIsDeletedFalse(bookId)
            .orElseThrow(() -> new NotFoundException("書籍が見つかりません"));

        // 現在のユーザーIDを取得
        String currentUserId = jwtClaimExtractor.getCurrentUserId();

        // 権限チェック（Spring Securityから取得）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isModerator = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR"));

        // 所有者チェック: 自分の書籍 or MODERATOR以上
        if (!book.getAuthorId().equals(currentUserId) && !isModerator && !isAdmin) {
            throw new ForbiddenException("この書籍を編集する権限がありません");
        }

        // 更新処理
        // ...
    }
}
```

または、Spring Securityの`@PreAuthorize`でSpEL式を使用：

```java
@Service
public class BookSecurityService {

    @Autowired
    private BookRepository bookRepository;

    public boolean isOwner(String bookId, String userId) {
        return bookRepository.findByIdAndIsDeletedFalse(bookId)
            .map(book -> book.getAuthorId().equals(userId))
            .orElse(false);
    }
}

// Controller
@PutMapping("/books/{id}")
@PreAuthorize("hasAnyRole('AUTHOR', 'MODERATOR', 'ADMIN') and " +
              "(hasAnyRole('MODERATOR', 'ADMIN') or " +
              "@bookSecurityService.isOwner(#id, authentication.name))")
public ResponseEntity<BookResponse> updateBook(
    @PathVariable String id,
    @RequestBody BookRequest request
) {
    // 実装
}
```

---

## Keycloak設定

### 推奨構成: Realm Role

```
sample-realm (Keycloak Realm)
├─ USER (デフォルトRole)
├─ PREMIUM_USER
├─ AUTHOR
├─ MODERATOR
└─ ADMIN
```

#### Realm Roleを推奨する理由

| 理由 | 説明 |
|------|------|
| **シンプル** | クライアント（my-books-api）が1つの場合は十分 |
| **ポータビリティ** | 将来的に別アプリ（my-music等）でも同じRoleを共有可能 |
| **管理容易** | Keycloak管理画面での設定が直感的 |
| **既存実装との一致** | SecurityConfig.javaがRealm Roleを抽出済み |

#### Keycloak管理画面での設定手順

1. **Realm Roleの作成**
   - Keycloak管理画面 → Realm Settings → Roles → Create Role
   - 以下のRoleを作成:
     - `USER`
     - `PREMIUM_USER`
     - `AUTHOR`
     - `MODERATOR`
     - `ADMIN`

2. **デフォルトRoleの設定**
   - Realm Settings → Roles → Default Roles
   - `USER`を追加（新規ユーザーに自動付与）

3. **ユーザーへのRole割り当て**
   - Users → 対象ユーザー → Role Mappings
   - Realm Rolesから必要なRoleを割り当て

---

### 代替構成: Client Role

マルチアプリ構成で各アプリ固有の権限を分けたい場合に有効です。

```
my-books-api (Client)
├─ book_reader (≒ USER)
├─ book_premium (≒ PREMIUM_USER)
├─ book_author (≒ AUTHOR)
├─ book_moderator (≒ MODERATOR)
└─ book_admin (≒ ADMIN)

my-music-api (Client)
├─ music_listener
├─ music_premium
└─ music_admin
```

#### Client Roleの利点

- アプリケーションごとに独立した権限体系
- Roleの名前空間が分離される
- 異なるアプリで異なる権限設計が可能

#### SecurityConfigの対応状況

現在の実装は既にClient Roleもサポートしています：

```java
// SecurityConfig.java (Line 168-195)
private List<String> extractResourceRoles(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    // resource_access.{client}.roles を抽出
}
```

---

### JWTトークンの例

#### Realm Roleの場合

```json
{
  "sub": "ad51b2bf-c290-4bd9-bbe4-f96e29cd74d6",
  "email": "user@example.com",
  "realm_access": {
    "roles": ["USER", "PREMIUM_USER"]
  }
}
```

Spring Securityでの変換結果:
- `ROLE_USER`
- `ROLE_PREMIUM_USER`

#### Client Roleの場合

```json
{
  "sub": "ad51b2bf-c290-4bd9-bbe4-f96e29cd74d6",
  "email": "author@example.com",
  "resource_access": {
    "my-books-api": {
      "roles": ["book_reader", "book_author"]
    }
  }
}
```

Spring Securityでの変換結果:
- `ROLE_BOOK_READER`
- `ROLE_BOOK_AUTHOR`

---

## 実装時の注意点

### 1. デフォルトRoleの設定

新規ユーザーには自動的に`USER`を付与してください。

**Keycloak設定:**
- Realm Settings → Roles → Default Roles → `USER`を追加

### 2. Role階層について

Spring Securityの`RoleHierarchy`は**使用しない**ことを推奨します。

**理由:**
- 明示的な`hasAnyRole()`の方が可読性が高い
- 権限の継承関係がコードから明確
- デバッグが容易

**推奨しない例:**
```java
// RoleHierarchyの使用（推奨しない）
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy(
        "ROLE_ADMIN > ROLE_MODERATOR > ROLE_AUTHOR > ROLE_PREMIUM_USER > ROLE_USER"
    );
    return hierarchy;
}
```

**推奨する例:**
```java
// 明示的なhasAnyRole()（推奨）
@PreAuthorize("hasAnyRole('PREMIUM_USER', 'AUTHOR', 'MODERATOR', 'ADMIN')")
```

### 3. 所有者チェックの実装

自分のリソースのみ編集可能にする場合は、Serviceレイヤーでチェックを実装してください。

**実装パターン:**
1. Serviceレイヤーでの明示的なチェック（推奨）
2. `@PreAuthorize`でのSpEL式使用
3. 専用の`SecurityService`の作成

### 4. テストの追加

Role別のテストケースを追加してください。

```java
@Test
@WithMockUser(roles = "USER")
void testAccessPremiumContent_USER_403Forbidden() throws Exception {
    mockMvc.perform(get("/book-content/books/1/chapters/1/pages/1")
            .with(jwt()))
        .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "PREMIUM_USER")
void testAccessPremiumContent_PREMIUM_200OK() throws Exception {
    mockMvc.perform(get("/book-content/books/1/chapters/1/pages/1")
            .with(jwt()))
        .andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "ADMIN")
void testDeleteUser_ADMIN_204NoContent() throws Exception {
    mockMvc.perform(delete("/admin/users/user123")
            .with(jwt()))
        .andExpect(status().isNoContent());
}

@Test
@WithMockUser(roles = "MODERATOR")
void testDeleteUser_MODERATOR_403Forbidden() throws Exception {
    mockMvc.perform(delete("/admin/users/user123")
            .with(jwt()))
        .andExpect(status().isForbidden());
}
```

### 5. エラーメッセージの改善

`ForbiddenException`のメッセージをRole別に分けると、デバッグが容易になります。

```java
// GlobalExceptionHandler.java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDeniedException(
    AccessDeniedException ex,
    HttpServletRequest request
) {
    String message = "この操作を実行する権限がありません";

    // より詳細なメッセージ（開発環境のみ推奨）
    if (isDevelopmentEnvironment()) {
        message = "必要なRole: " + extractRequiredRoles(request.getRequestURI());
    }

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.FORBIDDEN.value(),
        message,
        request.getRequestURI()
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
}
```

### 6. Swagger UIでのRole表示

Swagger UIでエンドポイントごとに必要なRoleを表示すると便利です。

```java
@Operation(
    description = "書籍ページコンテンツ取得",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "401", description = "未認証"),
        @ApiResponse(responseCode = "403", description = "権限不足（PREMIUM_USER以上が必要）"),
        @ApiResponse(responseCode = "404", description = "コンテンツが見つかりません")
    }
)
@GetMapping("/books/{id}/chapters/{chapter}/pages/{page}")
public ResponseEntity<BookChapterPageContentResponse> getPageContent(...) {
    // 実装
}
```

---

## 段階的実装計画

システムの成長に合わせて段階的に実装することを推奨します。

### Phase 1: 最小限の実装（現在 → 1週間）

**目的:** 基本的な権限分離の実現

**実装内容:**
- ✅ `USER` (既存のデフォルトユーザー)
- ✅ `ADMIN` (既に実装済み)

**変更箇所:**
- Keycloak: Realm Roleの作成（`USER`, `ADMIN`）
- SecurityConfig: 現状維持
- AdminUserController: 現状維持（`@PreAuthorize("hasRole('ADMIN')")`）

**テスト:**
- AdminUserControllerのテスト（401, 403のテスト）

---

### Phase 2: 有料化対応（1-2週間）

**目的:** フリーミアムモデルの実現

**実装内容:**
- ✅ `PREMIUM_USER` Roleの追加
- ✅ `/book-content/**` の認可強化
- ✅ 有料コンテンツ閲覧機能の実装

**変更箇所:**
- Keycloak: `PREMIUM_USER` Roleの作成
- SecurityConfig: `/book-content/**`の認可追加
  ```java
  .requestMatchers("/book-content/**")
      .hasAnyRole("PREMIUM_USER", "ADMIN")
  ```
- BookContentController: `@PreAuthorize`の追加

**新規機能:**
- サブスクリプション管理API（外部決済サービス連携）
- 有料コンテンツの配信

**テスト:**
- BookContentControllerのRole別テスト
- USERとPREMIUM_USERのアクセス制御テスト

---

### Phase 3: コンテンツプロバイダー対応（2-3週間）

**目的:** 著者・出版社向け機能の実装

**実装内容:**
- ✅ `AUTHOR` Roleの追加
- ✅ 書籍管理機能の強化
- ✅ 統計・アナリティクス機能

**変更箇所:**
- Keycloak: `AUTHOR` Roleの作成
- SecurityConfig: 書籍管理エンドポイントの認可追加
  ```java
  .requestMatchers(HttpMethod.POST, "/books/**")
      .hasAnyRole("AUTHOR", "ADMIN")
  .requestMatchers(HttpMethod.PUT, "/books/**")
      .hasAnyRole("AUTHOR", "ADMIN")
  ```
- BookController: `@PreAuthorize`の追加

**新規機能:**
- `/books/{id}/analytics`: 書籍統計API
- `/books/{id}/reviews/moderation`: 著者用レビュー管理
- 所有者チェックの実装（自分の書籍のみ編集可能）

**テスト:**
- 所有者チェックのテスト
- AUTHOR vs ADMIN vs USERのアクセス制御テスト

---

### Phase 4: コミュニティ管理（2-3週間）

**目的:** 健全なコミュニティの維持

**実装内容:**
- ✅ `MODERATOR` Roleの追加
- ✅ モデレーション機能の実装
- ✅ ユーザー報告機能

**変更箇所:**
- Keycloak: `MODERATOR` Roleの作成
- SecurityConfig: `/moderation/**`の認可追加
  ```java
  .requestMatchers("/moderation/**")
      .hasAnyRole("MODERATOR", "ADMIN")
  ```
- 新規Controller: `ModerationController`

**新規機能:**
- `/moderation/reports`: ユーザー報告管理API
- `/moderation/reviews/{id}`: 不適切レビュー削除
- `/moderation/books/{id}/visibility`: コンテンツの公開/非公開設定
- 通報システムの実装

**新規エンティティ:**
- `Report` (ユーザー報告)
- `ModerationLog` (モデレーション履歴)

**テスト:**
- ModerationControllerのRole別テスト
- 不適切コンテンツ削除のワークフローテスト

---

### Phase 5: 高度な権限管理（将来的な拡張）

**目的:** より細かい権限制御

**実装候補:**
- 動的なRole割り当て（API経由）
- Role階層の導入（必要に応じて）
- カスタム権限（Permission）の実装
- リソースレベルのアクセス制御（例: 特定のジャンルのみ編集可能）

---

## まとめ

### 推奨Role構成

| Role | 優先度 | 実装フェーズ | ビジネス価値 |
|------|-------|------------|------------|
| USER | 必須 | Phase 1 | ★★★★★ |
| ADMIN | 必須 | Phase 1 | ★★★★★ |
| PREMIUM_USER | 高 | Phase 2 | ★★★★☆ |
| AUTHOR | 中 | Phase 3 | ★★★☆☆ |
| MODERATOR | 中 | Phase 4 | ★★★☆☆ |

### 実装の優先順位

1. **Phase 1（最小限）**: すぐに実装可能（現状に近い）
2. **Phase 2（有料化）**: 収益化に直結（ビジネス価値が高い）
3. **Phase 3（著者向け）**: コンテンツ供給の促進
4. **Phase 4（モデレーション）**: コミュニティの健全性維持

### 技術的な選択

| 項目 | 推奨 | 理由 |
|------|------|------|
| Role種類 | **Realm Role** | シンプルで管理容易 |
| Role階層 | **使用しない** | 明示的なhasAnyRole()の方が明確 |
| 所有者チェック | **Serviceレイヤー** | ビジネスロジックとの分離 |
| テスト | **Role別テスト** | 権限漏れの防止 |

---

## 参考資料

- [Spring Security Reference - Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Keycloak Documentation - Roles](https://www.keycloak.org/docs/latest/server_admin/#assigning-permissions-and-access-using-roles-and-groups)
- [OAuth 2.0 Resource Server - JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

---

**作成日:** 2026-01-02
**バージョン:** 1.0
**対象プロジェクト:** My Books Backend (my-books-api)
