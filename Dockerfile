# ====================================
# 開発環境ステージ
# ====================================
FROM eclipse-temurin:21-jdk-jammy AS development

RUN apt update && \
    apt install -y git curl sudo bash python3 ca-certificates gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt install -y nodejs && \
    # Install Docker CLI for Testcontainers support
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu jammy stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt update && \
    apt install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

# 既存ユーザーがいないので、新規作成
RUN useradd -m vscode

# vscodeユーザーがパスワードなしでsudoを使えるように設定
# /etc/sudoers.d/vscodeファイルを作成し、NOPASSWD: ALL を設定
RUN echo "vscode ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/vscode && \
    chmod 0440 /etc/sudoers.d/vscode

# Add vscode user to docker group for Docker socket access
RUN groupadd -f docker && usermod -aG docker vscode

# CA証明書をコピー
COPY ./certs/rootCA.pem /tmp/rootCA.pem

# CA証明書をJavaトラストストアに追加
RUN keytool -import -trustcacerts -noprompt \
    -alias mkcert-ca \
    -file /tmp/rootCA.pem \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit && \
    rm /tmp/rootCA.pem

# vscodeユーザーに切り替え
USER vscode
WORKDIR /workspace

# Gradleキャッシュ用ディレクトリを作成（volume用）
RUN mkdir -p /home/vscode/.gradle

# Python uv（Serena MCP用）をインストール
RUN curl -LsSf https://astral.sh/uv/install.sh | sh

# uvをPATHに追加
ENV PATH="/home/vscode/.local/bin:$PATH"

# Gemini CLI, Claude Codeをグローバルインストール
USER root
RUN npm install -g @google/gemini-cli
RUN npm install -g @anthropic-ai/claude-code
# 元のユーザーに戻す
USER vscode

# ====================================
# 本番環境: ビルドステージ
# ====================================
FROM eclipse-temurin:21-jdk-jammy AS production-builder

WORKDIR /build

# Gradleラッパーとビルドファイルをコピー
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 依存関係を事前ダウンロード（キャッシュ効率化）
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon || true

# ソースコードをコピーしてビルド
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon

# ====================================
# 本番環境: 実行ステージ
# ====================================
FROM eclipse-temurin:21-jre-alpine AS production

RUN apk add --update curl

# CA証明書をコピー
COPY ./certs/rootCA.pem /tmp/rootCA.pem

# CA証明書をJavaトラストストアに追加
RUN keytool -import -trustcacerts -noprompt \
    -alias mkcert-ca \
    -file /tmp/rootCA.pem \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit && \
    rm /tmp/rootCA.pem

# セキュリティ: 非rootユーザーで実行
RUN addgroup -S appuser && adduser -S -G appuser appuser

WORKDIR /app

# ビルドステージからJARファイルのみコピー
COPY --from=production-builder /build/build/libs/*.jar app.jar

# 所有権を変更
RUN chown appuser:appuser /app/app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
