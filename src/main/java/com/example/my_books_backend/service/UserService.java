package com.example.my_books_backend.service;

import java.util.List;
import com.example.my_books_backend.dto.user.UpdateUserEmailRequest;
import com.example.my_books_backend.dto.user.UpdateUserPasswordRequest;
import com.example.my_books_backend.dto.user.CreateUserRequest;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.entity.User;

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
    UserResponse getUserById(Long id);

    /**
     * ユーザーを作成
     * 
     * @param request ユーザー作成リクエスト
     * @return 作成されたユーザー情報
     */
    User createUser(CreateUserRequest request);

    /**
     * ユーザーを削除
     * 
     * @param id 削除するユーザーのID
     */
    void deleteUser(Long id);

    /**
     * ユーザーのプロフィール情報を取得
     * 
     * @param user ユーザーエンティティ
     * @return ユーザープロフィール情報
     */
    UserProfileResponse getUserProfile(User user);

    /**
     * ユーザーのプロフィール情報のレビュー、お気に入り、ブックマークの数を取得
     * 
     * @param user ユーザーエンティティ
     * @return レビュー、お気に入り、ブックマークの数
     */
    UserProfileCountsResponse getUserProfileCounts(User user);

    /**
     * ユーザーのプロフィール情報を更新
     * 
     * @param request ユーザープロフィール更新リクエスト
     * @param user ユーザーエンティティ
     */
    void updateUserProfile(UpdateUserProfileRequest request, User user);

    /**
     * ユーザーのメールアドレスを更新
     * 
     * @param request ユーザーメールアドレス更新リクエスト
     * @param user ユーザーエンティティ
     */
    void updateUserEmail(UpdateUserEmailRequest request, User user);

    /**
     * ユーザーのパスワードを更新
     * 
     * @param request ユーザーパスワード更新リクエスト
     * @param user ユーザーエンティティ
     */
    void updateUserPassword(UpdateUserPasswordRequest request, User user);
}
