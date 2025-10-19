package com.example.my_books_backend.dto.bookmark;

import java.time.LocalDateTime;
import com.example.my_books_backend.dto.book.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private Long id;
    private Long userId;

    private Long chapterNumber;
    private String chapterTitle;
    private Long pageNumber;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookResponse book;
}
