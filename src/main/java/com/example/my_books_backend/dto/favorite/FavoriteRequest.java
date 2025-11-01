package com.example.my_books_backend.dto.favorite;

import org.springframework.lang.NonNull;
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
    private String bookId;
}
