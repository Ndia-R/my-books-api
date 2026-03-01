package com.example.my_books_api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.dto.user.UserResponse;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.util.PageableUtils;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // UserエンティティにないIdP管理のパラメーターは
    // 呼び出し側でJWTクレームから取得して手動で設定する必要がある
    @Mapping(target = "username", ignore = true) // JWTクレームから設定
    @Mapping(target = "email", ignore = true) // JWTクレームから設定
    @Mapping(target = "familyName", ignore = true) // JWTクレームから設定
    @Mapping(target = "givenName", ignore = true) // JWTクレームから設定
    @Mapping(target = "roles", ignore = true) // JWTクレームから設定
    @Mapping(target = "groups", ignore = true) // JWTクレームから設定
    UserProfileResponse toUserProfileResponse(User user);

    UserResponse toUserResponse(User user);

    List<UserProfileResponse> toUserProfileResponseList(List<User> users);

    default PageResponse<UserProfileResponse> toPageResponse(Page<User> users) {
        List<UserProfileResponse> responses = toUserProfileResponseList(users.getContent());
        return PageableUtils.toPageResponse(users, responses);
    }
}
