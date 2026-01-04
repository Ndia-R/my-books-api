# my-books アプリケーション ロール設計

## 1. 設計方針: 「役割」と「権限」の分離

本設計では、Keycloak管理者の提案に基づき、ロールを**「基本Role（権限）」**と**「Composite Role（役割）」**の2層構造で管理します。

| 要素                      | 用途                                 | 例                                       |
| :------------------------ | :----------------------------------- | :--------------------------------------- |
| **基本Role (Permission)** | 「何ができるか」という最小単位の権限 | `book:read:premium`, `review:delete:any` |
| **Composite Role (Role)** | 職務や立場に応じた権限のセット       | `premium-user`, `moderator`              |

これにより、アプリケーション開発者（バックエンド・フロントエンド）は、`user` や `admin` といった分かりやすい**役割（Role）**のみを意識すればよくなります。一方、権限の詳細な管理はKeycloak上で**権限（Permission）**の組み合わせによって実現でき、責務の分離とメンテナンス性の向上に繋がります。

---

## 2. 役割 (Composite Roles)

アプリケーションで利用する主な役割として、以下の5つを定義します。

| 役割 (Role)          | 説明                          | 想定ユーザー                                                             |
| :------------------- | :---------------------------- | :----------------------------------------------------------------------- |
| **`user`**           | **一般ユーザー** (デフォルト) | 無料登録したすべてのユーザー。書籍の閲覧や自身のレビュー管理が可能。     |
| **`premium-user`**   | **有料会員**                  | 月額課金ユーザー。有料コンテンツの閲覧など、追加機能へのアクセスが可能。 |
| **`content-editor`** | **コンテンツ編集者**          | 書籍のメタデータやジャンルを管理するスタッフ。                           |
| **`moderator`**      | **コミュニティ管理者**        | 不適切なレビューの削除など、コミュニティの健全性を維持するスタッフ。     |
| **`admin`**          | **システム管理者**            | すべての権限を持つシステム管理者。ユーザー管理やシステム設定も可能。     |

### `content-editor` ロールに関する補足

バックエンド案の `AUTHOR`（著者）とフロントエンド案の `EDITOR`（編集者）を `content-editor` に統合しました。

- **`content-editor` は、書籍情報を編集する権限を持ちます。**
- 「自分の書籍だけ編集可能」といった**所有者ベースの制御**は、このロールで行うのではなく、**バックエンドのAPI実装で制御する**ことを強く推奨します。

```java
// 例: BookService.java
public void updateBook(String bookId, User currentUser) {
    Book book = bookRepository.findById(bookId);

    // 管理者でもなく、本の所有者でもない場合はエラー
    if (!currentUser.hasRole("admin") && !book.getAuthorId().equals(currentUser.getId())) {
        throw new ForbiddenException("この書籍を編集する権限がありません");
    }
    // ...更新処理
}
```

これにより、ロールの種類をシンプルに保ったまま、柔軟なアクセス制御を実現できます。

---

## 3. 権限 (Basic Roles / Permissions)

役割（Composite Role）を構成する最小単位の権限です。命名規則は `{リソース}:{操作}` の形式を基本とします。

| 権限 (Permission)            | 説明                                               |
| :--------------------------- | :------------------------------------------------- |
| **書籍関連**                 |                                                    |
| `book:read`                  | 全ての書籍情報を閲覧する権限                       |
| `book:read:premium`          | 有料コンテンツを閲覧する権限                       |
| `book:write`                 | 書籍情報を新規作成・編集する権限                   |
| `book:delete`                | 書籍を削除する権限                                 |
| **レビュー関連**             |                                                    |
| `review:write:own`           | 自身のレビューを投稿・編集する権限                 |
| `review:delete:any`          | 他人のものを含む、すべてのレビューを削除する権限   |
| **お気に入り・ブックマーク** |                                                    |
| `favorite:manage`            | 自身のお気に入りを管理する権限                     |
| `bookmark:manage`            | 自身のブックマークを管理する権限                   |
| **ジャンル管理**             |                                                    |
| `genre:manage`               | ジャンルを新規作成・編集・削除する権限             |
| **ユーザー管理**             |                                                    |
| `user:read:own`              | 自身のプロフィール情報を閲覧・編集する権限         |
| `user:manage:all`            | 全ユーザーの情報を管理（閲覧・編集・削除）する権限 |

---

## 4. 権限マトリックス

各役割（Composite Role）にどの権限（Permission）が含まれるかを以下に示します。

| 権限                | `user` | `premium-user` | `content-editor` | `moderator` | `admin` |
| :------------------ | :----: | :------------: | :--------------: | :---------: | :-----: |
| `book:read`         |   ✅   |       ✅       |        ✅        |     ✅      |   ✅    |
| `book:read:premium` |        |       ✅       |        ✅        |     ✅      |   ✅    |
| `book:write`        |        |                |        ✅        |             |   ✅    |
| `book:delete`       |        |                |                  |             |   ✅    |
| `review:write:own`  |   ✅   |       ✅       |        ✅        |     ✅      |   ✅    |
| `review:delete:any` |        |                |                  |     ✅      |   ✅    |
| `favorite:manage`   |   ✅   |       ✅       |        ✅        |     ✅      |   ✅    |
| `bookmark:manage`   |   ✅   |       ✅       |        ✅        |     ✅      |   ✅    |
| `genre:manage`      |        |                |        ✅        |             |   ✅    |
| `user:read:own`     |   ✅   |       ✅       |        ✅        |     ✅      |   ✅    |
| `user:manage:all`   |        |                |                  |             |   ✅    |

- **`admin`** は、上記すべての権限を内包します。
- **`user`** は、新規登録時のデフォルトロールとして設定します。

---

## 5. 実装ガイドライン

### バックエンド (Spring Security)

- APIのエンドポイント保護には、役割（Role）ではなく、**権限（Permission）** を直接指定することを推奨します。これにより、APIが必要とする権限が明確になります。
- Spring Securityの `hasRole()` や `@PreAuthorize` を利用します。Keycloakの権限名は `ROLE_` プレフィックス付きで解釈されます (例: `ROLE_book:write`)。

```java
// SecurityConfig.java
.requestMatchers(HttpMethod.POST, "/api/books").hasRole("book:write")
.requestMatchers(HttpMethod.GET, "/api/book-content/**").hasRole("book:read:premium")

// ReviewController.java
@DeleteMapping("/reviews/{id}")
@PreAuthorize("hasRole('review:delete:any') or @reviewSecurity.isOwner(#id, authentication)")
public void deleteReview(@PathVariable Long id) { ... }
```

### フロントエンド (React)

- UI要素（ボタン、メニューなど）の表示/非表示の制御に役割（Role）を使用します。
- `useAuth` のようなカスタムフックで、ユーザーが持つ役割を簡単に判定できるようにします。

```typescript
// useRole.ts
import { useAuth } from '@/providers/auth-provider';

export const useRole = () => {
  const { roles } = useAuth(); // Keycloakのトークンから取得した役割リスト

  const isAdmin = () => roles.includes('admin');
  const isModerator = () => roles.includes('admin') || roles.includes('moderator');
  const isContentEditor = () => roles.includes('admin') || roles.includes('content-editor');

  return { isAdmin, isModerator, isContentEditor };
};

// Component.tsx
const { isContentEditor } = useRole();
// ...
{isContentEditor() && <Button>書籍を編集</Button>}
```

**重要**: フロントエンドでの表示制御はあくまでUI/UX向上のためです。**最終的なアクセス可否の判断は、必ずバックエンドのAPIで行う必要があります。**

---

## 6. Group機能の活用（応用編）

ここまでの設計はロールだけでも十分に機能しますが、ユーザー数が多くなったり、組織的な運用が始まったりした場合には、**Group機能**を活用するとユーザー管理が格段に効率化します。

### Groupとは？

Groupは、ユーザーを「組織」や「チーム」といった集団で管理するための機能です。

- **Role** が「何ができるか（権限）」を定義するのに対し、
- **Group** は「誰がどこに所属しているか（所属）」を表現します。

Groupに特定の役割（Composite Role）を紐付けておくことで、ユーザーをGroupに追加・移動するだけで、自動的にロールが付与・変更される仕組みを構築できます。

### my-booksにおけるGroup設計例

以下に`my-books`アプリケーションのためのGroup構造案を示します。

```
/
├── Staff (運営チーム)
│   ├── Admins (システム管理者)
│   ├── Content Editors (コンテンツ編集チーム)
│   └── Moderators (コミュニティ管理チーム)
│
└── Users (一般ユーザー)
    ├── Premium Users (有料会員)
    └── Standard Users (一般会員)
```

### GroupとRoleの紐付け

| Group                    | 紐付ける役割 (Composite Role) |
| :----------------------- | :---------------------------- |
| `/Staff/Admins`          | `admin`                       |
| `/Staff/Content Editors` | `content-editor`              |
| `/Staff/Moderators`      | `moderator`                   |
| `/Users/Premium Users`   | `premium-user`                |
| `/Users/Standard Users`  | `user`                        |

### Group活用のメリット: 役割変更の簡素化

例えば、あるスタッフが「コンテンツ編集者」から「コミュニティ管理者」に異動になった場合、管理者が行う作業は以下の通りです。

1.  対象ユーザーの所属を `/Staff/Content Editors` Group から外す。
2.  対象ユーザーを `/Staff/Moderators` Group に追加する。

これだけで、Keycloakは自動的に古い `content-editor` ロールを剥奪し、新しい `moderator` ロールを付与します。個別のロールを付け替える人的ミスを防ぎ、管理を大幅に簡素化できます。

ユーザーの役割変更や昇格・降格が頻繁に発生する可能性がある場合は、このGroup設計の導入を強く推奨します。
