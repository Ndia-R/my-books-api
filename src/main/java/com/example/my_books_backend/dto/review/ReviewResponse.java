package com.example.my_books_backend.dto.review;

import java.time.LocalDateTime;
import com.example.my_books_backend.dto.book.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;

    private String name;
    private String avatarPath;
    private String comment;
    private Double rating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookResponse book;
}
