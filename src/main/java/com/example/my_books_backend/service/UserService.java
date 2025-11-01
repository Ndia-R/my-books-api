package com.example.my_books_backend.service;

import java.util.List;
import org.springframework.lang.NonNull;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;

public interface UserService {
    /**
     * すべてのユーザーを取得 （主に管理者向けの機能）
     * 
     * @return ユーザーリスト
     */
    List<UserResponse> getAllUsers();

    /**
     * 指定されたユーザーを取得
     *
     * @param id ユーザーID
     * @return ユーザー
     */
    UserResponse getUserById(@NonNull String id);

    /**
     * ユーザーを削除
     *
     * @param id 削除するユーザーのID
     */
    void deleteUser(@NonNull String id);

    /**
     * ユーザーのプロフィール情報を取得（存在しない場合は自動作成）
     *
     * @param userId ユーザーID
     * @return ユーザープロフィール情報
     */
    UserProfileResponse getUserProfile(@NonNull String userId);

    /**
     * ユーザーのプロフィール情報のレビュー、お気に入り、ブックマークの数を取得
     *
     * @param userId ユーザーID
     * @return レビュー、お気に入り、ブックマークの数
     */
    UserProfileCountsResponse getUserProfileCounts(@NonNull String userId);

    /**
     * ユーザーのプロフィール情報を更新
     *
     * @param request ユーザープロフィール更新リクエスト
     * @param userId ユーザーID
     * @return 更新後のユーザープロフィール情報
     */
    UserProfileResponse updateUserProfile(UpdateUserProfileRequest request, @NonNull String userId);
}
