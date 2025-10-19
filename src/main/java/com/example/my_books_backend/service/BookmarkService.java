package com.example.my_books_backend.service;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.User;

public interface BookmarkService {
    /**
     * ユーザーが追加したブックマークを取得
     * 
     * @param user ユーザーエンティティ
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return ブックマークリスト
     */
    PageResponse<BookmarkResponse> getUserBookmarks(
        User user,
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * ブックマークを作成
     * 
     * @param request ブックマーク作成リクエスト
     * @param user ユーザーエンティティ
     * @return 作成されたブックマーク情報
     */
    BookmarkResponse createBookmark(BookmarkRequest request, User user);

    /**
     * ブックマークを更新
     * 
     * @param id 更新するブックマークのID
     * @param request ブックマーク更新リクエスト
     * @param user ユーザーエンティティ
     * @return 更新されたブックマーク情報
     */
    BookmarkResponse updateBookmark(Long id, BookmarkRequest request, User user);

    /**
     * ブックマークを削除
     * 
     * @param id 削除するブックマークのID
     * @param user ユーザーエンティティ
     */
    void deleteBookmark(Long id, User user);
}
