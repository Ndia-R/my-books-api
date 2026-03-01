package com.example.my_books_api.dto.book_preview_setting;

import java.time.LocalDateTime;
import java.util.List;

import com.example.my_books_api.dto.book.BookResponse;
import com.example.my_books_api.dto.book_chapter.BookChapterResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPreviewSettingResponse {
    private Long id;

    private Long maxChapter;
    private Long maxPage;

    private BookResponse book;

    private Long actualMaxChapter;
    private List<BookChapterResponse> chapters;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
