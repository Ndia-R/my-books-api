package com.example.my_books_api.service;

import org.springframework.lang.NonNull;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.bookmark.BookmarkRequest;
import com.example.my_books_api.dto.bookmark.BookmarkResponse;

public interface BookmarkService {
    /**
     * ユーザーが追加したブックマークを取得
     *
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return ブックマークリスト
     */
    PageResponse<BookmarkResponse> getUserBookmarks(
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * ブックマークを作成
     *
     * @param request ブックマーク作成リクエスト
     * @return 作成されたブックマーク情報
     */
    BookmarkResponse createBookmark(BookmarkRequest request);

    /**
     * ブックマークを更新
     *
     * @param id 更新するブックマークのID
     * @param request ブックマーク更新リクエスト
     * @return 更新されたブックマーク情報
     */
    BookmarkResponse updateBookmark(@NonNull Long id, BookmarkRequest request);

    /**
     * ブックマークを削除
     *
     * @param id 削除するブックマークのID
     */
    void deleteBookmark(@NonNull Long id);
}
