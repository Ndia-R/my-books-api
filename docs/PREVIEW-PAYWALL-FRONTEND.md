# 試し読みペイウォール表示 — フロントエンド実装ガイド

## 概要

試し読みの最後のページに到達した際にペイウォール画面を表示する機能の実装ガイド。
バックエンドへの追加エンドポイントは不要で、既存APIのレスポンスのみで判定が可能。

---

## 使用するAPI

### 1. 試し読み設定の取得（パブリック）

```
GET /preview-settings/books/{bookId}
```

**レスポンス例:**

```json
{
  "id": 1,
  "maxChapter": 3,
  "maxPage": 5,
  "createdAt": "2025-02-01T10:00:00",
  "updatedAt": "2025-02-07T15:30:00",
  "book": { ... }
}
```

| フィールド | 型 | 説明 |
|-----------|------|------|
| `maxChapter` | `number` | 試し読み可能な最大章番号。`-1` = 全章無制限 |
| `maxPage` | `number` | `maxChapter` における最大ページ番号。`-1` = その章の全ページ無制限 |

> **注意**: 書籍にプレビュー設定が未登録の場合もデフォルト値（`maxChapter: 1`, `maxPage: -1`）が返される。

### 2. 試し読みページコンテンツの取得（パブリック）

```
GET /book-content/preview/books/{bookId}/chapters/{chapter}/pages/{page}
```

**レスポンス例:**

```json
{
  "bookId": "afcIMuetDuzj",
  "chapterNumber": 3,
  "chapterTitle": "第3章 応用編",
  "pageNumber": 5,
  "totalPagesInChapter": 12,
  "content": "..."
}
```

| フィールド | 型 | 説明 |
|-----------|------|------|
| `chapterNumber` | `number` | 現在の章番号 |
| `pageNumber` | `number` | 現在のページ番号 |
| `totalPagesInChapter` | `number` | 現在の章の総ページ数 |

**試し読み範囲外のページにアクセスした場合:** `403 Forbidden`

```json
{
  "error": "FORBIDDEN",
  "message": "閲覧する権限がありません",
  "status": 403,
  "path": "/book-content/preview/books/afcIMuetDuzj/chapters/4/pages/1",
  "timestamp": "2025-02-07 15:30:00"
}
```

### 3. 目次の取得（パブリック・補助的）

```
GET /books/{bookId}/toc
```

**レスポンス例:**

```json
{
  "bookId": "afcIMuetDuzj",
  "title": "サンプル書籍",
  "chapters": [
    { "chapterNumber": 1, "chapterTitle": "第1章 入門", "totalPages": 8 },
    { "chapterNumber": 2, "chapterTitle": "第2章 基礎", "totalPages": 10 },
    { "chapterNumber": 3, "chapterTitle": "第3章 応用編", "totalPages": 12 }
  ]
}
```

> 目次情報は `maxChapter = -1`（全章無制限）のケースで最終章の特定に使用。

---

## ペイウォール判定ロジック

### 判定に必要なデータ

| データ | 取得元 | タイミング |
|--------|--------|-----------|
| `maxChapter`, `maxPage` | `GET /preview-settings/books/{bookId}` | 書籍閲覧開始時に1回取得してキャッシュ |
| `chapterNumber`, `pageNumber`, `totalPagesInChapter` | ページコンテンツのレスポンス | ページ遷移ごと |
| 最終章番号（`maxChapter = -1` の場合のみ） | `GET /books/{bookId}/toc` | 書籍閲覧開始時に1回取得 |

### 判定フロー

```
maxChapter が -1（全章無制限）か？
├─ YES → ペイウォール不要（全コンテンツが試し読み対象）
│        ※ 最終章の最終ページで「試し読み終了」表示は任意
└─ NO  → 現在の章番号と maxChapter を比較
          ├─ 現在の章 < maxChapter → ペイウォールではない
          ├─ 現在の章 > maxChapter → ここには到達しない（バックエンドが403を返す）
          └─ 現在の章 = maxChapter → maxPage を確認
                ├─ maxPage が -1 → 現在のページ = totalPagesInChapter ならペイウォール
                └─ maxPage が 正の数 → 現在のページ = maxPage ならペイウォール
```

### 実装例（TypeScript）

```typescript
type PreviewSetting = {
  maxChapter: number;
  maxPage: number;
};

type PageContent = {
  chapterNumber: number;
  pageNumber: number;
  totalPagesInChapter: number;
};

/**
 * 現在のページが試し読みの最後のページかを判定する。
 * true の場合、ペイウォール表示が必要。
 */
function isLastPreviewPage(
  setting: PreviewSetting,
  page: PageContent
): boolean {
  const { maxChapter, maxPage } = setting;
  const { chapterNumber, pageNumber, totalPagesInChapter } = page;

  // 全章無制限 → ペイウォール不要
  if (maxChapter === -1) return false;

  // まだ最大章に達していない → ペイウォールではない
  if (chapterNumber < maxChapter) return false;

  // 最大章にいる場合
  if (chapterNumber === maxChapter) {
    if (maxPage === -1) {
      // その章の全ページが試し読み対象 → 章の最終ページがペイウォール
      return pageNumber === totalPagesInChapter;
    }
    // ページ制限あり → そのページがペイウォール
    return pageNumber === maxPage;
  }

  // maxChapter を超えている（通常はバックエンドの403で到達しない）
  return true;
}
```

### 使用例

```typescript
// 書籍閲覧開始時に1回取得
const setting = await fetch(`/preview-settings/books/${bookId}`).then(r => r.json());

// ページ遷移ごとに判定
const pageContent = await fetch(
  `/book-content/preview/books/${bookId}/chapters/${chapter}/pages/${page}`
).then(r => r.json());

if (isLastPreviewPage(setting, pageContent)) {
  showPaywall();
}
```

---

## 「次のページ」ボタンの制御

ペイウォール判定に加えて、「次のページ」ボタンの遷移先判定も必要。

```typescript
/**
 * 次のページ情報を返す。ペイウォールが必要な場合は null を返す。
 */
function getNextPage(
  setting: PreviewSetting,
  page: PageContent
): { chapter: number; page: number } | null {
  const { chapterNumber, pageNumber, totalPagesInChapter } = page;
  const { maxChapter, maxPage } = setting;

  // 章内に次のページがあるか
  if (pageNumber < totalPagesInChapter) {
    const nextPage = pageNumber + 1;
    // 次のページがプレビュー範囲内か確認
    if (chapterNumber === maxChapter && maxPage !== -1 && nextPage > maxPage) {
      return null; // ペイウォール
    }
    return { chapter: chapterNumber, page: nextPage };
  }

  // 章の最後のページ → 次の章へ
  const nextChapter = chapterNumber + 1;
  if (maxChapter !== -1 && nextChapter > maxChapter) {
    return null; // ペイウォール
  }
  return { chapter: nextChapter, page: 1 };
}
```

---

## エラーハンドリング（防御的実装）

フロントエンドの判定をすり抜けた場合でも、バックエンドが403を返すため安全。

```typescript
try {
  const response = await fetch(
    `/book-content/preview/books/${bookId}/chapters/${chapter}/pages/${page}`
  );
  if (response.status === 403) {
    // バックエンド側の防御 → ペイウォール表示
    showPaywall();
    return;
  }
  const content = await response.json();
  renderPage(content);
} catch (error) {
  handleError(error);
}
```

---

## 設定値パターン一覧

| maxChapter | maxPage | 意味 | ペイウォール発生箇所 |
|------------|---------|------|---------------------|
| `1` | `-1` | **デフォルト**: 第1章のみ全ページ試し読み | 第1章の最終ページ |
| `3` | `5` | 第3章の5ページ目まで試し読み | 第3章の5ページ目 |
| `3` | `-1` | 第3章の全ページまで試し読み | 第3章の最終ページ |
| `-1` | `-1` | 全コンテンツ無制限（試し読み制限なし） | なし |
| `5` | `10` | 第5章の10ページ目まで試し読み | 第5章の10ページ目 |
