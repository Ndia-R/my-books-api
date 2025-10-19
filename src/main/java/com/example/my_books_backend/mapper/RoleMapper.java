package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import com.example.my_books_backend.dto.role.RoleResponse;
import com.example.my_books_backend.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toRoleResponse(Role role);

    List<RoleResponse> toRoleResponseList(List<Role> roles);
}
