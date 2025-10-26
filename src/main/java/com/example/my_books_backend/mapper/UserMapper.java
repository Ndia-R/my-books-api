package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // UserResponse: 他のユーザー情報を返す用（email/name無し）
    // id, displayName, avatarPathのみ自動マッピング
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    // UserProfileResponse: 自分自身の情報を返す用（email/name有り）
    // email, nameはUserエンティティにないため、
    // 呼び出し側でJWTクレームから取得して手動で設定する必要があります
    @Mapping(target = "email", ignore = true) // JWTクレームから設定
    @Mapping(target = "name", ignore = true) // JWTクレームから設定
    UserProfileResponse toUserProfileResponse(User user);

    List<UserProfileResponse> toUserProfileResponseList(List<User> users);
}
