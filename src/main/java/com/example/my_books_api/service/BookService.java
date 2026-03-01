package com.example.my_books_api.service;

import org.springframework.lang.NonNull;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book.BookDetailsResponse;
import com.example.my_books_api.dto.book.BookResponse;
import com.example.my_books_api.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_api.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingPublicResponse;

public interface BookService {
    /**
     * 書籍一覧取得
     * 
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @return 最新の書籍リスト
     */
    PageResponse<BookResponse> getBooks(
        Long page,
        Long size,
        String sortString
    );

    /**
     * タイトルで書籍を検索したリストを取得
     * 
     * @param keyword 検索キーワード
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @return 検索結果
     */
    PageResponse<BookResponse> getBooksByTitleKeyword(
        String keyword,
        Long page,
        Long size,
        String sortString
    );

    /**
     * ジャンルIDで書籍を検索したリストを取得
     * 
     * @param genreIdsQuery カンマ区切りのジャンルIDリスト（例："1,2,3"）
     * @param conditionQuery 検索条件（"SINGLE"、"AND"、"OR"のいずれか）
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @return 検索結果
     */
    PageResponse<BookResponse> getBooksByGenre(
        String genreIdsQuery,
        String conditionQuery,
        Long page,
        Long size,
        String sortString
    );

    /**
     * 指定された書籍の詳細情報を取得
     * 
     * @param id 書籍ID
     * @return 書籍の詳細情報
     */
    BookDetailsResponse getBookDetails(@NonNull String id);

    /**
     * 新しい書籍を作成
     *
     * @param request 書籍作成リクエスト
     * @return 作成された書籍の詳細情報
     */
    BookDetailsResponse createBook(com.example.my_books_api.dto.book.BookRequest request);

    /**
     * 既存の書籍を更新
     *
     * @param id 更新対象の書籍ID
     * @param request 書籍更新リクエスト
     * @return 更新された書籍の詳細情報
     */
    BookDetailsResponse updateBook(String id, com.example.my_books_api.dto.book.BookRequest request);

    /**
     * 書籍を削除
     *
     * @param id 削除対象の書籍ID
     */
    void deleteBook(String id);

    // --- 他のメソッド (目次・ページ取得等) ---

    /**
     * 指定された書籍の目次情報（章のリスト）を取得
     * 
     * @param id 書籍ID
     * @return 書籍の目次情報
     */
    BookTableOfContentsResponse getBookTableOfContents(@NonNull String id);

    /**
     * 指定された書籍の特定の章・ページのコンテンツを取得（試し読み）
     * 
     * @param bookId 書籍ID
     * @param chapterNumber 章番号
     * @param pageNumber ページ番号
     * @return 書籍のコンテンツ情報
     */
    BookChapterPageContentResponse getBookChapterPagePreview(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );

    /**
     * 指定された書籍の特定の章・ページのコンテンツを取得
     * 
     * @param bookId 書籍ID
     * @param chapterNumber 章番号
     * @param pageNumber ページ番号
     * @return 書籍のコンテンツ情報
     */
    BookChapterPageContentResponse getBookChapterPageContent(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );

    /**
     * 試し読み設定を取得（書籍ID指定）
     * 設定がない場合はデフォルト値（第1章全体）を返す
     *
     * @param bookId 書籍ID
     * @return 試し読み設定（パブリック用）
     */
    BookPreviewSettingPublicResponse getPreviewSettingByBookId(@NonNull String bookId);
}
