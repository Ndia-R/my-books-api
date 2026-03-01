package com.example.my_books_api.service;

import org.springframework.lang.NonNull;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingRequest;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingResponse;

public interface BookPreviewSettingService {

    /**
     * 試し読み設定一覧を取得
     *
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの件数
     * @param sortString ソート条件（例: "createdAt.desc", "updatedAt.asc"）
     * @return 試し読み設定一覧
     */
    PageResponse<BookPreviewSettingResponse> getPreviewSettings(
        Long page,
        Long size,
        String sortString
    );

    /**
     * ID指定で試し読み設定を取得（ID指定）
     *
     * @param id 試し読み設定ID
     * @return 試し読み設定
     */
    BookPreviewSettingResponse getPreviewSetting(@NonNull Long id);

    /**
     * 試し読み設定を作成
     *
     * @param request 試し読み設定リクエスト
     * @return 作成された試し読み情報
     */
    BookPreviewSettingResponse createPreviewSetting(BookPreviewSettingRequest request);

    /**
     * 試し読み設定を更新
     *
     * @param id 更新する試し読み設定のID
     * @param request 試し読み設定リクエスト
     * @return 更新された試し読み設定
     */
    BookPreviewSettingResponse updatePreviewSetting(@NonNull Long id, BookPreviewSettingRequest request);

    /**
     * 試し読み設定を削除（ID指定）
     *
     * @param bookId 削除する試し読み設定ID
     */
    void deletePreviewSetting(@NonNull Long id);

    /**
     * 試し読み設定を削除（書籍ID指定）
     *
     * @param bookId 削除する書籍ID
     */
    void deletePreviewSettingByBookId(String bookId);
}
