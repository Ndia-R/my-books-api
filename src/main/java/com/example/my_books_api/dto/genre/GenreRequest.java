package com.example.my_books_api.dto.genre;

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
public class GenreRequest {
    @NonNull
    @NotNull(message = "ジャンル名は必須です")
    @NotBlank(message = "ジャンル名は空文字列にできません")
    @Length(min = 1, max = 50, message = "ジャンル名は1文字以上50文字以内で入力してください")
    private String name;

    @NonNull
    @NotNull(message = "説明は必須です")
    @NotBlank(message = "説明は空文字列にできません")
    @Length(min = 1, max = 255, message = "説明は1文字以上255文字以内で入力してください")
    private String description;
}
