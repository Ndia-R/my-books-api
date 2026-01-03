package com.example.my_books_backend.service;

import org.springframework.lang.NonNull;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;

public interface UserService {
    /**
     * ユーザー一覧取得
     * 
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @return ユーザーリスト
     */
    PageResponse<UserProfileResponse> getUsers(
        Long page,
        Long size,
        String sortString
    );

    /**
     * 指定されたユーザーを取得
     *
     * @param id ユーザーID
     * @return ユーザー
     */
    UserProfileResponse getUserById(@NonNull String id);

    /**
     * ユーザーを削除
     *
     * @param id 削除するユーザーのID
     */
    void deleteUser(@NonNull String id);

    /**
     * ユーザーのプロフィール情報を取得（存在しない場合は自動作成）
     *
     * @return ユーザープロフィール情報
     */
    UserProfileResponse getUserProfile();

    /**
     * ユーザーのプロフィール情報のレビュー、お気に入り、ブックマークの数を取得
     *
     * @return レビュー、お気に入り、ブックマークの数
     */
    UserProfileCountsResponse getUserProfileCounts();

    /**
     * ユーザーのプロフィール情報を更新
     *
     * @param request ユーザープロフィール更新リクエスト
     * @return 更新後のユーザープロフィール情報
     */
    UserProfileResponse updateUserProfile(UpdateUserProfileRequest request);
}
