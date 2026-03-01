package com.example.my_books_api.service;

import org.springframework.lang.NonNull;
import com.example.my_books_api.dto.favorite.FavoriteRequest;
import com.example.my_books_api.dto.favorite.FavoriteResponse;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.favorite.FavoriteStatsResponse;

public interface FavoriteService {
    /**
     * 書籍に対するお気に入り数を取得
     * 
     * @param bookId 書籍ID
     * @return お気に入り数
     */
    FavoriteStatsResponse getBookFavoriteStats(String bookId);

    /**
     * ユーザーが追加したお気に入りを取得
     *
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param bookId 書籍ID（nullの場合はすべてが対象）
     * @return お気に入りリスト
     */
    PageResponse<FavoriteResponse> getUserFavorites(
        Long page,
        Long size,
        String sortString,
        String bookId
    );

    /**
     * お気に入りを作成
     *
     * @param request お気に入り作成リクエスト
     * @return 作成されたお気に入り情報
     */
    FavoriteResponse createFavorite(FavoriteRequest request);

    /**
     * お気に入りを削除（ID指定）
     *
     * @param id 削除するお気に入りのID
     */
    void deleteFavorite(@NonNull Long id);

    /**
     * お気に入りを削除（書籍ID指定）
     *
     * @param bookId 削除する書籍ID
     */
    void deleteFavoriteByBookId(String bookId);
}
