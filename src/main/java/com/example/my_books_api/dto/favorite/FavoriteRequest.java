package com.example.my_books_api.dto.favorite;

import org.springframework.lang.NonNull;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRequest {
    @NonNull
    @NotNull
    @NotBlank(message = "書籍IDは必須です")
    private String bookId;
}
