package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.entity.Role;
import com.example.my_books_backend.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringList")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringList")
    UserProfileResponse toUserProfileResponse(User user);

    List<UserProfileResponse> toUserProfileResponseList(List<User> users);

    @Named("rolesToStringList")
    default List<String> rolesToStringList(List<Role> roles) {
        return roles.stream().map(role -> role.getName().toString()).toList();
    }
}
