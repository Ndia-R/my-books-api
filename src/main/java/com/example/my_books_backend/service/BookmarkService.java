package com.example.my_books_backend.service;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;

public interface BookmarkService {
    /**
     * ユーザーが追加したブックマークを取得
     *
     * @param userId ユーザーID
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return ブックマークリスト
     */
    PageResponse<BookmarkResponse> getUserBookmarks(
        String userId,
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * ブックマークを作成
     *
     * @param request ブックマーク作成リクエスト
     * @param userId ユーザーID
     * @return 作成されたブックマーク情報
     */
    BookmarkResponse createBookmarkByUserId(BookmarkRequest request, String userId);

    /**
     * ブックマークを更新
     *
     * @param id 更新するブックマークのID
     * @param request ブックマーク更新リクエスト
     * @param userId ユーザーID
     * @return 更新されたブックマーク情報
     */
    BookmarkResponse updateBookmarkByUserId(Long id, BookmarkRequest request, String userId);

    /**
     * ブックマークを削除
     *
     * @param id 削除するブックマークのID
     * @param userId ユーザーID
     */
    void deleteBookmarkByUserId(Long id, String userId);
}
