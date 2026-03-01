package com.example.my_books_api.dto.favorite;

import java.time.LocalDateTime;
import com.example.my_books_api.dto.book.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {
    private Long id;
    private String userId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookResponse book;
}
