package com.example.my_books_api.service;

import org.springframework.lang.NonNull;
import com.example.my_books_api.dto.user.UserProfileCountsResponse;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.dto.user.UserResponse;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.user.UpdateSubscriptionPlanRequest;
import com.example.my_books_api.dto.user.UpdateUserProfileRequest;

public interface UserService {

    /**
     * ユーザー情報を取得
     *
     * @return ユーザー情報
     */
    UserResponse getUser();

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

    /**
     * 本人のサブスクリプションプランを更新
     *
     * @param request サブスクリプションプラン更新リクエスト
     * @return 更新後のユーザープロフィール情報
     */
    UserProfileResponse updateSubscriptionPlan(UpdateSubscriptionPlanRequest request);

    /**
     * 管理者が指定ユーザーのサブスクリプションプランを更新
     *
     * @param id 対象ユーザーID
     * @param request サブスクリプションプラン更新リクエスト
     * @return 更新後のユーザープロフィール情報
     */
    UserProfileResponse updateUserSubscriptionPlan(@NonNull String id, UpdateSubscriptionPlanRequest request);
}
