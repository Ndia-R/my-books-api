package com.example.my_books_backend.service;

import java.util.List;
import com.example.my_books_backend.dto.genre.GenreRequest;
import com.example.my_books_backend.dto.genre.GenreResponse;

public interface GenreService {
    /**
     * すべてのジャンルを取得
     * 
     * @return ジャンルリスト
     */
    List<GenreResponse> getAllGenres();

    /**
     * 指定されたジャンルを取得
     * 
     * @param id ジャンルID
     * @return ジャンル
     */
    GenreResponse getGenreById(Long id);

    /**
     * 指定されたジャンルを取得（複数指定）
     * 
     * @param ids ジャンルID
     * @return ジャンル
     */
    List<GenreResponse> getGenresByIds(List<Long> ids);

    /**
     * ジャンルを作成
     * 
     * @param request ジャンル作成リクエスト
     * @return 作成されたジャンル情報
     */
    GenreResponse createGenre(GenreRequest request);

    /**
     * ジャンルを更新
     * 
     * @param id 更新するジャンルのID
     * @param request ジャンル更新リクエスト
     * @return 更新されたジャンル情報
     */
    GenreResponse updateGenre(Long id, GenreRequest request);

    /**
     * ジャンルを削除
     * 
     * @param id 削除するジャンルのID
     */
    void deleteGenre(Long id);
}
