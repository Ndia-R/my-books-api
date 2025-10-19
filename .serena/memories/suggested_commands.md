# 開発用コマンド一覧

## 基本的な開発コマンド

### ビルド・テスト
```bash
# プロジェクトのビルド
./gradlew build

# テスト実行
./gradlew test

# クリーンビルド
./gradlew clean build

# 依存関係確認
./gradlew dependencies
```

### アプリケーション実行
```bash
# Spring Boot アプリケーション起動
./gradlew bootRun

# JAR ファイル生成（my-books.jar として生成）
./gradlew bootJar
```

### Docker 開発環境
```bash
# 開発環境の起動
docker-compose up -d

# アプリケーションのみ再起動
docker-compose restart app

# ログ確認
docker-compose logs -f app

# 開発環境の停止
docker-compose down
```

### 権限設定（必要に応じて）
```bash
# Gradle Wrapper の権限エラー対処
chmod +x gradlew
```

## 環境設定
- 環境変数は `.env.example` を参考に設定
- Docker環境では `docker-compose.yml` で自動設定

## ポート設定
- アプリケーション: 8080
- MySQL: 3306
- Swagger UI: http://localhost:8080/swagger-ui.html