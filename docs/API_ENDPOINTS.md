# My Books Backend - API エンドポイント設計ガイド（新人研修向け）

## 目次
1. [概要とRESTful設計](#概要とrestful設計)
2. [学習の進め方](#学習の進め方)
3. [認証システム（認証不要）](#認証システム認証不要)
4. [パブリック読み取り API](#パブリック読み取りapi)
5. [認証必要システム](#認証必要システム)
6. [ユーザー操作API](#ユーザー操作api)
7. [管理者専用API](#管理者専用api)
8. [Spring Boot実装パターン](#spring-boot実装パターン)

---

## 概要とRESTful設計

### システム概要
My Books Backend APIは、RESTful設計原則に基づいて構築された書籍管理システムです。認証レベルに応じて段階的にアクセス可能な機能を提供します。

### RESTful設計原則

#### 1. リソース指向設計
```
良い例:
GET /books          # 書籍一覧
GET /books/123      # 特定の書籍
POST /books         # 書籍作成
PUT /books/123      # 書籍更新
DELETE /books/123   # 書籍削除

悪い例:
GET /getBooks       # 動詞を使用
POST /book-create   # 動作指向
```

#### 2. HTTPメソッドの適切な使用
| メソッド | 用途 | 例 |
|---------|------|---|
| GET | リソースの取得 | `GET /books` |
| POST | リソースの作成 | `POST /reviews` |
| PUT | リソースの更新 | `PUT /reviews/123` |
| DELETE | リソースの削除 | `DELETE /favorites/123` |

#### 3. ステータスコードの統一
| コード | 意味 | 使用場面 |
|--------|------|---------|
| 200 | OK | GET, PUT の成功 |
| 201 | Created | POST の成功 |
| 204 | No Content | DELETE の成功 |
| 400 | Bad Request | バリデーションエラー |
| 401 | Unauthorized | 認証エラー |
| 403 | Forbidden | 認可エラー |
| 404 | Not Found | リソースが見つからない |

#### 4. URLの階層構造
```
書籍関連:
/books                    # 書籍一覧
/books/{id}              # 特定の書籍
/books/{id}/reviews      # 書籍のレビュー一覧
/books/{id}/stats        # 書籍の統計情報

ユーザー操作:
/me/profile              # 自分のプロフィール
/me/reviews              # 自分のレビュー一覧
/me/favorites            # 自分のお気に入り一覧
```

#### 5. レスポンス形式の統一
```json
{
  "data": [...],           // 実際のデータ（業界標準）
  "totalElements": 100,    // ページネーション情報
  "totalPages": 5,
  "currentPage": 1,
  "pageSize": 20
}
```

---

## 学習の進め方

### 学習ステップ
1. **認証システム** - ログイン・サインアップ（認証の基本）
2. **パブリックAPI** - 書籍・ジャンル情報（認証不要の読み取り）
3. **認証必要API** - 有料コンテンツ、ユーザー操作
4. **CRUD操作** - レビュー・お気に入り・ブックマーク
5. **管理者API** - 高度な権限管理

### 各ステップで学ぶこと
- **HTTP基礎** - メソッド、ステータスコード、ヘッダー
- **認証・認可** - JWT、Spring Security
- **バリデーション** - リクエストデータの検証
- **エラーハンドリング** - 例外処理とレスポンス設計
- **ページネーション** - 大量データの効率的な取得

---

## 認証システム（認証不要）

### 概要
ユーザーのログイン・サインアップ・ログアウト・トークン更新を担当します。

### AuthController (`/`)

#### POST /login
```http
POST /login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**レスポンス:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "name": "山田太郎",
    "email": "user@example.com"
  }
}
```

**学習ポイント:**
- **JWT認証**: アクセストークンとリフレッシュトークン（Cookie）
- **パスワードハッシュ**: BCryptでの暗号化検証
- **Spring Security**: 認証プロセスとの統合

#### POST /signup
```http
POST /signup
Content-Type: application/json

{
  "name": "山田太郎",
  "email": "user@example.com", 
  "password": "password123"
}
```

**学習ポイント:**
- **バリデーション**: `@Valid` による入力検証
- **重複チェック**: メールアドレスの一意性確認
- **自動ログイン**: 登録後の即座なログイン

#### POST /logout
```http
POST /logout
```

**学習ポイント:**
- **Cookie削除**: リフレッシュトークンの無効化
- **セキュリティ**: サーバーサイドでのトークン管理

#### POST /refresh-token
```http
POST /refresh-token
Cookie: refreshToken=...
```

**学習ポイント:**
- **トークン更新**: アクセストークンの自動更新
- **セキュリティ**: リフレッシュトークンの検証

### Spring Boot実装例
```java
@RestController
@RequestMapping("")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        AccessTokenResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(loginResponse);
    }
}
```

---

## パブリック読み取りAPI

### 概要
認証不要で書籍情報やジャンル情報を取得できるAPIです。

### BookController (`/books`)

#### GET /books/new-releases
最新書籍10冊を取得

```http
GET /books/new-releases
```

**レスポンス:**
```json
{
  "data": [
    {
      "id": "book123",
      "title": "プログラミング入門",
      "authors": ["山田太郎"],
      "averageRating": 4.5,
      "reviewCount": 120
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "currentPage": 1,
  "pageSize": 10
}
```

#### GET /books/search
タイトル検索

```http
GET /books/search?q=プログラミング&page=1&size=20&sort=popularity.desc
```

**パラメータ:**
- `q`: 検索キーワード（必須）
- `page`: ページ番号（1ベース、デフォルト: 1）
- `size`: ページサイズ（デフォルト: 20）
- `sort`: ソート条件（デフォルト: popularity.desc）

**学習ポイント:**
- **クエリパラメータ**: `@RequestParam` の使用
- **デフォルト値**: パラメータのデフォルト設定
- **ページネーション**: 大量データの効率的な取得

#### GET /books/discover
ジャンル検索

```http
GET /books/discover?genreIds=1,2&condition=AND&page=1&size=20
```

**パラメータ:**
- `genreIds`: ジャンルIDのカンマ区切り（必須）
- `condition`: 検索条件（SINGLE/AND/OR、必須）

**学習ポイント:**
- **複数値パラメータ**: カンマ区切り文字列の処理
- **検索条件**: AND/OR検索の実装
- **JOINクエリ**: 複数テーブルの結合

#### GET /books/{id}
書籍詳細

```http
GET /books/afcIMuetDuzj
```

**レスポンス:**
```json
{
  "id": "afcIMuetDuzj",
  "title": "湖畔の永遠",
  "description": "美しい湖畔を舞台にした感動のラブストーリー",
  "authors": ["田中美咲"],
  "genres": [
    {"id": 3, "name": "ロマンス"}
  ],
  "averageRating": 4.2,
  "reviewCount": 45
}
```

#### GET /books/{id}/toc
書籍の目次

```http
GET /books/afcIMuetDuzj/toc
```

**学習ポイント:**
- **パスパラメータ**: `@PathVariable` の使用
- **階層データ**: 章構造の表現

#### GET /books/{id}/reviews
書籍のレビュー一覧

```http
GET /books/afcIMuetDuzj/reviews?page=1&size=3&sort=updatedAt.desc
```

#### GET /books/{id}/stats
書籍の統計情報

```http
GET /books/afcIMuetDuzj/stats
```

**レスポンス:**
```json
{
  "bookId": "afcIMuetDuzj",
  "reviewStats": {
    "totalCount": 45,
    "averageRating": 4.2,
    "ratingDistribution": {
      "5": 20,
      "4": 15,
      "3": 8,
      "2": 2,
      "1": 0
    }
  },
  "favoriteStats": {
    "totalCount": 123
  }
}
```

### GenreController (`/genres`)

#### GET /genres
全ジャンル取得

```http
GET /genres
```

**学習ポイント:**
- **マスターデータ**: ジャンル情報の管理
- **キャッシュ**: 頻繁にアクセスされるデータの最適化

---

## 認証必要システム

### 概要
JWT認証が必要なエンドポイント群です。

### BookContentController (`/content/books`)

#### GET /content/books/{id}/chapters/{chapter}/pages/{page}
書籍ページコンテンツ（有料機能）

```http
GET /content/books/afcIMuetDuzj/chapters/1/pages/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**レスポンス:**
```json
{
  "bookId": "afcIMuetDuzj",
  "chapterNumber": 1,
  "pageNumber": 1,
  "content": "第一章の実際の内容がここに表示されます...",
  "hasNextPage": true,
  "hasPreviousPage": false
}
```

**学習ポイント:**
- **認証必要**: JWT トークンによる認証
- **有料コンテンツ**: `/content/**` パターンでの分離
- **複数パスパラメータ**: 階層的なリソース指定

### UserController (`/me`)

#### GET /me/profile
ユーザープロフィール

```http
GET /me/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**レスポンス:**
```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "user@example.com",
  "avatarPath": "/avatar01.png",
  "createdAt": "2024-01-15T10:30:00"
}
```

**学習ポイント:**
- **認証ユーザー**: `@AuthenticationPrincipal User user`
- **プロフィール管理**: ユーザー情報の取得

#### GET /me/profile-counts
ユーザーの活動統計

```http
GET /me/profile-counts
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**レスポンス:**
```json
{
  "reviewCount": 5,
  "favoriteCount": 12,
  "bookmarkCount": 8
}
```

#### PUT /me/profile
プロフィール更新

```http
PUT /me/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "name": "山田次郎",
  "avatarPath": "/avatar02.png"
}
```

**学習ポイント:**
- **データ更新**: PUT メソッドの使用
- **バリデーション**: 更新データの検証

---

## ユーザー操作API

### 概要
認証されたユーザーが書籍に対して行う操作（レビュー・お気に入り・ブックマーク）です。

### ReviewController (`/reviews`)

#### POST /reviews
レビュー作成

```http
POST /reviews
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "bookId": "afcIMuetDuzj",
  "rating": 4.5,
  "comment": "とても感動的な作品でした"
}
```

**レスポンス:**
```json
{
  "id": 123,
  "userId": 1,
  "book": {
    "id": "afcIMuetDuzj",
    "title": "湖畔の永遠"
  },
  "rating": 4.5,
  "comment": "とても感動的な作品でした",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**学習ポイント:**
- **201 Created**: リソース作成時のステータスコード
- **Location ヘッダー**: 作成されたリソースのURL
- **非同期処理**: 書籍統計の更新

#### PUT /reviews/{id}
レビュー更新

```http
PUT /reviews/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "bookId": "afcIMuetDuzj",
  "rating": 5.0,
  "comment": "再読して、さらに素晴らしさを実感しました"
}
```

#### DELETE /reviews/{id}
レビュー削除

```http
DELETE /reviews/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**学習ポイント:**
- **204 No Content**: 削除成功時のステータスコード
- **権限チェック**: 自分のレビューのみ削除可能

### FavoriteController (`/favorites`)

#### POST /favorites
お気に入り追加

```http
POST /favorites
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "bookId": "afcIMuetDuzj"
}
```

**学習ポイント:**
- **シンプルなリクエスト**: bookIdのみのペイロード
- **重複防止**: 同じ書籍の重複お気に入りを防ぐ

#### DELETE /favorites/{id}
お気に入り削除

```http
DELETE /favorites/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### BookmarkController (`/bookmarks`)

#### POST /bookmarks
ブックマーク追加

```http
POST /bookmarks
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "pageContentId": 456,
  "note": "この場面が印象的でした"
}
```

**レスポンス:**
```json
{
  "id": 789,
  "userId": 1,
  "book": {
    "id": "afcIMuetDuzj",
    "title": "湖畔の永遠"
  },
  "chapterNumber": 3,
  "pageNumber": 5,
  "chapterTitle": "運命の出会い",
  "note": "この場面が印象的でした",
  "createdAt": "2024-01-15T10:30:00"
}
```

**学習ポイント:**
- **階層データ**: 書籍→章→ページの関係
- **動的データ取得**: 章タイトルの自動付与
- **ユーザーメモ**: 任意のメモ機能

#### PUT /bookmarks/{id}
ブックマーク更新

```http
PUT /bookmarks/789
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "pageContentId": 456,
  "note": "何度読んでも感動します"
}
```

---

## 管理者専用API

### 概要
ADMIN ロールを持つユーザーのみアクセス可能なエンドポイントです。

### AdminUserController (`/admin/users`)

#### GET /admin/users
全ユーザー取得

```http
GET /admin/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**学習ポイント:**
- **@PreAuthorize**: メソッドレベルでの認可制御
- **ロールベース認可**: ADMIN ロールのチェック

#### DELETE /admin/users/{id}
ユーザー削除

```http
DELETE /admin/users/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### RoleController (`/roles`)

管理者がロール管理を行うエンドポイント群（GET/POST/PUT/DELETE）

---

## Spring Boot実装パターン

### 1. Controller設計の基本パターン

#### RESTController の基本構成
```java
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Book", description = "書籍")
public class BookController {
    private final BookService bookService;
    
    @GetMapping("")
    public ResponseEntity<PageResponse<BookResponse>> getBooks() {
        // 実装
    }
}
```

**学習ポイント:**
- **@RestController**: JSON レスポンスの自動変換
- **@RequestMapping**: ベースURLの設定
- **@RequiredArgsConstructor**: 依存性注入の簡素化
- **@Tag**: OpenAPI/Swagger ドキュメント生成

#### パラメータハンドリング
```java
@GetMapping("/search")
public ResponseEntity<PageResponse<BookResponse>> search(
    @RequestParam String q,                                     // 必須パラメータ
    @RequestParam(defaultValue = "1") Long page,               // デフォルト値
    @RequestParam(required = false) String bookId              // 任意パラメータ
) {
    // 実装
}
```

#### バリデーションパターン
```java
@PostMapping("")
public ResponseEntity<ReviewResponse> create(
    @Valid @RequestBody ReviewRequest request,
    @AuthenticationPrincipal User user
) {
    // @Valid により自動バリデーション
    ReviewResponse response = reviewService.create(request, user);
    return ResponseEntity.created(location).body(response);
}
```

### 2. 認証・認可パターン

#### JWT認証の実装
```java
@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);
        if (jwtUtils.validateToken(token)) {
            Authentication auth = jwtUtils.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

#### 認可の実装パターン
```java
// クラスレベル認可
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    // 全メソッドでADMIN権限が必要
}

// メソッドレベル認可
@PreAuthorize("hasRole('USER')")
@GetMapping("/me/profile")
public ResponseEntity<UserProfileResponse> getProfile() {
    // USER権限が必要
}
```

### 3. エラーハンドリングパターン

#### グローバル例外ハンドラ
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
        return ResponseEntity.status(400).body(error);
    }
}
```

### 4. レスポンス設計パターン

#### 統一されたレスポンス構造
```java
// 成功レスポンス
{
  "data": [...],             // データ
  "totalElements": 100,      // 総件数
  "totalPages": 5,           // 総ページ数
  "currentPage": 1,          // 現在ページ
  "pageSize": 20             // ページサイズ
}

// エラーレスポンス
{
  "errorCode": "NOT_FOUND",
  "message": "書籍が見つかりません",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 5. セキュリティ設定パターン

#### Spring Security設定
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // パブリックエンドポイント
                .requestMatchers("/login", "/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/genres/**").permitAll()
                
                // 認証必要エンドポイント
                .requestMatchers("/content/**").authenticated()
                .requestMatchers("/me/**").authenticated()
                
                // 管理者専用
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 学習の次のステップ

### 1. 実装順序の推奨
1. **基本Controller作成** - Book, Genre（GET のみ）
2. **認証機能実装** - Auth Controller
3. **パブリックAPI** - 書籍検索・詳細取得
4. **認証必要API** - User Controller
5. **CRUD操作** - Review, Favorite, Bookmark
6. **管理者機能** - Admin Controller

### 2. 発展的なトピック
- **OpenAPI/Swagger**: API ドキュメント自動生成
- **フィルタリング**: 複雑な検索条件の実装
- **キャッシュ**: @Cacheable による性能向上
- **非同期処理**: @Async による統計更新
- **監査ログ**: API アクセスの記録

### 3. テスト戦略
- **@WebMvcTest**: Controller 層の単体テスト
- **MockMvc**: HTTPリクエストのシミュレーション
- **@WithMockUser**: 認証済みユーザーのテスト
- **統合テスト**: @SpringBootTest による完全なテスト

### 4. 実践的なAPI設計Tips

#### ページネーションの実装
```java
@GetMapping("")
public ResponseEntity<PageResponse<BookResponse>> getBooks(
    @RequestParam(defaultValue = "1") Long page,
    @RequestParam(defaultValue = "20") Long size,
    @RequestParam(defaultValue = "popularity.desc") String sort
) {
    // 1ベースページングをJPA 0ベースに変換
    Pageable pageable = PageableUtils.of(page - 1, size, sort);
    Page<Book> books = bookRepository.findAll(pageable);
    
    // PageResponse に変換（dataプロパティを使用）
    return ResponseEntity.ok(PageableUtils.toPageResponse(books, bookMapper::toResponse));
}
```

#### エラーレスポンスの統一
```java
public class ErrorResponse {
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    
    // コンストラクタ、getter/setter
}
```

#### バリデーションの実装
```java
public class ReviewRequest {
    @NotBlank(message = "書籍IDは必須です")
    private String bookId;
    
    @DecimalMin(value = "0.0", message = "評価は0.0以上である必要があります")
    @DecimalMax(value = "5.0", message = "評価は5.0以下である必要があります")
    private BigDecimal rating;
    
    @Size(max = 1000, message = "コメントは1000文字以下である必要があります")
    private String comment;
}
```

---

## まとめ

このAPI設計を理解することで、現代的なRESTful APIの開発スキルが体系的に身につきます。

### 重要な学習ポイント
1. **RESTful設計**: リソース指向とHTTPメソッドの適切な使用
2. **段階的認証**: パブリック→認証→管理者のアクセス制御
3. **エラーハンドリング**: 統一されたエラーレスポンス
4. **Spring Security**: JWT認証と認可の実装
5. **API設計**: 一貫性のあるエンドポイント設計
6. **業界標準準拠**: `data`プロパティによるレスポンス形式の統一

各エンドポイントの実装を通じて、Spring Bootでの本格的なWeb API開発の技術を身につけることができます。わからない部分があれば、いつでも質問してください！