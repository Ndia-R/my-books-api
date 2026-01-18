# ロール管理機能 - 全体設計

## 概要

ユーザーのロール変更（プレミアム切り替え、スタッフ任命など）をログアウト不要でシームレスに反映する機能の全体設計。

### 対応するロール変更パターン

| 変更パターン | 実行者 | エンドポイント |
|-------------|--------|---------------|
| 一般 → プレミアム | ユーザー自身 | `/me/subscription/upgrade` |
| プレミアム → 一般 | ユーザー自身 | `/me/subscription/downgrade` |
| 任意のロール変更 | 管理者 | `/admin/users/{userId}/roles` |

## 選択したアプローチ

**アプローチA: トークン強制リフレッシュ**

- UXが最も良く、ユーザーはシームレスに新しいロールを取得
- BFFに新エンドポイント追加が必要

---

## システム全体のシーケンス

```
[Phase 1: ロール変更]
ユーザー → Frontend → BFF → my-books-api → Keycloak (ロール変更)

[Phase 2: トークンリフレッシュ]
Frontend → BFF (トークン強制リフレッシュ) → Keycloak → Redis (新トークン保存)
         ↓
Frontend (新しいロールでUI更新)
```

---

## 各コンポーネントの実装仕様

### 1. my-books-api（このリポジトリ）

#### 1-1. Keycloak Admin Client 依存関係追加

```gradle
// build.gradle
implementation 'org.keycloak:keycloak-admin-client:24.0.0'
```

#### 1-2. 新規設定クラス

**ファイル**: `src/main/java/com/example/my_books_backend/config/KeycloakAdminConfig.java`

```java
@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminConfig {
    private String serverUrl;      // https://vsv-crystal.skygroup.local/auth
    private String realm;          // sample-realm
    private String clientId;       // admin-cli または専用クライアント
    private String clientSecret;   // クライアントシークレット

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm(realm)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build();
    }
}
```

#### 1-3. サービス層

##### SubscriptionService（ユーザー自身のサブスクリプション管理）

**ファイル**: `src/main/java/com/example/my_books_backend/service/SubscriptionService.java`

```java
public interface SubscriptionService {
    /** プレミアムにアップグレード */
    SubscriptionResponse upgradeToPremium();

    /** 一般ユーザーにダウングレード */
    SubscriptionResponse downgradeToGeneral();

    /** 現在のサブスクリプション状態を取得 */
    SubscriptionStatusResponse getStatus();
}
```

##### UserRoleService（管理者による汎用ロール管理）

**ファイル**: `src/main/java/com/example/my_books_backend/service/UserRoleService.java`

```java
public interface UserRoleService {
    /** ユーザーのロール一覧を取得 */
    List<String> getUserRoles(String userId);

    /** ロールを追加 */
    void addRole(String userId, String role);

    /** ロールを削除 */
    void removeRole(String userId, String role);

    /** ロールを一括設定（置換） */
    void setRoles(String userId, List<String> roles);
}
```

**実装の共通点**:
- 両サービスとも内部で Keycloak Admin API を使用
- `SubscriptionService` は `ui:general-user` / `ui:premium-user` のみを操作
- `UserRoleService` は任意のロールを操作可能（管理者用）

#### 1-4. コントローラー

##### MeSubscriptionController（ユーザー自身用）

**ファイル**: `src/main/java/com/example/my_books_backend/controller/MeSubscriptionController.java`

**ベースパス**: `/me/subscription`

| エンドポイント | メソッド | 権限 | 説明 |
|---------------|---------|------|------|
| `/me/subscription` | GET | 認証済み | 現在のサブスクリプション状態 |
| `/me/subscription/upgrade` | POST | 認証済み | プレミアムにアップグレード |
| `/me/subscription/downgrade` | POST | 認証済み | 一般ユーザーにダウングレード |

##### AdminUserRoleController（管理者用）

**ファイル**: `src/main/java/com/example/my_books_backend/controller/AdminUserRoleController.java`

**ベースパス**: `/admin/users/{userId}/roles`

| エンドポイント | メソッド | 権限 | 説明 |
|---------------|---------|------|------|
| `/admin/users/{userId}/roles` | GET | `user:manage` | ユーザーのロール一覧取得 |
| `/admin/users/{userId}/roles` | PUT | `user:manage` | ロールを一括設定（置換） |
| `/admin/users/{userId}/roles` | POST | `user:manage` | ロールを追加 |
| `/admin/users/{userId}/roles/{role}` | DELETE | `user:manage` | 特定ロールを削除 |

##### 管理者用APIのリクエスト/レスポンス例

```json
// PUT /admin/users/{userId}/roles - ロール一括設定
// Request Body:
{
  "roles": ["ui:content-editor", "ui:general-user"]
}

// POST /admin/users/{userId}/roles - ロール追加
// Request Body:
{
  "role": "ui:moderator"
}

// GET /admin/users/{userId}/roles - ロール一覧取得
// Response:
{
  "userId": "keycloak-uuid",
  "roles": ["ui:content-editor", "ui:general-user"]
}
```

#### 1-5. application.yml への設定追加

```yaml
keycloak:
  admin:
    server-url: ${KEYCLOAK_ADMIN_SERVER_URL:https://vsv-crystal.skygroup.local/auth}
    realm: ${KEYCLOAK_ADMIN_REALM:sample-realm}
    client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
```

---

### 2. BFF（api-gateway-bff）- 別リポジトリ

#### 2-1. 新規エンドポイント

| エンドポイント | メソッド | 説明 |
|---------------|---------|------|
| `/bff/auth/refresh-session` | POST | トークンを強制リフレッシュし、新しいユーザー情報を返却 |

#### 2-2. 実装仕様

```java
@PostMapping("/bff/auth/refresh-session")
public ResponseEntity<UserInfoResponse> refreshSession(HttpServletRequest request) {
    // 1. 現在のセッションからOAuth2AuthorizedClientを取得
    // 2. Refresh Tokenを使用してKeycloakにトークンリフレッシュを要求
    // 3. 新しいAccess Token + Refresh TokenをRedisに保存
    // 4. 新しいユーザー情報（ロール含む）を返却
}
```

#### 2-3. レスポンス形式

```json
{
  "userId": "keycloak-uuid",
  "email": "user@example.com",
  "displayName": "表示名",
  "roles": ["ui:premium-user"]  // 更新後のロール
}
```

---

### 3. Frontend

#### 3-1. ユーザー自身のアップグレードフロー

```typescript
async function upgradeToPremium(): Promise<void> {
  try {
    // 1. バックエンドでロール変更
    await api.post('/api/my-books/me/subscription/upgrade');

    // 2. BFFでセッションリフレッシュ（新ロール取得）
    const response = await api.post('/bff/auth/refresh-session');

    // 3. ユーザー状態を更新
    setUserProfile(response.data);

    // 4. UIを更新（プレミアム機能を有効化）
    toast.success('プレミアム会員になりました！');
  } catch (error) {
    toast.error('アップグレードに失敗しました');
  }
}
```

#### 3-2. 管理者によるロール変更フロー（管理画面）

```typescript
// 管理者がユーザーをコンテンツ編集者に任命
async function assignContentEditor(userId: string): Promise<void> {
  try {
    // 1. ロールを追加
    await api.post(`/api/my-books/admin/users/${userId}/roles`, {
      role: 'ui:content-editor'
    });

    toast.success('コンテンツ編集者に任命しました');
  } catch (error) {
    toast.error('ロール変更に失敗しました');
  }
}

// 管理者がユーザーのロールを一括設定
async function setUserRoles(userId: string, roles: string[]): Promise<void> {
  await api.put(`/api/my-books/admin/users/${userId}/roles`, { roles });
}
```

**注意**: 管理者によるロール変更の場合、対象ユーザーが次回ログイン時または次回トークンリフレッシュ時に新しいロールが反映されます。即時反映が必要な場合は、対象ユーザーに再ログインを促すか、管理者が対象ユーザーのセッションを無効化する機能を検討してください。

---

## セキュリティ考慮事項

1. **Keycloak Admin API アクセス**
   - my-books-api からのみアクセス可能（Client Credentials Grant）
   - 専用のサービスアカウントを使用
   - 最小権限の原則（ロール変更のみ許可）

2. **エンドポイント別の権限制御**

   | エンドポイント | 制御 |
   |---------------|------|
   | `/me/subscription/*` | 認証済みユーザーが自分自身のみ操作可能 |
   | `/admin/users/{userId}/roles` | `user:manage` 権限が必要 |

3. **ロール変更の制約**
   - `/me/subscription/*` では `ui:general-user` / `ui:premium-user` のみ操作可能
   - スタッフ系ロール（`ui:content-editor`, `ui:moderator`, `ui:admin`）は管理者APIでのみ変更可能
   - 自分自身に `ui:admin` を付与することはできない（権限昇格攻撃の防止）

4. **不正なダウングレード防止**
   - サブスクリプション期間中のダウングレードは要件に応じて制御

---

## 実装順序（推奨）

### Phase 1: my-books-api の実装
1. [ ] Keycloak Admin Client 依存関係追加
2. [ ] KeycloakAdminConfig 設定クラス作成
3. [ ] UserRoleService インターフェース/実装（汎用ロール管理）
4. [ ] SubscriptionService インターフェース/実装（UserRoleServiceを内部利用）
5. [ ] MeSubscriptionController 作成
6. [ ] AdminUserRoleController 作成
7. [ ] 単体テスト作成

### Phase 2: BFF の実装（別リポジトリ）
1. [ ] `/bff/auth/refresh-session` エンドポイント追加
2. [ ] OAuth2AuthorizedClientManager でのトークンリフレッシュ実装
3. [ ] レスポンス形式の定義

### Phase 3: Frontend の実装（別リポジトリ）
1. [ ] ユーザー向けアップグレード/ダウングレード API 呼び出し
2. [ ] 管理画面でのロール管理 UI
3. [ ] セッションリフレッシュ呼び出し
4. [ ] UI状態の更新

---

## 検証方法

### my-books-api 側

**ユーザー自身のサブスクリプション変更**:
1. 一般ユーザーでログイン
2. `POST /me/subscription/upgrade` を呼び出し
3. Keycloak 管理コンソールで `ui:premium-user` ロールが追加されていることを確認

**管理者によるロール変更**:
1. 管理者（`user:manage` 権限保持）でログイン
2. `POST /admin/users/{userId}/roles` で `ui:content-editor` を追加
3. Keycloak 管理コンソールで対象ユーザーのロールが更新されていることを確認
4. 権限のないユーザーからのアクセスで 403 が返ることを確認

### 全体フロー
1. 一般ユーザーでログイン
2. プレミアムアップグレード実行
3. ログアウトせずにプレミアム機能（有料コンテンツ閲覧等）が利用可能になることを確認

---

## 備考

- トリガー（決済連携 or 管理者操作）は未定のため、両方に対応できる設計
- BFF は別リポジトリのため、この設計書を仕様として共有
