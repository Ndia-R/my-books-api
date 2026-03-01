package com.example.my_books_api.dto.user;

import org.hibernate.validator.constraints.Length;
import org.springframework.lang.NonNull;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    @NonNull
    @NotNull(message = "表示名は必須です")
    @NotBlank(message = "表示名は空文字列にできません")
    @Length(min = 1, max = 50, message = "表示名は1文字以上50文字以内で入力してください")
    private String displayName;

    @NonNull
    @NotNull
    private String avatarPath;
}
