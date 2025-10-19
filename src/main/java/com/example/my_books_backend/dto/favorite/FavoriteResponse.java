package com.example.my_books_backend.dto.favorite;

import java.time.LocalDateTime;
import com.example.my_books_backend.dto.book.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {
    private Long id;
    private Long userId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookResponse book;
}
