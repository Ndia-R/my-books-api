# my-booksアプリ用 Role設計提案

## 目次
- [現状分析](#現状分析)
- [提案するRole設計](#提案するrole設計)
- [基本Role（権限の最小単位）](#基本role権限の最小単位)
- [Composite Role（職務ベースの権限セット）](#composite-role職務ベースの権限セット)
- [Group設計案（オプション）](#group設計案オプション)
- [権限マトリックス](#権限マトリックス)
- [実装の優先順位](#実装の優先順位)
- [実装方法](#実装方法)

---

## 現状分析

### my-booksアプリの概要

**アーキテクチャ**:
- フロントエンド: React 19 + TypeScript + Vite
- バックエンド: Spring Boot 3.3.5 + Java 21
- データベース: MySQL 8.0
- 認証: Keycloak 26.3.3 (OAuth2/OIDC)

**主な機能**:
1. 書籍管理（閲覧、検索、詳細表示）
2. レビュー機能（投稿、編集、削除）
3. お気に入り機能（ブックマーク）
4. しおり機能（ページレベルのブックマーク + コメント）
5. ユーザープロフィール管理
6. 統計・ランキング機能

### 現在のRole設計

現在は以下の単純なRole設計になっています：

| Role | 説明 | 権限 |
|------|------|------|
| `ADMIN` | 管理者 | ユーザー管理機能にアクセス可能 |
| 暗黙的な一般ユーザー | 認証済みユーザー全員 | レビュー、お気に入り、ブックマーク機能を使用可能 |

### 現在のセキュリティ設定

**SecurityConfig による細かい制御**:

| エンドポイント | メソッド | 認証要件 | 備考 |
|---|---|---|---|
| `/books/**` | GET | 不要 | 書籍情報は公開 |
| `/genres/**` | GET | 不要 | ジャンル情報は公開 |
| `/book-content/**` | GET | 必要 | 有料コンテンツ（認証ユーザーのみ） |
| `/reviews` | POST/PUT/DELETE | 必要 | ユーザーのレビュー操作 |
| `/favorites` | POST/DELETE | 必要 | お気に入り追加・削除 |
| `/bookmarks` | POST/PUT/DELETE | 必要 | ブックマーク管理 |
| `/me/**` | GET/PUT | 必要 | ユーザープロフィール管理 |
| `/admin/users/**` | ALL | ADMIN | 管理者機能 |

---

## 提案するRole設計

### 設計方針

1. **基本Role**: 権限の最小単位（「何ができるか」を明確に定義）
2. **Composite Role**: 複数の基本Roleを組み合わせた職務ベースの権限セット
3. **Group**: 組織構造の表現（オプション）

### メリット

- ✅ **保守性**: 権限を細かく分けることで、後から調整しやすい
- ✅ **拡張性**: 新しい役割が増えても、Composite Roleを追加するだけ
- ✅ **明確性**: Role名から「何ができるか」が一目瞭然
- ✅ **柔軟性**: 個別ユーザーに特別な権限を追加することも可能
- ✅ **段階的導入**: Phase 1（最小構成）→ Phase 2（機能拡張）→ Phase 3（運営体制強化）と段階的に実装可能

---

## 基本Role（権限の最小単位）

### 1. データアクセス権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `book:read:all` | 全書籍の閲覧権限 | 書籍一覧・詳細表示 |
| `book:read:premium` | プレミアム書籍の閲覧権限 | 有料コンテンツへのアクセス |
| `book:write` | 書籍の追加・編集権限 | 管理者・編集者向け |
| `book:delete` | 書籍の削除権限 | 管理者のみ |

### 2. レビュー権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `review:create` | レビュー投稿権限 | 一般ユーザー |
| `review:edit:own` | 自分のレビュー編集権限 | 一般ユーザー |
| `review:delete:own` | 自分のレビュー削除権限 | 一般ユーザー |
| `review:delete:any` | 全レビュー削除権限 | モデレーター・管理者 |
| `review:verify` | レビュー承認権限 | モデレーター向け（将来の機能拡張） |

### 3. お気に入り・ブックマーク権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `favorite:manage` | お気に入り管理権限 | 一般ユーザー |
| `bookmark:manage` | ブックマーク管理権限 | 一般ユーザー |

### 4. ユーザー管理権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `user:view:all` | 全ユーザー情報閲覧権限 | 管理者 |
| `user:edit:own` | 自分のプロフィール編集権限 | 一般ユーザー |
| `user:edit:any` | 全ユーザー編集権限 | 管理者 |
| `user:delete` | ユーザー削除権限 | 管理者 |

### 5. 統計・分析権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `analytics:view` | 統計情報閲覧権限 | 管理者・アナリスト |
| `analytics:export` | 統計データエクスポート権限 | 管理者 |

### 6. ジャンル管理権限

| Role名 | 説明 | 用途 |
|--------|------|------|
| `genre:manage` | ジャンル管理権限 | 管理者・編集者 |

---

## Composite Role（職務ベースの権限セット）

### 一般ユーザー向け

#### 1. `reader` (読者)

**説明**: 基本的な書籍閲覧とレビュー投稿が可能な一般ユーザー

**含まれるRole**:
- `book:read:all`
- `review:create`
- `review:edit:own`
- `review:delete:own`
- `favorite:manage`
- `bookmark:manage`
- `user:edit:own`

**想定ユーザー**: 無料会員、一般的な読者ユーザー

**Access Token例**:
```json
{
  "realm_access": {
    "roles": [
      "book:read:all",
      "review:create",
      "review:edit:own",
      "review:delete:own",
      "favorite:manage",
      "bookmark:manage",
      "user:edit:own"
    ]
  }
}
```

---

#### 2. `premium_reader` (プレミアム読者)

**説明**: 有料コンテンツへのアクセスも可能なプレミアム会員

**含まれるRole**:
- `book:read:all`
- `book:read:premium` ⭐ 追加
- `review:create`
- `review:edit:own`
- `review:delete:own`
- `favorite:manage`
- `bookmark:manage`
- `user:edit:own`

**想定ユーザー**: 課金ユーザー、サブスクリプション会員

**readerとの違い**:
- ✅ プレミアム書籍（有料コンテンツ）へのアクセスが可能

---

### 運営スタッフ向け

#### 3. `content_editor` (コンテンツ編集者)

**説明**: 書籍の追加・編集が可能（削除は不可）

**含まれるRole**:
- `book:read:all`
- `book:read:premium`
- `book:write` ⭐ 追加
- `genre:manage` ⭐ 追加
- `review:create`
- `review:edit:own`
- `review:delete:own`
- `favorite:manage`
- `bookmark:manage`
- `user:edit:own`

**想定ユーザー**: 出版社スタッフ、コンテンツ管理者

**premium_readerとの違い**:
- ✅ 書籍の追加・編集が可能
- ✅ ジャンルの管理が可能
- ❌ 書籍の削除は不可（安全性のため）

---

#### 4. `moderator` (モデレーター)

**説明**: レビューの管理と承認が可能なコミュニティマネージャー

**含まれるRole**:
- `book:read:all`
- `book:read:premium`
- `review:create`
- `review:edit:own`
- `review:delete:own`
- `review:delete:any` ⭐ 追加
- `review:verify` ⭐ 追加
- `favorite:manage`
- `bookmark:manage`
- `user:edit:own`

**想定ユーザー**: コミュニティマネージャー、カスタマーサポート

**premium_readerとの違い**:
- ✅ 他人のレビューを削除可能（不適切なレビューの削除）
- ✅ レビューの承認が可能（将来の機能拡張用）

---

#### 5. `analyst` (アナリスト)

**説明**: 統計情報の閲覧とエクスポートが可能なデータアナリスト

**含まれるRole**:
- `book:read:all`
- `book:read:premium`
- `analytics:view` ⭐ 追加
- `analytics:export` ⭐ 追加
- `user:edit:own`

**想定ユーザー**: データアナリスト、マーケティング担当者

**特徴**:
- ✅ 統計情報の閲覧とエクスポートが可能
- ❌ レビュー投稿やお気に入り機能は使用しない（業務用アカウント）

---

### 管理者向け

#### 6. `admin` (管理者)

**説明**: 全機能へのフルアクセス権限を持つシステム管理者

**含まれるRole**:
- `book:read:all`
- `book:read:premium`
- `book:write`
- `book:delete` ⭐ 追加
- `review:create`
- `review:edit:own`
- `review:delete:own`
- `review:delete:any`
- `review:verify`
- `favorite:manage`
- `bookmark:manage`
- `user:view:all` ⭐ 追加
- `user:edit:own`
- `user:edit:any` ⭐ 追加
- `user:delete` ⭐ 追加
- `analytics:view`
- `analytics:export`
- `genre:manage`

**想定ユーザー**: システム管理者

**特徴**:
- ✅ すべての機能にアクセス可能
- ✅ 書籍の削除が可能
- ✅ 全ユーザーの管理が可能

---

## Group設計案（オプション）

組織的な運用をする場合は、Groupを使って管理することもできます。

```
/my-books
├── /readers (一般読者)
│   ├── /free (無料会員) → reader
│   └── /premium (有料会員) → premium_reader
│
├── /staff (運営スタッフ)
│   ├── /editors (編集者) → content_editor
│   ├── /moderators (モデレーター) → moderator
│   └── /analysts (アナリスト) → analyst
│
└── /admins (管理者) → admin
```

### Groupを使うメリット

- ユーザーをGroupに追加するだけで、自動的にComposite Roleが付与される
- 組織構造が明確になる
- 大量のユーザーを管理しやすい

---

## 権限マトリックス

| 機能 | reader | premium_reader | content_editor | moderator | analyst | admin |
|------|:------:|:--------------:|:--------------:|:---------:|:-------:|:-----:|
| 無料書籍閲覧 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 有料書籍閲覧 | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 書籍追加・編集 | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| 書籍削除 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| レビュー投稿 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| 自分のレビュー編集 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| 他人のレビュー削除 | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| レビュー承認 | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| お気に入り・ブックマーク | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| ジャンル管理 | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| ユーザー管理 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| 統計閲覧・エクスポート | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |

### 凡例
- ✅ 可能
- ❌ 不可

---

## 実装の優先順位

### Phase 1（最小構成） - 必須

**目的**: 現在のシステムをRole-Based Access Controlに移行

**実装するRole**:
1. `reader` (一般ユーザー)
2. `admin` (管理者)

**基本Role**:
- `book:read:all`
- `review:create`, `review:edit:own`, `review:delete:own`
- `favorite:manage`, `bookmark:manage`
- `user:edit:own`
- `user:view:all`, `user:edit:any`, `user:delete`
- `book:write`, `book:delete`
- `genre:manage`

**サンプルユーザー**:
- `user01` (reader)
- `admin01` (admin)

**想定期間**: 1週間

---

### Phase 2（機能拡張） - 推奨

**目的**: 有料会員機能と編集者機能の追加

**実装するRole**:
3. `premium_reader` (プレミアム会員)
4. `content_editor` (編集者)

**追加の基本Role**:
- `book:read:premium`

**サンプルユーザー**:
- `premium01` (premium_reader)
- `editor01` (content_editor)

**想定期間**: 1週間

---

### Phase 3（運営体制強化） - オプション

**目的**: コミュニティ管理とデータ分析の強化

**実装するRole**:
5. `moderator` (モデレーター)
6. `analyst` (アナリスト)

**追加の基本Role**:
- `review:delete:any`, `review:verify`
- `analytics:view`, `analytics:export`

**サンプルユーザー**:
- `moderator01` (moderator)
- `analyst01` (analyst)

**想定期間**: 1週間

---

## 実装方法

### 1. production.json への追加

既存の `production.json` に以下を追加します。

#### 基本Role（Phase 1）

```json
{
  "name": "book:read:all",
  "description": "Permission to read all books"
},
{
  "name": "review:create",
  "description": "Permission to create reviews"
},
{
  "name": "review:edit:own",
  "description": "Permission to edit own reviews"
},
{
  "name": "review:delete:own",
  "description": "Permission to delete own reviews"
},
{
  "name": "favorite:manage",
  "description": "Permission to manage favorites"
},
{
  "name": "bookmark:manage",
  "description": "Permission to manage bookmarks"
},
{
  "name": "user:edit:own",
  "description": "Permission to edit own profile"
},
{
  "name": "user:view:all",
  "description": "Permission to view all users"
},
{
  "name": "user:edit:any",
  "description": "Permission to edit any user"
},
{
  "name": "user:delete",
  "description": "Permission to delete users"
},
{
  "name": "book:write",
  "description": "Permission to create and edit books"
},
{
  "name": "book:delete",
  "description": "Permission to delete books"
},
{
  "name": "genre:manage",
  "description": "Permission to manage genres"
}
```

#### Composite Role（Phase 1）

```json
{
  "name": "reader",
  "description": "Basic reader role (Composite Role)",
  "composite": true,
  "composites": {
    "realm": [
      "book:read:all",
      "review:create",
      "review:edit:own",
      "review:delete:own",
      "favorite:manage",
      "bookmark:manage",
      "user:edit:own"
    ]
  }
},
{
  "name": "admin",
  "description": "Administrator role with full access (Composite Role)",
  "composite": true,
  "composites": {
    "realm": [
      "book:read:all",
      "review:create",
      "review:edit:own",
      "review:delete:own",
      "favorite:manage",
      "bookmark:manage",
      "user:edit:own",
      "user:view:all",
      "user:edit:any",
      "user:delete",
      "book:write",
      "book:delete",
      "genre:manage"
    ]
  }
}
```

#### Group（Phase 1）

```json
"groups": [
  {
    "name": "my-books",
    "path": "/my-books",
    "subGroups": [
      {
        "name": "readers",
        "path": "/my-books/readers",
        "subGroups": [
          {
            "name": "free",
            "path": "/my-books/readers/free",
            "realmRoles": ["reader"]
          }
        ]
      },
      {
        "name": "admins",
        "path": "/my-books/admins",
        "realmRoles": ["admin"]
      }
    ]
  }
]
```

#### サンプルユーザー（Phase 1）

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "username": "user01",
  "enabled": true,
  "email": "user01@example.com",
  "emailVerified": true,
  "firstName": "User",
  "lastName": "01",
  "credentials": [
    {
      "type": "password",
      "value": "Test@1234",
      "temporary": false
    }
  ],
  "groups": ["/my-books/readers/free"],
  "realmRoles": [
    "default-roles-production",
    "offline_access",
    "uma_authorization",
    "user"
  ],
  "clientRoles": {
    "account": ["manage-account", "view-profile"]
  }
},
{
  "id": "22222222-2222-2222-2222-222222222222",
  "username": "admin01",
  "enabled": true,
  "email": "admin01@example.com",
  "emailVerified": true,
  "firstName": "Admin",
  "lastName": "01",
  "credentials": [
    {
      "type": "password",
      "value": "Test@1234",
      "temporary": false
    }
  ],
  "groups": ["/my-books/admins"],
  "realmRoles": [
    "default-roles-production",
    "offline_access",
    "uma_authorization",
    "user"
  ],
  "clientRoles": {
    "account": ["manage-account", "view-profile"]
  }
}
```

---

### 2. Spring Boot側の実装

#### SecurityConfig の更新

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            // 公開エンドポイント
            .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/genres/**").permitAll()

            // プレミアムコンテンツ（Phase 2で実装）
            .requestMatchers(HttpMethod.GET, "/book-content/**")
                .hasAnyRole("book:read:premium", "admin")

            // レビュー機能
            .requestMatchers(HttpMethod.POST, "/reviews")
                .hasRole("review:create")
            .requestMatchers(HttpMethod.PUT, "/reviews/**")
                .hasRole("review:edit:own")
            .requestMatchers(HttpMethod.DELETE, "/reviews/**")
                .hasAnyRole("review:delete:own", "review:delete:any", "admin")

            // お気に入り・ブックマーク
            .requestMatchers("/favorites/**")
                .hasRole("favorite:manage")
            .requestMatchers("/bookmarks/**")
                .hasRole("bookmark:manage")

            // プロフィール管理
            .requestMatchers(HttpMethod.GET, "/me/**")
                .hasRole("user:edit:own")
            .requestMatchers(HttpMethod.PUT, "/me/**")
                .hasRole("user:edit:own")

            // 管理者機能
            .requestMatchers("/admin/users/**")
                .hasAnyRole("user:view:all", "admin")
            .requestMatchers(HttpMethod.POST, "/admin/books/**")
                .hasRole("book:write")
            .requestMatchers(HttpMethod.DELETE, "/admin/books/**")
                .hasRole("book:delete")
            .requestMatchers("/admin/genres/**")
                .hasRole("genre:manage")

            // その他は認証必須
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

    return http.build();
}
```

#### JwtAuthenticationConverter の設定

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();

    // Keycloak の realm_access.roles を抽出
    grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter jwtAuthenticationConverter =
        new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        grantedAuthoritiesConverter
    );

    return jwtAuthenticationConverter;
}
```

---

### 3. フロントエンド側の実装

#### 権限チェックのユーティリティ関数

```typescript
// src/lib/permissions.ts

export interface UserRoles {
  realm_access: {
    roles: string[];
  };
}

export const hasRole = (user: UserRoles | null, role: string): boolean => {
  if (!user) return false;
  return user.realm_access.roles.includes(role);
};

export const hasAnyRole = (
  user: UserRoles | null,
  roles: string[]
): boolean => {
  if (!user) return false;
  return roles.some(role => user.realm_access.roles.includes(role));
};

// 便利な権限チェック関数
export const canReadPremiumBooks = (user: UserRoles | null): boolean => {
  return hasAnyRole(user, ['book:read:premium', 'admin']);
};

export const canEditBook = (user: UserRoles | null): boolean => {
  return hasAnyRole(user, ['book:write', 'admin']);
};

export const canDeleteBook = (user: UserRoles | null): boolean => {
  return hasAnyRole(user, ['book:delete', 'admin']);
};

export const canDeleteAnyReview = (user: UserRoles | null): boolean => {
  return hasAnyRole(user, ['review:delete:any', 'admin']);
};

export const canManageUsers = (user: UserRoles | null): boolean => {
  return hasAnyRole(user, ['user:view:all', 'admin']);
};
```

#### コンポーネントでの使用例

```typescript
// src/components/BookDetail.tsx

import { useUser } from '@/contexts/UserContext';
import { canReadPremiumBooks, canEditBook } from '@/lib/permissions';

export function BookDetail({ book }) {
  const { user } = useUser();

  return (
    <div>
      <h1>{book.title}</h1>

      {/* プレミアムコンテンツの表示制御 */}
      {book.isPremium && !canReadPremiumBooks(user) && (
        <div className="premium-alert">
          このコンテンツはプレミアム会員限定です
        </div>
      )}

      {/* 編集ボタンの表示制御 */}
      {canEditBook(user) && (
        <button onClick={handleEdit}>編集</button>
      )}
    </div>
  );
}
```

---

### 4. Keycloakの再起動

```bash
# production.json を更新後、Keycloakを再起動
cd /home/ubuntu/vsv-crystal
docker-compose restart keycloak
```

---

## テスト方法

### 1. Keycloak Admin Consoleでの確認

1. Keycloak Admin Consoleにログイン
   ```
   https://vsv-emerald.skygroup.local/auth/admin
   ```

2. `production-realm` を選択

3. Roleの確認:
   - `Realm roles` → 基本Roleが追加されていることを確認
   - Composite Roleをクリック → 含まれるRoleを確認

4. Groupの確認:
   - `Groups` → `/my-books` 配下のGroupを確認

5. Userの確認:
   - `Users` → `user01` をクリック
   - `Groups` タブ → `/my-books/readers/free` に所属していることを確認
   - `Role mappings` タブ → `reader` Roleが付与されていることを確認

---

### 2. Access Tokenの確認

#### Postman でのテスト

**POST** `https://vsv-emerald.skygroup.local/auth/realms/production-realm/protocol/openid-connect/token`

**Body (x-www-form-urlencoded)**:
```
grant_type: password
client_id: api-gateway-bff-client
client_secret: your-client-secret
username: user01
password: Test@1234
scope: openid profile email
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 900
}
```

**Access Tokenをデコード** (https://jwt.io):
```json
{
  "sub": "11111111-1111-1111-1111-111111111111",
  "preferred_username": "user01",
  "email": "user01@example.com",
  "realm_access": {
    "roles": [
      "book:read:all",
      "review:create",
      "review:edit:own",
      "review:delete:own",
      "favorite:manage",
      "bookmark:manage",
      "user:edit:own"
    ]
  }
}
```

---

### 3. APIエンドポイントのテスト

#### 一般ユーザー（user01）でのテスト

```bash
# Access Tokenを取得
ACCESS_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# 書籍一覧取得（成功）
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://vsv-emerald.skygroup.local/api/books

# レビュー投稿（成功）
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "rating": 5, "comment": "Great book!"}' \
  https://vsv-emerald.skygroup.local/api/reviews

# ユーザー管理（失敗 - 403 Forbidden）
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://vsv-emerald.skygroup.local/api/admin/users
```

#### 管理者（admin01）でのテスト

```bash
# Access Tokenを取得
ACCESS_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# ユーザー管理（成功）
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://vsv-emerald.skygroup.local/api/admin/users

# 書籍削除（成功）
curl -X DELETE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://vsv-emerald.skygroup.local/api/admin/books/1
```

---

## まとめ

### 設計の特徴

1. **細粒度の権限管理**: 基本Roleで「何ができるか」を明確に定義
2. **職務ベースのComposite Role**: 一般ユーザー、プレミアム会員、編集者、モデレーター、アナリスト、管理者の6つの役割
3. **段階的導入**: Phase 1（最小構成）→ Phase 2（機能拡張）→ Phase 3（運営体制強化）
4. **柔軟性**: 個別ユーザーに特別な権限を追加することも可能
5. **保守性**: 権限の調整が容易

### 次のステップ

1. production.json にRole、Group、サンプルユーザーを追加
2. Keycloakを再起動してRealmを再インポート
3. Spring Boot側のSecurityConfigを更新
4. フロントエンド側の権限チェックを実装
5. テストを実施

---

## 関連ドキュメント

- [ROLE-DESIGN.md](./ROLE-DESIGN.md) - production-realmの基本的なRole設計
- [README-realms.md](./README-realms.md) - Keycloak 3-Realm Architecture
- [production.json](./import/production.json) - production-realmの完全な定義
