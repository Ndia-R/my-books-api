# My Books アプリケーション ロール設計提案書

## 📋 目次

- [現状分析](#現状分析)
- [推奨ロール設計](#推奨ロール設計)
  - [基本構成（シンプル版）](#基本構成シンプル版)
  - [拡張構成（将来性を考慮）](#拡張構成将来性を考慮)
- [Keycloakでの実装方法](#keycloakでの実装方法)
- [実装上の考慮事項](#実装上の考慮事項)
- [推奨アプローチ](#推奨アプローチ)

---

## 現状分析

### アプリケーション概要

**My Books** は、React 19 + TypeScript + Vite で構築された日本語の書籍発見・読書プラットフォーム。

### 現在の機能と権限

#### 誰でもできること（未認証ユーザー）
- 書籍の閲覧
- 書籍の検索
- レビューの閲覧

#### 認証ユーザーのみ
- お気に入りの追加・削除
- しおりの作成・更新・削除
- レビューの投稿・編集・削除
- プロフィールの編集

#### 現在の権限チェック実装
- レビューの編集・削除は投稿者本人のみ（`userProfile?.id === review.userId`）
- フロントエンド側でのユーザーIDベースのチェック

### 課題

- ロールベースのアクセス制御（RBAC）が未実装
- 管理者権限が定義されていない
- 不適切なコンテンツへの対応フローが不明確
- 将来的な機能拡張（プレミアムプラン等）への対応が未定義

---

## 推奨ロール設計

### 基本構成（シンプル版）

**現時点での推奨構成** - YAGNI原則に基づき、必要最小限のロールのみ定義

#### ロール一覧

| ロール名 | 説明 | 対象ユーザー |
|---------|------|------------|
| `USER` | 一般ユーザー | 認証済みの全ユーザー（デフォルト） |
| `ADMIN` | システム管理者 | システム管理者のみ |

#### USER（一般ユーザー）

**対象**: 認証済みの一般ユーザー（デフォルトロール）

**権限**:
- ✅ 書籍の閲覧・検索
- ✅ 書籍の全文読書
- ✅ お気に入りの追加・削除
- ✅ しおりの作成・更新・削除
- ✅ レビューの投稿・編集・削除（**自分のもののみ**）
- ✅ プロフィールの編集

**制限**:
- ❌ 他人のレビューの削除
- ❌ ユーザー管理
- ❌ 書籍データの編集
- ❌ システム設定の変更

#### ADMIN（システム管理者）

**対象**: システム管理者のみ

**権限**:
- ✅ **USERの全権限**
- ✅ 全てのレビューの削除（不適切コンテンツ対応）
- ✅ ユーザー管理（アカウント停止・削除等）
- ✅ 書籍データの追加・編集・削除
- ✅ システム設定の変更
- ✅ ログの閲覧

---

### 拡張構成（将来性を考慮）

将来的な機能拡張を見越した詳細なロール設計

#### ロール一覧

| ロール名 | 説明 | 対象ユーザー | 優先度 |
|---------|------|------------|--------|
| `GUEST` | ゲストユーザー | 未認証ユーザー | - |
| `USER` | 一般ユーザー | 認証済みの全ユーザー | 必須 |
| `PREMIUM_USER` | プレミアムユーザー | 有料プラン加入者 | 将来 |
| `MODERATOR` | モデレーター | コンテンツ管理者 | 中期 |
| `EDITOR` | 編集者 | 書籍コンテンツ編集者 | 中期 |
| `ADMIN` | システム管理者 | システム管理者 | 必須 |

#### GUEST（ゲストユーザー）

**対象**: 未認証ユーザー

**権限**:
- ✅ 書籍の閲覧・検索（制限付き）
- ✅ レビューの閲覧

**制限**:
- ❌ 書籍の全文閲覧
- ❌ お気に入り・しおり機能
- ❌ レビューの投稿

#### USER（一般ユーザー）

**対象**: 認証済みの一般ユーザー（デフォルトロール）

**権限**:
- ✅ **GUESTの全権限**
- ✅ 書籍の全文閲覧
- ✅ お気に入りの追加・削除
- ✅ しおりの作成・更新・削除
- ✅ レビューの投稿・編集・削除（**自分のもののみ**）
- ✅ プロフィールの編集

#### PREMIUM_USER（プレミアムユーザー）

**対象**: 有料プラン加入者

**権限**:
- ✅ **USERの全権限**
- ✅ 広告非表示
- ✅ オフライン読書機能
- ✅ 高度な検索機能（詳細フィルタリング等）
- ✅ レビューの優先表示
- ✅ 限定コンテンツへのアクセス

**用途**: 将来的なマネタイズ戦略

#### MODERATOR（モデレーター）

**対象**: コンテンツモデレーションチーム

**権限**:
- ✅ **USERの全権限**
- ✅ 不適切なレビューの削除
- ✅ ユーザーレポートの確認・対応
- ✅ コンテンツの品質管理
- ✅ ユーザーへの警告通知

**制限**:
- ❌ ユーザーアカウントの削除（ADMINのみ）
- ❌ 書籍データの編集（EDITORまたはADMINのみ）

**用途**: コミュニティ管理、不適切コンテンツ対応

#### EDITOR（編集者）

**対象**: 書籍コンテンツ編集チーム

**権限**:
- ✅ **USERの全権限**
- ✅ 書籍データの追加・編集
- ✅ ジャンルの管理
- ✅ 特集ページの作成・編集
- ✅ 書籍メタデータの更新

**制限**:
- ❌ ユーザー管理
- ❌ レビューの削除（MODERATORまたはADMINのみ）

**用途**: コンテンツ管理の分業化

#### ADMIN（システム管理者）

**対象**: システム管理者

**権限**:
- ✅ **全ての権限**
- ✅ ユーザー管理（作成・編集・削除・ロール割り当て）
- ✅ ロール管理
- ✅ システム設定の変更
- ✅ ログの閲覧
- ✅ データベース管理
- ✅ セキュリティ設定

---

## Keycloakでの実装方法

### Realm Rolesの設定

#### 基本構成の場合

```
Realm: my-books-realm

Realm Roles:
├─ user          (デフォルトロール)
└─ admin
```

#### 拡張構成の場合

```
Realm: my-books-realm

Realm Roles:
├─ guest         (未認証用、参考)
├─ user          (デフォルトロール)
├─ premium_user
├─ moderator
├─ editor
└─ admin

Client Roles (my-books-frontend):
└─ (必要に応じて細かい権限を定義)
```

### デフォルトロールの設定

新規ユーザー登録時に自動的に `user` ロールを付与する設定：

1. Keycloak Admin Console にログイン
2. **Realm Settings** → **User Registration** → **Default Roles**
3. `user` ロールを追加

### 複合ロール（Composite Roles）の活用

管理者に全権限を付与する例：

```
admin (composite role)
├─ user
├─ premium_user
├─ moderator
└─ editor
```

**設定方法**:
1. Keycloak Admin Console → **Roles** → **admin**
2. **Composite Roles** タブ
3. `user`, `premium_user`, `moderator`, `editor` を追加

**メリット**: `admin` ロールを付与するだけで全ての権限が自動的に付与される

### Keycloakトークンへのロール埋め込み

Access TokenのClaimsに `roles` を含める設定：

1. Keycloak Admin Console → **Client Scopes** → **roles**
2. **Mappers** タブ → **realm roles**
3. **Token Claim Name**: `roles`
4. **Claim JSON Type**: `String`
5. **Add to access token**: `ON`

これにより、JWTアクセストークンに以下のようなClaimが含まれます：

```json
{
  "roles": ["user", "premium_user"],
  "sub": "12345-67890-abcdef",
  "email": "user@example.com"
}
```

---

## 実装上の考慮事項

### 1. フロントエンドでのロールチェック

#### UserProfile型の拡張

現在の `UserProfile` 型にロール情報を追加：

```typescript
// src/types/domain/user.ts
export type UserProfile = {
  id: number;
  displayName: string;
  avatarPath: string;
  username: string;
  email: string;
  familyName: string;
  givenName: string;
  roles: string[]; // 追加
};
```

#### カスタムフックでのロールチェック

```typescript
// src/hooks/use-role.ts
import { useAuth } from '@/providers/auth-provider';

export const useRole = () => {
  const { userProfile } = useAuth();

  const hasRole = (role: string): boolean => {
    return userProfile?.roles?.includes(role) ?? false;
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  };

  const hasAllRoles = (roles: string[]): boolean => {
    return roles.every(role => hasRole(role));
  };

  // 便利メソッド
  const isAdmin = () => hasRole('admin');
  const isModerator = () => hasRole('moderator') || isAdmin();
  const isEditor = () => hasRole('editor') || isAdmin();
  const isPremiumUser = () => hasRole('premium_user') || isAdmin();

  return {
    hasRole,
    hasAnyRole,
    hasAllRoles,
    isAdmin,
    isModerator,
    isEditor,
    isPremiumUser,
  };
};
```

#### 使用例

```typescript
// コンポーネント内での使用
import { useRole } from '@/hooks/use-role';

export default function BookReviewItem({ review }: Props) {
  const { userProfile } = useAuth();
  const { isAdmin, isModerator } = useRole();

  // 自分のレビューまたは管理者・モデレーターなら編集可能
  const canEdit = userProfile?.id === review.userId;
  const canDelete = canEdit || isModerator();

  return (
    <div>
      {canEdit && <Button onClick={handleEdit}>編集</Button>}
      {canDelete && <Button onClick={handleDelete}>削除</Button>}
    </div>
  );
}
```

#### ルートガードでの使用

```typescript
// src/routes/admin-route.tsx
import { useRole } from '@/hooks/use-role';
import { Navigate } from 'react-router';

export default function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAdmin } = useRole();

  if (!isAdmin()) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
```

### 2. バックエンドでの権限チェック

#### Spring Securityでの実装例

```java
// APIサイドの例
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    // 自分のレビューまたは管理者・モデレーターのみ削除可能
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or @reviewSecurity.isOwner(#reviewId, authentication)")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // 編集者以上のみ書籍データを編集可能
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @PutMapping("/books/{bookId}")
    public ResponseEntity<BookDetails> updateBook(
            @PathVariable String bookId,
            @RequestBody BookUpdateRequest request) {
        BookDetails updated = bookService.updateBook(bookId, request);
        return ResponseEntity.ok(updated);
    }
}
```

#### カスタムセキュリティ式の実装

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

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        String username = authentication.getName();
        return review.getUsername().equals(username);
    }
}
```

### 3. BFFでのロール変換

BFFがKeycloakからトークンを取得した際、ロール情報をフロントエンド用のレスポンスに含める：

```java
// BFFのAuthControllerでの実装例
@GetMapping("/user-info")
public ResponseEntity<UserProfile> getUserInfo(OAuth2AuthenticationToken authentication) {
    // Keycloakからユーザー情報を取得
    OAuth2User oauth2User = authentication.getPrincipal();

    // ロール情報を抽出
    List<String> roles = extractRoles(oauth2User);

    // UserProfileレスポンスを構築
    UserProfile profile = UserProfile.builder()
        .id(getUserId(oauth2User))
        .displayName(oauth2User.getAttribute("name"))
        .username(oauth2User.getAttribute("preferred_username"))
        .email(oauth2User.getAttribute("email"))
        .roles(roles) // ロール情報を追加
        .build();

    return ResponseEntity.ok(profile);
}

private List<String> extractRoles(OAuth2User oauth2User) {
    // Keycloakのトークンからロール情報を抽出
    Map<String, Object> realmAccess = oauth2User.getAttribute("realm_access");
    if (realmAccess != null && realmAccess.containsKey("roles")) {
        return (List<String>) realmAccess.get("roles");
    }
    return Collections.emptyList();
}
```

### 4. セキュリティのベストプラクティス

#### フロントエンドでの注意点

⚠️ **重要**: フロントエンドのロールチェックは **UI制御のみ** に使用

- ✅ ボタンの表示/非表示
- ✅ メニュー項目の表示/非表示
- ✅ ルーティングガード

❌ **セキュリティ上の制約ではない** - 必ずバックエンドでも検証が必要

#### バックエンドでの注意点

✅ **必須**: 全てのAPIエンドポイントで権限チェックを実装

```java
// 悪い例 ❌
@DeleteMapping("/{reviewId}")
public void deleteReview(@PathVariable Long reviewId) {
    // 権限チェックなし！
    reviewService.deleteReview(reviewId);
}

// 良い例 ✅
@PreAuthorize("hasRole('ADMIN') or @reviewSecurity.isOwner(#reviewId, authentication)")
@DeleteMapping("/{reviewId}")
public void deleteReview(@PathVariable Long reviewId) {
    reviewService.deleteReview(reviewId);
}
```

### 5. テスト戦略

#### フロントエンドテスト

```typescript
// use-role.test.ts
import { renderHook } from '@testing-library/react';
import { useRole } from '@/hooks/use-role';

describe('useRole', () => {
  it('should return true for admin role', () => {
    // モックAuthProviderでroles: ['admin']を提供
    const { result } = renderHook(() => useRole(), { wrapper: MockAuthProvider });
    expect(result.current.isAdmin()).toBe(true);
  });

  it('should return false for user without admin role', () => {
    // モックAuthProviderでroles: ['user']を提供
    const { result } = renderHook(() => useRole(), { wrapper: MockAuthProvider });
    expect(result.current.isAdmin()).toBe(false);
  });
});
```

#### バックエンドテスト

```java
@Test
@WithMockUser(roles = "USER")
void testDeleteReview_asNonOwner_shouldThrowAccessDenied() {
    assertThrows(AccessDeniedException.class, () -> {
        reviewController.deleteReview(123L);
    });
}

@Test
@WithMockUser(roles = "ADMIN")
void testDeleteReview_asAdmin_shouldSucceed() {
    assertDoesNotThrow(() -> {
        reviewController.deleteReview(123L);
    });
}
```

---

## 推奨アプローチ

### 現時点での推奨: 基本構成（シンプル版）

**採用すべきロール**:
- `USER` (デフォルト)
- `ADMIN`

### 理由

1. **YAGNI原則** (You Aren't Gonna Need It)
   - 現在必要な機能のみ実装
   - 過度な設計を避ける

2. **シンプルさ**
   - 管理が容易
   - バグが少ない
   - メンテナンスコストが低い

3. **拡張性**
   - 必要に応じて後から追加可能
   - Keycloakの設定変更だけで対応可能

### 拡張のタイミング

以下の機能が必要になった際に、拡張構成に移行：

#### PREMIUM_USER ロールの追加
- 有料プランの導入が決定
- マネタイズ戦略の開始

#### MODERATOR ロールの追加
- ユーザー数の増加
- 不適切コンテンツの増加
- コンテンツモデレーションチームの編成

#### EDITOR ロールの追加
- 書籍データ管理の分業化
- 編集チームの編成
- コンテンツ更新頻度の増加

### 実装ロードマップ

#### Phase 1: 基本実装（現在）
- [ ] `USER` と `ADMIN` ロールの定義
- [ ] Keycloakでのロール設定
- [ ] フロントエンドでの `useRole` フック実装
- [ ] バックエンドでの基本的な権限チェック
- [ ] `UserProfile` 型への `roles` フィールド追加

#### Phase 2: セキュリティ強化（1-2ヶ月後）
- [ ] 全APIエンドポイントでの権限チェック徹底
- [ ] ロールベーステストの追加
- [ ] セキュリティ監査

#### Phase 3: 機能拡張（必要に応じて）
- [ ] `MODERATOR` ロールの追加
- [ ] `EDITOR` ロールの追加
- [ ] `PREMIUM_USER` ロールの追加
- [ ] ロール管理UIの実装

---

## まとめ

### キーポイント

1. **現時点では「基本構成」で十分**
   - `USER` と `ADMIN` の2ロールのみ

2. **フロントエンドとバックエンドの両方で権限チェックが必須**
   - フロントエンド: UI制御のみ
   - バックエンド: セキュリティ制約

3. **Keycloakの機能を最大限活用**
   - Realm Roles
   - Composite Roles
   - Default Roles
   - Token Claims

4. **段階的な拡張が可能**
   - 必要に応じてロールを追加
   - 既存システムへの影響を最小化

### 次のステップ

1. **基本構成の実装を開始**
2. **バックエンドAPIでのロールチェック実装**
3. **フロントエンドでの`useRole`フック実装**
4. **テストの追加**
5. **ドキュメントの整備**

---

## 参考資料

- [Keycloak Documentation - Authorization Services](https://www.keycloak.org/docs/latest/authorization_services/)
- [Spring Security - Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [OWASP - Role-Based Access Control](https://owasp.org/www-community/Access_Control)

---

**作成日**: 2026-01-02
**対象アプリケーション**: My Books
**認証基盤**: Keycloak
**バージョン**: 1.0
