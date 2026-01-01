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

詳細は [system-architecture-overview-vps2.md](system-architecture-overview-vps2.md) を参照してください。

### レイヤーアーキテクチャ

```
Controller → Service → Repository → Entity
          ↓
        DTO ← Mapper (MapStruct)
```

## 🚀 クイックスタート

### 前提条件

- Java 21
- Docker & Docker Compose
- Gradle 8.x
- Keycloak (VPS1で稼働中)

### 開発環境のセットアップ

1. **リポジトリのクローン**

```bash
git clone <repository-url>
cd my-books-backend
```

2. **環境変数の設定**

```bash
cp .env.example .env
# .envファイルを編集して環境に合わせて設定
```

3. **Docker環境の起動**

```bash
docker-compose up -d
```

4. **アプリケーションのビルド**

```bash
./gradlew build
```

5. **アプリケーションの起動**

```bash
./gradlew bootRun
```

または、Dockerコンテナ内で：

```bash
docker-compose exec my-books-api ./gradlew bootRun
```

6. **動作確認**

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator Health: http://localhost:8080/actuator/health

## 🧪 テスト

### 全テスト実行

```bash
./gradlew test
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
./gradlew bootJar
# 出力: build/libs/my-books.jar
```

### 本番環境へのデプロイ

1. **環境変数の設定**

```bash
cp .env.production.example .env.production
# .env.productionファイルを編集
```

2. **本番環境の起動**

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d
```

詳細は [CLAUDE.md](CLAUDE.md) の「Docker 開発環境」セクションを参照してください。

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
| **API ドキュメント** | OpenAPI 3 / Swagger UI |
| **マッピング** | MapStruct 1.5.5 |
| **ビルドツール** | Gradle |
| **コンテナ** | Docker & Docker Compose |
| **テスト** | JUnit 5, Mockito, Testcontainers |

## 📖 ドキュメント

- **[CLAUDE.md](CLAUDE.md)**: 開発ガイド（包括的な8000行超のドキュメント）
- **[system-architecture-overview-vps2.md](system-architecture-overview-vps2.md)**: システムアーキテクチャ詳細
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
