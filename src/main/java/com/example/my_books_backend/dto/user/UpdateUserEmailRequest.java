package com.example.my_books_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateUserEmailRequest {
    @NotBlank(message = "メールアドレスは必須です")
    @Email
    private String email;

    // Note: Keycloak認証では、パスワードフィールドは不要
    // メールアドレス変更はKeycloak側で認証後に行われる
}
