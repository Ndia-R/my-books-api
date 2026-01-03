# バックエンド実装ガイド: Keycloak新ロール設計の適用

## 1. はじめに

このドキュメントは、[keycloak/ROLE-DESIGN.md](./keycloak/ROLE-DESIGN.md)で定義された新しい権限・ロール設計を、Spring Bootバックエンドアプリケーションに適用するための実装ガイドです。

Keycloak側の設定は完了しています。次はこのバックエンドアプリケーションが、Keycloakによって発行されたJWTに含まれる権限情報を正しく解釈し、APIエンドポイントのアクセス制御を行うように改修します。

**ゴール:** アプリケーションの各APIが、新しい権限（Permission）に基づいて正しく保護され、セキュリティが強化されること。

---

## 2. 主な作業内容

作業は大きく分けて以下の2点です。

1.  `SecurityConfig.java` でのエンドポイント毎の権限設定
2.  `@PreAuthorize` を利用した、より動的なメソッドレベルの権限設定

### 2.1. `SecurityConfig.java` の更新

まず、全体的なAPIのアクセス制御を、`http.authorizeHttpRequests` を使って設定します。
現在は `/admin/**` のような大まかな設定になっていますが、これを `ROLE-DESIGN.md` で定義した、より細かい**権限（Permission）ベース**のチェックに置き換えます。

**【重要】**
Keycloakから渡される権限名（例: `book:write`）は、Spring Securityでは `ROLE_book:write` のように `ROLE_` プレフィックスが付与されたものとして扱います。`hasRole()` を使う際はプレフィックスを除いた名前 (`book:write`) を、`hasAuthority()` を使う場合は完全な名前 (`ROLE_book:write`) を指定してください。

#### 修正方針の例

```java
// SecurityConfig.java

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // ... (csrf, sessionManagementなどの設定) ...
        .authorizeHttpRequests(auth -> auth
            // --- 公開エンドポイント (変更なし) ---
            .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/genres/**").permitAll()

            // --- 認証必須エンドポイント (新しい権限ベースに修正) ---

            // ▼▼▼ 以下のように修正 ▼▼▼

            // プレミアムコンテンツ閲覧 (GET /api/book-content/**)
            .requestMatchers(HttpMethod.GET, "/api/book-content/**").hasRole("book:read:premium")

            // 書籍の作成 (POST /api/books)
            .requestMatchers(HttpMethod.POST, "/api/books").hasRole("book:write")
            
            // 書籍の更新 (PUT /api/books/{id})
            .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("book:write")

            // 書籍の削除 (DELETE /api/books/{id})
            .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("book:delete")

            // ジャンル管理
            .requestMatchers("/api/genres/**").hasRole("genre:manage")
            
            // レビュー投稿 (POST /api/reviews)
            .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("review:write:own")

            // ユーザー自身の情報更新 (PUT /api/me/profile)
            .requestMatchers(HttpMethod.PUT, "/api/me/profile").hasRole("user:read:own")
            
            // 管理者機能: ユーザー管理
            .requestMatchers("/api/admin/users/**").hasRole("user:manage:all")

            // その他の認証済みリクエスト (フォールバック)
            .anyRequest().authenticated()
        )
        // ... (oauth2ResourceServerの設定) ...
    );
    return http.build();
}
```

**※注意:** 上記はあくまで一例です。実際のエンドポイントに合わせて、`ROLE-DESIGN.md`の権限マトリックスを参照しながら適切な権限を割り当ててください。

### 2.2. メソッドレベルでの動的な権限設定

`SecurityConfig` での設定だけでは、「自分のレビューだけ削除可能」といった動的な制御は困難です。このようなケースでは、ControllerやServiceのメソッドに `@PreAuthorize` アノテーションを付与します。

#### 例1: レビューの削除

レビューの削除は、「管理者」または「モデレーター」であれば誰のレビューでも削除でき、「一般ユーザー」は自分のレビューしか削除できない、という要件になります。

```java
// ReviewController.java or ReviewService.java

// hasRole('review:delete:any') -> モデレーターや管理者が持つ権限
// @reviewSecurity.isOwner(#id, authentication) -> 実行ユーザーがそのレビューの所有者かチェックするカスタムロジック
@PreAuthorize("hasRole('review:delete:any') or @reviewSecurity.isOwner(#id, authentication)")
@DeleteMapping("/reviews/{id}")
public void deleteReview(@PathVariable Long id) {
    // ... 削除処理 ...
}
```

この `@reviewSecurity.isOwner(...)` のようなカスタム評価ロジックは、別途`@Component`として実装する必要があります。

```java
// ReviewSecurity.java

@Component("reviewSecurity")
public class ReviewSecurity {
    
    @Autowired
    private ReviewRepository reviewRepository;

    public boolean isOwner(Long reviewId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        // レビューの所有者IDと認証ユーザーのID(sub)を比較
        String currentUserId = authentication.getName(); 
        return reviewRepository.findById(reviewId)
            .map(review -> review.getUserId().equals(currentUserId))
            .orElse(false);
    }
}
```

#### 例2: 書籍の更新

書籍の更新も同様に、所有者チェックが必要です。

```java
// BookController.java or BookService.java

// hasRole('book:write') -> 編集者が持つ権限
// @bookSecurity.isOwner(#id, authentication) -> 実行ユーザーがその本の著者かチェックするロジック
@PreAuthorize("hasRole('book:write')")
@PutMapping("/books/{id}")
public void updateBook(@PathVariable String id, @RequestBody BookUpdateRequest request) {
    // ... 更新処理 ...
}
```
// `ROLE-DESIGN.md` の方針に基づき、`content-editor`は所有者に関わらず全ての書籍情報を編集できます。


### 2.3. JWTコンバーターの確認

Keycloakのレルムロール（今回作成した権限）が `ROLE_` プレフィックス付きでSpring Securityに認識されるよう、`JwtAuthenticationConverter` が正しく設定されているか確認してください。

```java
// SecurityConfig.java

@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    // Keycloakのアクセストークン内の `realm_access.roles` クレームを権限として使用する
    grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles"); 
    // `ROLE_` プレフィックスを自動付与する
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); 

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
}
```

---

## 3. テストの追加

権限設定を変更した後は、必ずテストを追加・更新してください。`@WithMockUser` を使うと、特定の権限を持つモックユーザーでテストを実行できます。

```java
// BookControllerTest.java

@Test
@WithMockUser(roles = {"book:write"}) // book:write権限を持つユーザーでテスト
void whenCreateBookWithWritePermission_thenSucceeds() throws Exception {
    // 書籍作成APIの呼び出し
    mockMvc.perform(post("/api/books")...).andExpect(status().isCreated());
}

@Test
@WithMockUser(roles = {"user"}) // 一般ユーザー権限では失敗するはず
void whenCreateBookWithoutWritePermission_thenIsForbidden() throws Exception {
    // 書籍作成APIの呼び出し
    mockMvc.perform(post("/api/books")...).andExpect(status().isForbidden());
}
```

---

## 4. 参考資料

- **最重要:** [keycloak/ROLE-DESIGN.md](./keycloak/ROLE-DESIGN.md)
  - 全ての役割（Role）と権限（Permission）の定義が記載されています。実装の際は必ずこちらを正としてください。
- [Spring Security Docs: Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Docs: JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

ご不明な点があれば、お気軽にご質問ください。