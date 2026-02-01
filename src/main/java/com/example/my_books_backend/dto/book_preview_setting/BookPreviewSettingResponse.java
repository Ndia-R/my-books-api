package com.example.my_books_backend.dto.book_preview_setting;

import java.time.LocalDateTime;

import com.example.my_books_backend.dto.book.BookResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPreviewSettingResponse {
    private Long id;

    private boolean unlimitedChapter;
    private boolean unlimitedPage;
    private Long maxChapter;
    private Long maxPage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookResponse book;
}
