package com.example.my_books_backend.dto.user;

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
    private String id; // Keycloak UUID

    @NotNull
    @Email
    private String email;

    private String name;
    private String avatarPath;
}
