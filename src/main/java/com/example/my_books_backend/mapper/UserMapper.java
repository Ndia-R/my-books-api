package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    UserProfileResponse toUserProfileResponse(User user);

    List<UserProfileResponse> toUserProfileResponseList(List<User> users);
}
