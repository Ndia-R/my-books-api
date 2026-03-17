# 📚 My Books Backend

Spring Boot 3.3.5 + Java 21で構築された書籍管理REST APIバックエンド

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-OAuth%202.0-red.svg)](https://www.keycloak.org/)

## 🎯 概要

**My Books Backend** は、VPS2台構成のマルチアプリケーションアーキテクチャの一部として動作する、エンタープライズグレードの書籍管理REST APIです。Keycloak統合による強固なセキュリティ、BFF経由のアクセス制御、包括的なテスト体制を備えたプロダクションレディなアプリケーションです。

### 主要機能

- 📖 **書籍管理**: 詳細情報、章・ページコンテンツ、目次管理
- ⭐ **レビューシステム**: 評価、コメント、統計情報
- ❤️ **お気に入り機能**: ブックマーク、お気に入り登録
- 🔐 **認証・認可**: Keycloak統合（OAuth 2.0 / OpenID Connect）
- 🏷️ **ジャンル管理**: 多対多関係、AND/OR検索
- 👤 **ユーザー管理**: プロフィール、レビュー履歴、統計情報
- 🛡️ **セキュリティ**: JWT認証、Role-Based Access Control (RBAC)

## 🏗️ アーキテクチャ

### VPS2台構成での役割分離

```
VPS1 (vsv-crystal.skygroup.local)
├─ Keycloak (認証プロバイダー)
└─ Docker Registry (イメージ管理)

VPS2 (vsv-emerald.skygroup.local)
├─ Nginx Edge Proxy
├─ Frontend (SPA)
├─ BFF (認証ゲートウェイ)
├─ my-books-api (本アプリケーション) ← ここ
└─ MySQL 8.0
```

詳細は [system-architecture-overview-vps2.md](docs/system-architecture-overview-vps2.md) を参照してください。

### レイヤーアーキテクチャ

```
Controller → Service → Repository → Entity
          ↓
        DTO ← Mapper (MapStruct)
```

## 🚀 クイックスタート（新規メンバー向け）

### 前提条件

- Docker & Docker Compose
- Git

> ローカルで直接起動する場合はさらに Java 21 と Gradle 8.x が必要ですが、通常はコンテナ内での開発を推奨します。

---

### ステップ1: リポジトリのクローン

```bash
git clone <repository-url>
cd my-books-backend
```

---

### ステップ2: 外部Dockerネットワークの作成（初回のみ）

このプロジェクトは `vsv-emerald-network` という外部Dockerネットワークを使用します。初回のみ作成が必要です。

```bash
docker network create vsv-emerald-network
```

> すでに存在する場合はエラーになりますが、そのまま進めて問題ありません。

---

### ステップ3: Docker Secretsファイルの作成

データベースのパスワードはDocker Secretsで管理します。**`secrets/` ディレクトリはGit管理外**のため、手動で作成が必要です。

```bash
mkdir -p secrets
# 開発環境用のパスワードを設定（チームで共有している値を使用）
echo -n "your_db_password" > secrets/my_books_db_password
echo -n "your_db_root_password" > secrets/my_books_db_root_password
```

> ⚠️ パスワードの実際の値はチームメンバーに確認してください。`secrets/` ディレクトリは `.gitignore` 対象のため、コミットしないでください。

---

### ステップ4: 環境変数ファイルの設定

```bash
cp .env.example .env
```

`.env` を開き、`IDP_ISSUER_URI` を実際のKeycloak環境のURLに変更してください。

```bash
# .env の主要設定
IDP_ISSUER_URI=https://vsv-crystal.skygroup.local/auth/realms/sample-realm
MY_BOOKS_DB_NAME=my-books-db
MY_BOOKS_DB_URL=jdbc:mysql://my-books-db:3306/my-books-db
MY_BOOKS_DB_USER=user
```

---

### ステップ5: CA証明書について

`certs/rootCA.pem` はリポジトリに含まれており、Dockerビルド時に自動的にJavaトラストストアへインポートされます。VPS1（vsv-crystal）の自己署名証明書に対応するために必要です。特別な操作は不要です。

---

### ステップ6: Docker環境の起動

```bash
docker-compose up -d
```

初回はDockerイメージのビルドが行われるため、数分かかります。

起動確認：

```bash
docker-compose ps
```

`my-books-db` と `my-books-api` の両方が `Up` になっていればOKです。

---

### ステップ7: アプリケーションの起動

`my-books-api` コンテナは起動後に待機状態（`sleep infinity`）になります。コンテナ内に入ってアプリを起動してください。

```bash
# コンテナ内に入る
docker-compose exec my-books-api bash

# コンテナ内でアプリケーションを起動
./gradlew bootRun
```

---

### ステップ8: 動作確認

ブラウザまたはcurlで以下にアクセスして確認します。

| URL | 説明 |
|-----|------|
| http://localhost:8080/actuator/health | ヘルスチェック（`{"status":"UP"}` が返れば正常） |
| http://localhost:8080/swagger-ui.html | Swagger UI（API一覧） |
| http://localhost:8080/books/new-releases | 書籍一覧（認証不要） |

---

## 🧪 テスト

### 全テスト実行

```bash
# コンテナ内で実行
./gradlew test
```

### テスト種別

```bash
# 単体テストのみ（Docker内Dockerなし）
./gradlew test --tests "*ControllerTest"
./gradlew test --tests "*ServiceTest"

# 統合テスト（Testcontainers使用 - Docker必須）
./gradlew test --tests "*RepositoryTest"
```

### テストカバレッジ（約85%）

- **リポジトリテスト**: Testcontainers (MySQL 8.0) で統合テスト
- **コントローラーテスト**: MockMvc + Spring Security Test
- **サービステスト**: Mockito単体テスト
- **ユーティリティテスト**: PageableUtils、JwtClaimExtractor等

詳細は [CLAUDE.md](CLAUDE.md) の「テスト構造」セクションを参照してください。

## 📦 ビルドとデプロイ

### JARファイルの生成

```bash
# コンテナ内で実行
./gradlew bootJar
# 出力: build/libs/my-books.jar
```

### Docker イメージのビルド（本番用）

```bash
docker build --target production -t my-books-api:latest .
```

## 🔑 認証・認可

### Keycloak統合

- **認証方式**: OAuth 2.0 / OpenID Connect
- **トークン**: JWT (Access Token)
- **ユーザーID**: Keycloak UUID (String型)
- **パスワード管理**: Keycloak側で管理
- **Role管理**: Keycloak側で管理（Realm Role、Client Role）

### エンドポイントアクセス制御

| エンドポイント | メソッド | 認証 | 備考 |
|-------------|---------|-----|------|
| `/books/**` | GET | 不要 | 書籍情報の閲覧のみパブリック |
| `/genres/**` | GET | 不要 | ジャンル情報の閲覧のみパブリック |
| `/book-content/**` | GET | 必要 | 書籍コンテンツ（有料機能） |
| `/reviews` | POST/PUT/DELETE | 必要 | レビューの投稿・編集・削除 |
| `/favorites` | POST/DELETE | 必要 | お気に入りの追加・削除 |
| `/bookmarks` | POST/PUT/DELETE | 必要 | ブックマークの管理 |
| `/me/**` | GET/PUT | 必要 | ユーザープロフィール管理 |
| `/admin/**` | ALL | ADMIN | 管理者機能 |

## 📚 API ドキュメント

### Swagger UI

開発環境: http://localhost:8080/swagger-ui.html

### 主要エンドポイント

#### 書籍関連
- `GET /books/new-releases` - 最新書籍（10冊）
- `GET /books/search?q=keyword` - タイトル検索
- `GET /books/discover?genreIds=1,2&condition=AND` - ジャンル検索
- `GET /books/{id}` - 書籍詳細
- `GET /books/{id}/toc` - 目次
- `GET /books/{id}/reviews` - レビュー一覧

#### ユーザー機能（`/me/**`）
- `GET /me/profile` - プロフィール情報
- `GET /me/reviews` - 投稿レビュー一覧
- `GET /me/favorites` - お気に入り一覧
- `GET /me/bookmarks` - ブックマーク一覧
- `PUT /me/profile` - プロフィール更新

詳細は [CLAUDE.md](CLAUDE.md) の「API 設計」セクションを参照してください。

## 🛠️ 技術スタック

| カテゴリ | 技術 |
|---------|------|
| **言語** | Java 21 |
| **フレームワーク** | Spring Boot 3.3.5 |
| **データベース** | MySQL 8.0 |
| **ORM** | JPA / Hibernate |
| **認証** | Keycloak (OAuth 2.0 / OIDC) |
| **セキュリティ** | Spring Security 6 + OAuth2 Resource Server |
| **シークレット管理** | Docker Secrets |
| **API ドキュメント** | OpenAPI 3 / Swagger UI |
| **マッピング** | MapStruct 1.5.5 |
| **ビルドツール** | Gradle |
| **コンテナ** | Docker & Docker Compose |
| **テスト** | JUnit 5, Mockito, Testcontainers |

## 🔒 セキュリティ設計

### Docker Secrets

データベースパスワードは Docker Secrets で管理しています。

```
secrets/
├── my_books_db_password       # アプリ用DBパスワード
└── my_books_db_root_password  # MySQLルートパスワード
```

- `secrets/` ディレクトリは `.gitignore` に登録されており、Gitにはコミットされません
- コンテナ起動時に `/run/secrets/` 以下にマウントされ、アプリケーションが自動読み込みします

### 3層防御アーキテクチャ

1. **SecurityConfig**: エンドポイントパターンでの粗いチェック（第1層）
2. **サービス層**: `@PreAuthorize` による厳密な権限チェック（最後の砦）
3. **コントローラー層**: 権限チェックなし（サービス層に委譲）

## 📖 ドキュメント

- **[CLAUDE.md](CLAUDE.md)**: 開発ガイド（包括的なドキュメント）
- **[docs/system-architecture-overview-vps2.md](docs/system-architecture-overview-vps2.md)**: システムアーキテクチャ詳細
- **[docs/ROLE-DESIGN.md](docs/ROLE-DESIGN.md)**: 権限・ロール設計
- **[docs/DATABASE.md](docs/DATABASE.md)**: データベース設計
- **[README.env.md](README.env.md)**: 環境変数設定ガイド

## 🔧 開発

### 推奨IDE設定

- **IDE**: IntelliJ IDEA、Eclipse、VS Code
- **Plugins**: Lombok、Spring Boot、Docker
- **Java SDK**: Temurin 21

### コーディング規約

詳細は [CLAUDE.md](CLAUDE.md) の「開発規約」セクションを参照してください。

## 📊 パフォーマンス最適化

- ✅ **2クエリ戦略**: N+1問題の完全解決
- ✅ **HikariCP**: 接続プール最適化
- ✅ **MapStruct最適化**: コンパイル時最適化（15-20%高速化）
- ✅ **章タイトルバッチ取得**: 効率的な動的情報付与

## 🤝 貢献

プロジェクトへの貢献を歓迎します。

## 📝 ライセンス

[ライセンス情報を記載]

## 👥 作成者

[作成者情報を記載]

---

**詳細なドキュメントは [CLAUDE.md](CLAUDE.md) を参照してください。**
