package com.example.my_books_backend.dto.user;

import java.util.List;
import com.example.my_books_backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotNull
    @Email
    private String email;

    @NotNull
    private String password;

    private List<Role> roles;
    private String name;
    private String avatarPath;
}
