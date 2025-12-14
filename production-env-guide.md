# 🚀 VPS2本番環境 `.env` ファイル作成ガイド

このガイドは、VPS2の本番環境用`.env`ファイルを作成する際の**重要な注意点とベストプラクティス**をまとめたものです。

## 📋 目次

1. [セキュリティ上の重大な変更点](#1-セキュリティ上の重大な変更点)
2. [VPS2本番環境 `.env` テンプレート](#2-vps2本番環境-env-テンプレート)
3. [パスワード生成の推奨方法](#3-パスワード生成の推奨方法)
4. [本番環境固有の設定値](#4-本番環境固有の設定値)
5. [致命的なミスを防ぐチェックリスト](#5-致命的なミスを防ぐチェックリスト)
6. [機密情報管理のベストプラクティス](#6-機密情報管理のベストプラクティス)
7. [本番環境デプロイ前の最終確認](#7-本番環境デプロイ前の最終確認)

---

## 1. 🚨 セキュリティ上の重大な変更点

### ❌ 開発環境（絶対に本番で使ってはいけない設定）

```bash
# 開発環境 - 危険な設定
DB_USER=root                              # ❌ rootユーザー使用
DB_PASSWORD=password                      # ❌ 弱いパスワード
SERVER_ERROR_INCLUDE_MESSAGE=always       # ❌ エラー詳細を公開
SERVER_ERROR_INCLUDE_STACKTRACE=always    # ❌ スタックトレース公開
SPRING_JPA_SHOW_SQL=true                  # ❌ SQLログ出力（パフォーマンス低下）
LOGGING_LEVEL=DEBUG                       # ❌ 大量ログ出力
```

### ✅ 本番環境（必須の設定）

```bash
# 本番環境 - セキュアな設定
DB_USER=my_books_user                     # ✅ 専用ユーザー
DB_PASSWORD=<強力なランダムパスワード>      # ✅ 32文字以上推奨
SERVER_ERROR_INCLUDE_MESSAGE=never        # ✅ エラー詳細を隠蔽
SERVER_ERROR_INCLUDE_STACKTRACE=never     # ✅ スタックトレース非表示
SPRING_JPA_SHOW_SQL=false                 # ✅ SQLログ無効化
LOGGING_LEVEL=INFO                        # ✅ 必要最小限のログ
```

---

## 2. 📝 VPS2本番環境 `.env` テンプレート

以下のテンプレートを `vps2-deployment/.env` として保存してください。

```bash
# ============================================
# VPS2 本番環境設定
# ============================================
# 最終更新: 2025-XX-XX
# 注意: このファイルは機密情報を含むため、絶対にGitにコミットしないこと

# ============================================
# 共通設定
# ============================================
# Identity Provider (Keycloak on VPS1)
IDP_ISSUER_URI=https://vsv-crystal.skygroup.local/auth/realms/test-realm

# Docker Registry (VPS1)
REGISTRY_URL=vsv-crystal.skygroup.local

# ============================================
# Backend Database Configuration
# ============================================
# ⚠️ 警告: 本番環境では必ず強力なパスワードを設定すること
BACKEND_DB_NAME=my_books_production
BACKEND_DB_USER=my_books_user
BACKEND_DB_PASSWORD=<CHANGE_ME_強力なパスワード32文字以上>

# ⚠️ 注意: DB_URLはdocker-compose.ymlで backend-db:3306 を使用
# ここでは定義不要（compose側で SPRING_DATASOURCE_URL として構築）

# ============================================
# Backend Application Settings
# ============================================
# Image Tag
BACKEND_IMAGE_TAG=latest

# JPA Settings (本番環境 - SQLログ無効化)
BACKEND_JPA_SHOW_SQL=false
BACKEND_JPA_FORMAT_SQL=false

# DataSource Pool Settings (本番環境 - 高負荷対応)
BACKEND_POOL_MAX_SIZE=50          # 同時接続ユーザー数に応じて調整
BACKEND_POOL_MIN_IDLE=20          # 最小アイドル接続数
BACKEND_POOL_CONNECTION_TIMEOUT=30000

# Error Response Settings (セキュリティ - エラー詳細非表示)
BACKEND_ERROR_INCLUDE_MESSAGE=never
BACKEND_ERROR_INCLUDE_STACKTRACE=never

# Logging (本番環境 - INFO レベル)
BACKEND_LOGGING_LEVEL=INFO

# ============================================
# BFF Configuration
# ============================================
BFF_IMAGE_TAG=latest
# ... BFF固有の設定 ...

# ============================================
# Frontend Configuration
# ============================================
FRONTEND_IMAGE_TAG=latest
# ... Frontend固有の設定 ...

# ============================================
# Redis Configuration
# ============================================
REDIS_PASSWORD=<CHANGE_ME_Redisパスワード>

# ============================================
# セキュリティ設定
# ============================================
# HTTPS証明書の更新日（管理用）
SSL_CERT_EXPIRY=2026-01-01
```

---

## 3. 🔑 パスワード生成の推奨方法

本番環境では**強力なランダムパスワード**を使用してください。

### Linux/macOS でのパスワード生成

```bash
# 方法1: Base64エンコード（32文字）
openssl rand -base64 32

# 方法2: 英数字記号混在パスワード生成（32文字）
openssl rand -base64 48 | tr -d "=+/" | cut -c1-32

# 生成例
# XkP9mN2vL8qR5wT3hJ7fG4dS6aZ1cV0b
```

### パスワード要件

- **最小文字数**: 32文字以上
- **文字種**: 英大文字、英小文字、数字を含む
- **避けるべき**: 辞書にある単語、個人情報、推測可能な文字列

---

## 4. ⚙️ 本番環境固有の設定値

| 設定項目 | 開発環境 | 本番環境 | 理由 |
|---------|---------|---------|------|
| **DB_NAME** | `my-books-db` | `my_books_production` | 環境の明示的な分離 |
| **DB_USER** | `root` | `my_books_user` | 最小権限の原則 |
| **DB_PASSWORD** | `password` | `<32文字以上>` | セキュリティ |
| **POOL_MAX_SIZE** | `10` | `50` | 本番負荷対応 |
| **POOL_MIN_IDLE** | `5` | `20` | レスポンス速度 |
| **JPA_SHOW_SQL** | `true` | `false` | ログ容量・パフォーマンス |
| **ERROR_INCLUDE_MESSAGE** | `always` | `never` | 情報漏洩防止 |
| **ERROR_INCLUDE_STACKTRACE** | `always` | `never` | 攻撃者への情報提供防止 |
| **LOGGING_LEVEL** | `DEBUG` | `INFO` または `WARN` | ログ容量削減 |

### 設定値の詳細説明

#### **POOL_MAX_SIZE（最大接続数）**
- **推奨値**: 50-100
- **決定要素**: 予想される同時接続ユーザー数
- **計算式**: `同時接続数 × 1.5`（バッファを含む）

#### **POOL_MIN_IDLE（最小アイドル接続数）**
- **推奨値**: POOL_MAX_SIZE の 30-40%
- **効果**: 接続確立時間の短縮

#### **LOGGING_LEVEL**
- **開発**: `DEBUG` - すべての詳細ログ
- **本番**: `INFO` - 重要な情報のみ
- **高負荷時**: `WARN` - 警告とエラーのみ

---

## 5. 🚨 致命的なミスを防ぐチェックリスト

本番環境の`.env`ファイル作成時に**必ず確認**してください。

### セキュリティチェック

- [ ] `DB_PASSWORD` が32文字以上のランダム文字列
- [ ] `DB_USER` が `root` **ではない**
- [ ] `SERVER_ERROR_INCLUDE_MESSAGE=never`
- [ ] `SERVER_ERROR_INCLUDE_STACKTRACE=never`
- [ ] `SPRING_JPA_SHOW_SQL=false`
- [ ] Redis等の他サービスもパスワード設定済み
- [ ] `<CHANGE_ME_...>` プレースホルダーがすべて置換済み

### パフォーマンスチェック

- [ ] `DATASOURCE_POOL_MAX_SIZE` が適切（50-100推奨）
- [ ] `DATASOURCE_POOL_MIN_IDLE` が適切（20-40推奨）
- [ ] `LOGGING_LEVEL=INFO` または `WARN`
- [ ] `SPRING_JPA_FORMAT_SQL=false`

### 接続設定チェック

- [ ] `IDP_ISSUER_URI` がVPS1の正しいURL（HTTPS）
- [ ] データベースホスト名が `backend-db`（docker-compose内部）
- [ ] ポート番号が適切（通常変更不要）

### ファイル管理チェック

- [ ] `.env` が `.gitignore` に含まれている
- [ ] ファイルの権限が `600`（所有者のみ読み書き可能）
  ```bash
  chmod 600 .env
  ```
- [ ] バックアップが安全な場所に保存されている

---

## 6. 🔐 機密情報管理のベストプラクティス

### オプション1: Docker Secrets（推奨）

機密情報を環境変数ファイルではなく、Docker Secretsで管理する方法です。

#### **docker-compose.yml の設定**

```yaml
services:
  backend:
    image: ${REGISTRY_URL}/my-books-api:${BACKEND_IMAGE_TAG}
    secrets:
      - backend_db_password
      - redis_password
    environment:
      # ファイルパスを指定
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/backend_db_password
      REDIS_PASSWORD_FILE: /run/secrets/redis_password

secrets:
  backend_db_password:
    file: ./secrets/backend_db_password.txt
  redis_password:
    file: ./secrets/redis_password.txt
```

#### **Secretsファイルの作成**

```bash
# Secretsディレクトリ作成
mkdir -p secrets

# パスワードファイル作成
echo "XkP9mN2vL8qR5wT3hJ7fG4dS6aZ1cV0b" > secrets/backend_db_password.txt
echo "aB3dF9gH2jK5mN8pQ1rS4tU7vW0xY6z" > secrets/redis_password.txt

# 権限設定（重要）
chmod 700 secrets/
chmod 600 secrets/*.txt
```

#### **利点**
- ✅ 環境変数に機密情報が含まれない
- ✅ `docker-compose config` で表示されない
- ✅ コンテナ内で `/run/secrets/` にマウントされる（tmpfs、再起動で消える）

---

### オプション2: 環境変数ファイルの暗号化

`.env`ファイル自体を暗号化して管理する方法です。

#### **暗号化**

```bash
# GPGで暗号化（パスフレーズを要求される）
gpg -c .env

# → .env.gpg が生成される
```

#### **デプロイ時の復号化**

```bash
# 復号化
gpg -d .env.gpg > .env

# 権限設定
chmod 600 .env

# デプロイ後は.envを削除（オプション）
# docker-compose up -d
# rm .env
```

#### **利点**
- ✅ `.env.gpg` をGitで管理可能（暗号化済み）
- ✅ パスフレーズで保護
- ✅ チーム間での共有が容易

---

### オプション3: 環境変数のみで管理（非推奨）

```bash
# シェル環境変数として設定
export BACKEND_DB_PASSWORD="XkP9mN2vL8qR5wT3hJ7fG4dS6aZ1cV0b"

# docker-composeで参照
docker-compose up -d
```

#### **欠点**
- ❌ シェル履歴に残る可能性
- ❌ プロセスリストで表示される可能性
- ❌ 管理が煩雑

---

## 7. 🎯 本番環境デプロイ前の最終確認

VPS2で実行する確認コマンドです。

```bash
# VPS2にSSH接続
ssh user@vsv-emerald.skygroup.local

# デプロイディレクトリに移動
cd /path/to/vps2-deployment

# ============================================
# 1. .envファイルの存在確認
# ============================================
ls -la .env
# 出力例: -rw------- 1 user user 1234 Jan 23 10:00 .env

# ============================================
# 2. 権限確認（600であること）
# ============================================
stat -c "%a %n" .env
# 期待値: 600 .env

# ============================================
# 3. パスワードが変更されているか確認
# ============================================
grep -i "CHANGE_ME" .env || echo "✅ OK: パスワード設定済み"
# 期待値: ✅ OK: パスワード設定済み

# ============================================
# 4. rootユーザーが使われていないか確認
# ============================================
grep "DB_USER=root" .env && echo "⚠️ WARNING: rootユーザーが使われています" || echo "✅ OK"
# 期待値: ✅ OK

# ============================================
# 5. エラー詳細が非表示になっているか確認
# ============================================
grep "ERROR_INCLUDE.*=never" .env || echo "⚠️ WARNING: エラー詳細が公開されます"
# 期待値: （何も表示されない = 設定が正しい）

# ============================================
# 6. SQLログが無効化されているか確認
# ============================================
grep "JPA_SHOW_SQL=false" .env || echo "⚠️ WARNING: SQLログが有効です"
# 期待値: （何も表示されない = 設定が正しい）

# ============================================
# 7. ログレベルが適切か確認
# ============================================
grep "LOGGING_LEVEL=INFO\|LOGGING_LEVEL=WARN" .env || echo "⚠️ WARNING: ログレベルが不適切です"
# 期待値: （何も表示されない = 設定が正しい）

# ============================================
# 8. .envファイルがGitに追跡されていないか確認
# ============================================
git check-ignore .env && echo "✅ OK: .envは.gitignoreに含まれています" || echo "⚠️ WARNING: .envがGitで追跡される可能性があります"
# 期待値: ✅ OK: .envは.gitignoreに含まれています
```

### すべて確認が完了したら

```bash
# Docker Composeでデプロイ
docker-compose pull
docker-compose up -d

# ログ確認（エラーがないか）
docker-compose logs -f backend

# ヘルスチェック確認
docker-compose ps
# 期待値: すべてのサービスが "healthy" または "running"
```

---

## 📋 まとめ

### 絶対に変更すべき設定（本番環境）

1. **BACKEND_DB_PASSWORD** - 強力なランダムパスワード（32文字以上）
2. **BACKEND_DB_USER** - 専用ユーザー（`root`を使わない）
3. **BACKEND_ERROR_INCLUDE_MESSAGE** - `never`
4. **BACKEND_ERROR_INCLUDE_STACKTRACE** - `never`
5. **BACKEND_JPA_SHOW_SQL** - `false`
6. **BACKEND_LOGGING_LEVEL** - `INFO`

### 推奨される追加変更

- **BACKEND_POOL_MAX_SIZE** - `50`（負荷に応じて調整）
- **BACKEND_POOL_MIN_IDLE** - `20`
- **BACKEND_DB_NAME** - `my_books_production`（環境の明示的分離）

### そのまま使える設定

- **IDP_ISSUER_URI** - VPS1のKeycloak URL（変更不要）
- **BACKEND_POOL_CONNECTION_TIMEOUT** - `30000`
- **REGISTRY_URL** - `vsv-crystal.skygroup.local`

---

## 🔗 関連ドキュメント

- [backend-dev.md](./backend-dev.md) - バックエンド開発環境ガイド
- [README.env.md](./README.env.md) - 環境変数設定ガイド（開発環境）
- [system-architecture-overview-vps2.md](./system-architecture-overview-vps2.md) - システム構成概要

---

**最終更新日**: 2025-01-23
